package br.com.techmind.academy.payment;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class MercadoPagoGateway {

    private final String accessToken;
    private final String webhookSecret;
    private final String publicUrl;
    private final String webhookBaseUrl;
    private final boolean sandbox;
    private final RestClient client;

    public MercadoPagoGateway(
            @Value("${app.payments.mercado-pago.access-token:}") String accessToken,
            @Value("${app.payments.mercado-pago.webhook-secret:}") String webhookSecret,
            @Value("${app.payments.public-url:http://localhost:3000}") String publicUrl,
            @Value("${app.payments.webhook-base-url:}") String webhookBaseUrl,
            @Value("${app.payments.mercado-pago.sandbox:true}") boolean sandbox
    ) {
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.publicUrl = trimTrailingSlash(publicUrl);
        this.webhookBaseUrl = trimTrailingSlash(webhookBaseUrl);
        this.sandbox = sandbox;
        this.client = RestClient.builder()
                .baseUrl("https://api.mercadopago.com")
                .build();
    }

    public boolean isConfigured() {
        return !accessToken.isBlank();
    }

    public boolean isWebhookValidationConfigured() {
        return !webhookSecret.isBlank();
    }

    public ProviderCheckoutSession createPreference(PaymentOrder order) {
        requireConfigured();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("external_reference", order.getExternalReference());
        body.put("auto_return", "approved");
        body.put("back_urls", Map.of(
                "success", publicUrl + "/?payment_result=success",
                "pending", publicUrl + "/?payment_result=pending",
                "failure", publicUrl + "/?payment_result=failure"
        ));
        body.put("payer", Map.of("email", order.getUser().getEmail()));
        body.put("items", List.of(Map.of(
                "id", order.getPlanCode().name() + "-" + order.getBillingPeriod().name(),
                "title", title(order),
                "description", description(order),
                "quantity", 1,
                "currency_id", order.getCurrency(),
                "unit_price", order.getAmount()
        )));

        if (!webhookBaseUrl.isBlank()
                && !webhookBaseUrl.contains("localhost")
                && !webhookBaseUrl.contains("127.0.0.1")) {
            body.put(
                    "notification_url",
                    webhookBaseUrl + "/api/v1/payments/webhooks/mercado-pago"
            );
        }

        try {
            var response = client.post()
                    .uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.path("id").asText().isBlank()) {
                throw new ResponseStatusException(
                        BAD_GATEWAY,
                        "Mercado Pago não retornou uma preferência válida"
                );
            }

            String checkoutUrl = sandbox
                    ? response.path("sandbox_init_point").asText()
                    : response.path("init_point").asText();

            if (checkoutUrl.isBlank()) {
                checkoutUrl = sandbox
                        ? response.path("init_point").asText()
                        : response.path("sandbox_init_point").asText();
            }

            if (checkoutUrl.isBlank()) {
                throw new ResponseStatusException(
                        BAD_GATEWAY,
                        "Mercado Pago não retornou a URL de checkout"
                );
            }

            return new ProviderCheckoutSession(
                    response.path("id").asText(),
                    checkoutUrl
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Não foi possível criar o checkout no Mercado Pago",
                    exception
            );
        }
    }

    public ProviderPaymentDetails getPayment(String paymentId) {
        requireConfigured();

        try {
            var response = client.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.path("id").asText().isBlank()) {
                throw new ResponseStatusException(
                        BAD_GATEWAY,
                        "Pagamento não encontrado no Mercado Pago"
                );
            }

            return new ProviderPaymentDetails(
                    response.path("id").asText(),
                    response.path("external_reference").asText(),
                    response.path("status").asText(),
                    decimal(response.get("transaction_amount")),
                    response.path("currency_id").asText()
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Não foi possível consultar o pagamento no Mercado Pago",
                    exception
            );
        }
    }

    public boolean validateWebhook(
            String signatureHeader,
            String requestId,
            String dataId
    ) {
        if (!isWebhookValidationConfigured()
                || signatureHeader == null
                || requestId == null
                || dataId == null) {
            return false;
        }

        Map<String, String> signatureParts = new HashMap<>();
        for (String part : signatureHeader.split(",")) {
            var keyValue = part.trim().split("=", 2);
            if (keyValue.length == 2) {
                signatureParts.put(keyValue[0], keyValue[1]);
            }
        }

        String timestamp = signatureParts.get("ts");
        String receivedHash = signatureParts.get("v1");

        if (timestamp == null || receivedHash == null) {
            return false;
        }

        String manifest = "id:" + dataId.toLowerCase(Locale.ROOT)
                + ";request-id:" + requestId
                + ";ts:" + timestamp + ";";

        String calculatedHash = hmacSha256Hex(webhookSecret, manifest);

        return MessageDigest.isEqual(
                calculatedHash.getBytes(StandardCharsets.UTF_8),
                receivedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "Mercado Pago ainda não está configurado no servidor"
            );
        }
    }

    private BigDecimal decimal(JsonNode node) {
        return node == null || node.isNull()
                ? BigDecimal.ZERO
                : node.decimalValue();
    }

    private String title(PaymentOrder order) {
        return "TechMind " + prettyPlan(order)
                + " - " + prettyPeriod(order);
    }

    private String description(PaymentOrder order) {
        return "Acesso TechMind AI Academy ao plano "
                + prettyPlan(order)
                + " por período "
                + prettyPeriod(order).toLowerCase(Locale.ROOT);
    }

    private String prettyPlan(PaymentOrder order) {
        return order.getPlanCode().name().equals("PRO") ? "Pro" : "Career";
    }

    private String prettyPeriod(PaymentOrder order) {
        return order.getBillingPeriod().name().equals("ANNUAL") ? "Anual" : "Mensal";
    }

    private String hmacSha256Hex(String secret, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            byte[] result = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(result.length * 2);
            for (byte item : result) {
                hex.append(String.format("%02x", item & 0xff));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Não foi possível validar a assinatura do webhook",
                    exception
            );
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

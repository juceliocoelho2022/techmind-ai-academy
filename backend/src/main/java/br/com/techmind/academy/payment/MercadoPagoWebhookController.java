package br.com.techmind.academy.payment;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/payments/webhooks/mercado-pago")
public class MercadoPagoWebhookController {

    private final MercadoPagoGateway gateway;
    private final PaymentService paymentService;

    public MercadoPagoWebhookController(
            MercadoPagoGateway gateway,
            PaymentService paymentService
    ) {
        this.gateway = gateway;
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Void> webhook(
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestParam(name = "data.id", required = false) String queryDataId,
            @RequestParam(name = "type", required = false) String queryType,
            @RequestBody(required = false) JsonNode body
    ) {
        String type = queryType;
        if ((type == null || type.isBlank()) && body != null) {
            type = body.path("type").asText();
        }

        if (type != null && !type.isBlank() && !"payment".equalsIgnoreCase(type)) {
            return ResponseEntity.ok().build();
        }

        String dataId = queryDataId;
        if ((dataId == null || dataId.isBlank()) && body != null) {
            dataId = body.path("data").path("id").asText();
        }

        if (dataId == null || dataId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (!gateway.isWebhookValidationConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Validação de webhook do Mercado Pago não configurada"
            );
        }

        if (!gateway.validateWebhook(signature, requestId, dataId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            paymentService.reconcileWebhook(dataId);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity.ok().build();
            }
            throw exception;
        }

        return ResponseEntity.ok().build();
    }
}

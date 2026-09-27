package br.com.techmind.academy.payment;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoGatewayTest {

    @Test
    void shouldValidateSignedWebhookManifest() throws Exception {
        String secret = "test-webhook-secret";
        String requestId = "request-123";
        String dataId = "987654321";
        String timestamp = "1742505638683";
        String manifest = "id:" + dataId
                + ";request-id:" + requestId
                + ";ts:" + timestamp + ";";

        String hash = hmac(secret, manifest);

        var gateway = new MercadoPagoGateway(
                "TEST-access-token",
                secret,
                "http://localhost:3000",
                "",
                true
        );

        assertThat(
                gateway.validateWebhook(
                        "ts=" + timestamp + ",v1=" + hash,
                        requestId,
                        dataId
                )
        ).isTrue();

        assertThat(
                gateway.validateWebhook(
                        "ts=" + timestamp + ",v1=invalid",
                        requestId,
                        dataId
                )
        ).isFalse();
    }

    private String hmac(String secret, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));

        byte[] result = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();

        for (byte item : result) {
            hex.append(String.format("%02x", item & 0xff));
        }

        return hex.toString();
    }
}

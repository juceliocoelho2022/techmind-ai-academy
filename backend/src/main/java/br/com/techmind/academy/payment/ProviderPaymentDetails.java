package br.com.techmind.academy.payment;

import java.math.BigDecimal;

public record ProviderPaymentDetails(
        String paymentId,
        String externalReference,
        String status,
        BigDecimal amount,
        String currency
) {
}

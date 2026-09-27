package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentHistoryResponse(
        Long id,
        String externalReference,
        SubscriptionPlan plan,
        BillingPeriod billingPeriod,
        BigDecimal amount,
        String currency,
        PaymentOrderStatus status,
        PaymentProvider provider,
        String providerStatus,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt
) {
    static PaymentHistoryResponse from(PaymentOrder order) {
        return new PaymentHistoryResponse(
                order.getId(),
                order.getExternalReference(),
                order.getPlanCode(),
                order.getBillingPeriod(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getProvider(),
                order.getProviderStatus(),
                order.getCreatedAt(),
                order.getPaidAt()
        );
    }
}

package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;

import java.math.BigDecimal;

public record CheckoutResponse(
        Long orderId,
        String externalReference,
        SubscriptionPlan plan,
        BillingPeriod billingPeriod,
        BigDecimal amount,
        String currency,
        PaymentOrderStatus status,
        PaymentProvider provider,
        String checkoutUrl
) {
    static CheckoutResponse from(PaymentOrder order) {
        return new CheckoutResponse(
                order.getId(),
                order.getExternalReference(),
                order.getPlanCode(),
                order.getBillingPeriod(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getProvider(),
                order.getCheckoutUrl()
        );
    }
}

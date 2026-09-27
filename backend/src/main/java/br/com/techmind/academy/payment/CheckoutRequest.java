package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull SubscriptionPlan plan,
        @NotNull BillingPeriod billingPeriod
) {
}

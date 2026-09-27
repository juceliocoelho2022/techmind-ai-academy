package br.com.techmind.academy.subscription;

import jakarta.validation.constraints.NotNull;

public record UpgradeSubscriptionRequest(
        @NotNull SubscriptionPlan plan,
        @NotNull BillingPeriod billingPeriod
) {
}

package br.com.techmind.academy.subscription;

import java.time.OffsetDateTime;

public record AdminUpgradeRequestResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        SubscriptionPlan currentPlan,
        SubscriptionPlan requestedPlan,
        BillingPeriod billingPeriod,
        UpgradeRequestStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt,
        String resolvedByEmail
) {
}

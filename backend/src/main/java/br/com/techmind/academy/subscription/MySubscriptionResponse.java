package br.com.techmind.academy.subscription;

import java.time.OffsetDateTime;

public record MySubscriptionResponse(
        Long userId,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        SubscriptionSource source,
        OffsetDateTime startedAt,
        OffsetDateTime endsAt,
        PendingUpgrade pendingUpgrade
) {
    public record PendingUpgrade(
            Long id,
            SubscriptionPlan requestedPlan,
            BillingPeriod billingPeriod,
            UpgradeRequestStatus status,
            OffsetDateTime createdAt
    ) {}
}

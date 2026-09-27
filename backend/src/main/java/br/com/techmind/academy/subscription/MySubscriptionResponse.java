package br.com.techmind.academy.subscription;

import java.time.OffsetDateTime;

public record MySubscriptionResponse(
        Long userId,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        SubscriptionSource source,
        BillingPeriod billingPeriod,
        OffsetDateTime startedAt,
        OffsetDateTime endsAt,
        OffsetDateTime canceledAt,
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

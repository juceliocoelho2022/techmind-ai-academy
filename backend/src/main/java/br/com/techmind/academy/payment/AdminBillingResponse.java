package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;
import br.com.techmind.academy.subscription.SubscriptionSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminBillingResponse(
        OffsetDateTime generatedAt,
        BigDecimal grossPaidRevenue,
        BigDecimal paidRevenue30Days,
        BigDecimal mrrEquivalent,
        BigDecimal arrEquivalent,
        long paidOrders,
        long activePremiumAccesses,
        long activePaidSubscriptions,
        long proActive,
        long careerActive,
        long expiring7Days,
        long expiring30Days,
        List<RecentPayment> recentPayments,
        List<UpcomingExpiration> upcomingExpirations
) {
    public record RecentPayment(
            Long id,
            String externalReference,
            Long userId,
            String userName,
            String userEmail,
            SubscriptionPlan plan,
            BillingPeriod billingPeriod,
            BigDecimal amount,
            String currency,
            PaymentOrderStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime paidAt
    ) {}

    public record UpcomingExpiration(
            Long subscriptionId,
            Long userId,
            String userName,
            String userEmail,
            SubscriptionPlan plan,
            BillingPeriod billingPeriod,
            SubscriptionSource source,
            OffsetDateTime endsAt
    ) {}
}

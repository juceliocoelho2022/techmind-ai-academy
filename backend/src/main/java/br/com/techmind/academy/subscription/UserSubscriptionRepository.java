package br.com.techmind.academy.subscription;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<UserSubscription> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<UserSubscription> findByUserEmail(String email);

    long countByStatusAndPlanCodeAndBillingPeriodAndEndsAtAfter(
            SubscriptionStatus status,
            SubscriptionPlan planCode,
            BillingPeriod billingPeriod,
            OffsetDateTime now
    );

    long countByStatusAndSourceAndPlanCodeAndBillingPeriodAndEndsAtAfter(
            SubscriptionStatus status,
            SubscriptionSource source,
            SubscriptionPlan planCode,
            BillingPeriod billingPeriod,
            OffsetDateTime now
    );

    long countByStatusAndEndsAtBetween(
            SubscriptionStatus status,
            OffsetDateTime start,
            OffsetDateTime end
    );

    @EntityGraph(attributePaths = "user")
    List<UserSubscription> findTop20ByStatusAndEndsAtBetweenOrderByEndsAtAsc(
            SubscriptionStatus status,
            OffsetDateTime start,
            OffsetDateTime end
    );
}

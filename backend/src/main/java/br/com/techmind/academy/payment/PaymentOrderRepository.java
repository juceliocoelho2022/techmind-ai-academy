package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<PaymentOrder> findByExternalReference(String externalReference);

    @EntityGraph(attributePaths = "user")
    Optional<PaymentOrder> findByProviderPaymentId(String providerPaymentId);

    @EntityGraph(attributePaths = "user")
    Optional<PaymentOrder> findFirstByUserIdAndPlanCodeAndBillingPeriodAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId,
            SubscriptionPlan planCode,
            BillingPeriod billingPeriod,
            Collection<PaymentOrderStatus> statuses,
            OffsetDateTime createdAt
    );
}

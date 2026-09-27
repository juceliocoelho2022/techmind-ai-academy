package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<PaymentOrder> findByExternalReference(String externalReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "user")
    @Query("select p from PaymentOrder p where p.externalReference = :externalReference")
    Optional<PaymentOrder> findForUpdateByExternalReference(
            @Param("externalReference") String externalReference
    );

    @EntityGraph(attributePaths = "user")
    Optional<PaymentOrder> findByProviderPaymentId(String providerPaymentId);

    @EntityGraph(attributePaths = "user")
    List<PaymentOrder> findByUserEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = "user")
    List<PaymentOrder> findTop50ByOrderByCreatedAtDesc();

    long countByStatus(PaymentOrderStatus status);

    @Query("select sum(p.amount) from PaymentOrder p where p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentOrderStatus status);

    @Query("""
            select sum(p.amount)
            from PaymentOrder p
            where p.status = :status
              and p.paidAt >= :since
            """)
    BigDecimal sumAmountByStatusSince(
            @Param("status") PaymentOrderStatus status,
            @Param("since") OffsetDateTime since
    );

    @EntityGraph(attributePaths = "user")
    Optional<PaymentOrder> findFirstByUserIdAndPlanCodeAndBillingPeriodAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId,
            SubscriptionPlan planCode,
            BillingPeriod billingPeriod,
            Collection<PaymentOrderStatus> statuses,
            OffsetDateTime createdAt
    );
}

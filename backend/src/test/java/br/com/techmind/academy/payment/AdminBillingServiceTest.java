package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.*;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminBillingServiceTest {

    @Test
    void shouldCalculateFinancialSummaryWithoutTreatingManualAccessAsPaidMrr() {
        var paymentRepository = mock(PaymentOrderRepository.class);
        var subscriptionRepository = mock(UserSubscriptionRepository.class);
        var userRepository = mock(UserRepository.class);
        var pricing = new PaymentPlanPricing();

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findByEmail(admin.getEmail()))
                .thenReturn(Optional.of(admin));

        when(paymentRepository.sumAmountByStatus(PaymentOrderStatus.PAID))
                .thenReturn(new BigDecimal("1000.00"));
        when(paymentRepository.sumAmountByStatusSince(
                eq(PaymentOrderStatus.PAID),
                any(OffsetDateTime.class)
        )).thenReturn(new BigDecimal("249.70"));
        when(paymentRepository.countByStatus(PaymentOrderStatus.PAID))
                .thenReturn(7L);
        when(paymentRepository.findTop50ByOrderByCreatedAtDesc())
                .thenReturn(List.of());

        when(subscriptionRepository
                .countByStatusAndPlanCodeAndBillingPeriodAndEndsAtAfter(
                        eq(SubscriptionStatus.ACTIVE),
                        eq(SubscriptionPlan.PRO),
                        eq(BillingPeriod.MONTHLY),
                        any(OffsetDateTime.class)
                )).thenReturn(2L);
        when(subscriptionRepository
                .countByStatusAndPlanCodeAndBillingPeriodAndEndsAtAfter(
                        eq(SubscriptionStatus.ACTIVE),
                        eq(SubscriptionPlan.PRO),
                        eq(BillingPeriod.ANNUAL),
                        any(OffsetDateTime.class)
                )).thenReturn(1L);
        when(subscriptionRepository
                .countByStatusAndPlanCodeAndBillingPeriodAndEndsAtAfter(
                        eq(SubscriptionStatus.ACTIVE),
                        eq(SubscriptionPlan.CAREER),
                        eq(BillingPeriod.MONTHLY),
                        any(OffsetDateTime.class)
                )).thenReturn(1L);
        when(subscriptionRepository
                .countByStatusAndPlanCodeAndBillingPeriodAndEndsAtAfter(
                        eq(SubscriptionStatus.ACTIVE),
                        eq(SubscriptionPlan.CAREER),
                        eq(BillingPeriod.ANNUAL),
                        any(OffsetDateTime.class)
                )).thenReturn(0L);

        when(subscriptionRepository
                .countByStatusAndSourceAndPlanCodeAndBillingPeriodAndEndsAtAfter(
                        eq(SubscriptionStatus.ACTIVE),
                        eq(SubscriptionSource.PAYMENT),
                        eq(SubscriptionPlan.PRO),
                        eq(BillingPeriod.MONTHLY),
                        any(OffsetDateTime.class)
                )).thenReturn(1L);
        when(subscriptionRepository
                .countByStatusAndSourceAndPlanCodeAndBillingPeriodAndEndsAtAfter(
                        eq(SubscriptionStatus.ACTIVE),
                        eq(SubscriptionSource.PAYMENT),
                        eq(SubscriptionPlan.PRO),
                        eq(BillingPeriod.ANNUAL),
                        any(OffsetDateTime.class)
                )).thenReturn(1L);
        when(subscriptionRepository
                .countByStatusAndSourceAndPlanCodeAndBillingPeriodAndEndsAtAfter(
                        eq(SubscriptionStatus.ACTIVE),
                        eq(SubscriptionSource.PAYMENT),
                        eq(SubscriptionPlan.CAREER),
                        eq(BillingPeriod.MONTHLY),
                        any(OffsetDateTime.class)
                )).thenReturn(1L);
        when(subscriptionRepository
                .countByStatusAndSourceAndPlanCodeAndBillingPeriodAndEndsAtAfter(
                        eq(SubscriptionStatus.ACTIVE),
                        eq(SubscriptionSource.PAYMENT),
                        eq(SubscriptionPlan.CAREER),
                        eq(BillingPeriod.ANNUAL),
                        any(OffsetDateTime.class)
                )).thenReturn(0L);

        when(subscriptionRepository.countByStatusAndEndsAtBetween(
                eq(SubscriptionStatus.ACTIVE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(1L, 2L);

        when(subscriptionRepository
                .findTop20ByStatusAndEndsAtBetweenOrderByEndsAtAsc(
                        eq(SubscriptionStatus.ACTIVE),
                        any(OffsetDateTime.class),
                        any(OffsetDateTime.class)
                )).thenReturn(List.of());

        var service = new AdminBillingService(
                paymentRepository,
                subscriptionRepository,
                userRepository,
                pricing
        );

        var response = service.summary(admin.getEmail());

        assertThat(response.grossPaidRevenue()).isEqualByComparingTo("1000.00");
        assertThat(response.paidRevenue30Days()).isEqualByComparingTo("249.70");
        assertThat(response.paidOrders()).isEqualTo(7);
        assertThat(response.activePremiumAccesses()).isEqualTo(4);
        assertThat(response.activePaidSubscriptions()).isEqualTo(3);
        assertThat(response.proActive()).isEqualTo(3);
        assertThat(response.careerActive()).isEqualTo(1);
        assertThat(response.expiring7Days()).isEqualTo(1);
        assertThat(response.expiring30Days()).isEqualTo(2);
        assertThat(response.mrrEquivalent()).isEqualByComparingTo("179.72");
        assertThat(response.arrEquivalent()).isEqualByComparingTo("2156.64");
    }
}

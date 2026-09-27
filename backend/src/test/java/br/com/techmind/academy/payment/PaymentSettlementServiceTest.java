package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;
import br.com.techmind.academy.subscription.SubscriptionService;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaymentSettlementServiceTest {

    @Test
    void shouldActivatePlanOnlyOnceForApprovedPayment() {
        var repository = mock(PaymentOrderRepository.class);
        var subscriptionService = mock(SubscriptionService.class);

        var order = order();
        when(repository.findForUpdateByExternalReference("tm-order-1"))
                .thenReturn(Optional.of(order));
        when(repository.save(order)).thenReturn(order);

        var service = new PaymentSettlementService(
                repository,
                subscriptionService
        );

        var payment = new ProviderPaymentDetails(
                "mp-123",
                "tm-order-1",
                "approved",
                new BigDecimal("49.90"),
                "BRL"
        );

        var settled = service.settle(payment);

        assertThat(settled.getStatus()).isEqualTo(PaymentOrderStatus.PAID);
        assertThat(settled.getProviderPaymentId()).isEqualTo("mp-123");
        assertThat(settled.getPaidAt()).isNotNull();

        verify(subscriptionService).activatePaidPlan(
                7L,
                SubscriptionPlan.PRO,
                BillingPeriod.MONTHLY,
                "mp-123"
        );

        service.settle(payment);

        verify(subscriptionService, times(1)).activatePaidPlan(
                anyLong(),
                any(),
                any(),
                anyString()
        );
    }

    @Test
    void shouldKeepPendingPaymentWithoutActivatingSubscription() {
        var repository = mock(PaymentOrderRepository.class);
        var subscriptionService = mock(SubscriptionService.class);

        var order = order();
        when(repository.findForUpdateByExternalReference("tm-order-1"))
                .thenReturn(Optional.of(order));
        when(repository.save(order)).thenReturn(order);

        var service = new PaymentSettlementService(
                repository,
                subscriptionService
        );

        var settled = service.settle(
                new ProviderPaymentDetails(
                        "mp-456",
                        "tm-order-1",
                        "pending",
                        new BigDecimal("49.90"),
                        "BRL"
                )
        );

        assertThat(settled.getStatus()).isEqualTo(PaymentOrderStatus.PENDING);
        verifyNoInteractions(subscriptionService);
    }

    private PaymentOrder order() {
        var user = User.builder()
                .id(7L)
                .name("Aluno")
                .email("aluno@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.STUDENT)
                .build();

        return PaymentOrder.builder()
                .id(10L)
                .externalReference("tm-order-1")
                .user(user)
                .planCode(SubscriptionPlan.PRO)
                .billingPeriod(BillingPeriod.MONTHLY)
                .amount(new BigDecimal("49.90"))
                .currency("BRL")
                .provider(PaymentProvider.MERCADO_PAGO)
                .status(PaymentOrderStatus.CHECKOUT_CREATED)
                .build();
    }
}

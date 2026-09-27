package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;
import br.com.techmind.academy.subscription.SubscriptionService;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Test
    void shouldCreateCheckoutUsingServerSidePrice() {
        var userRepository = mock(UserRepository.class);
        var orderRepository = mock(PaymentOrderRepository.class);
        var pricing = new PaymentPlanPricing();
        var gateway = mock(MercadoPagoGateway.class);
        var settlementService = mock(PaymentSettlementService.class);
        var subscriptionService = mock(SubscriptionService.class);

        var user = User.builder()
                .id(7L)
                .name("Aluno")
                .email("aluno@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.STUDENT)
                .build();

        when(gateway.isConfigured()).thenReturn(true);
        when(gateway.hasValidReturnUrl()).thenReturn(true);
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(subscriptionService.currentPlanForUser(user.getId()))
                .thenReturn(SubscriptionPlan.FREE);
        when(orderRepository
                .findFirstByUserIdAndPlanCodeAndBillingPeriodAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
                        eq(7L),
                        eq(SubscriptionPlan.PRO),
                        eq(BillingPeriod.MONTHLY),
                        anyCollection(),
                        any()
                ))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(PaymentOrder.class)))
                .thenAnswer(invocation -> {
                    var order = invocation.getArgument(0, PaymentOrder.class);
                    if (order.getId() == null) order.setId(99L);
                    return order;
                });
        when(gateway.createPreference(any(PaymentOrder.class)))
                .thenReturn(new ProviderCheckoutSession(
                        "pref-1",
                        "https://www.mercadopago.com/checkout/test"
                ));

        var service = new PaymentService(
                userRepository,
                orderRepository,
                pricing,
                gateway,
                settlementService,
                subscriptionService
        );

        var response = service.createCheckout(
                user.getEmail(),
                new CheckoutRequest(
                        SubscriptionPlan.PRO,
                        BillingPeriod.MONTHLY
                )
        );

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("49.90"));
        assertThat(response.currency()).isEqualTo("BRL");
        assertThat(response.status()).isEqualTo(PaymentOrderStatus.CHECKOUT_CREATED);
        assertThat(response.checkoutUrl()).contains("mercadopago.com");

        verify(gateway).createPreference(argThat(order ->
                order.getAmount().compareTo(new BigDecimal("49.90")) == 0
                        && order.getPlanCode() == SubscriptionPlan.PRO
        ));
    }
}

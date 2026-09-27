package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;
import br.com.techmind.academy.subscription.SubscriptionService;
import br.com.techmind.academy.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final List<PaymentOrderStatus> REUSABLE_STATUSES = List.of(
            PaymentOrderStatus.CHECKOUT_CREATED,
            PaymentOrderStatus.PENDING
    );

    private final UserRepository userRepository;
    private final PaymentOrderRepository orderRepository;
    private final PaymentPlanPricing pricing;
    private final MercadoPagoGateway gateway;
    private final PaymentSettlementService settlementService;
    private final SubscriptionService subscriptionService;

    public PaymentService(
            UserRepository userRepository,
            PaymentOrderRepository orderRepository,
            PaymentPlanPricing pricing,
            MercadoPagoGateway gateway,
            PaymentSettlementService settlementService,
            SubscriptionService subscriptionService
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.pricing = pricing;
        this.gateway = gateway;
        this.settlementService = settlementService;
        this.subscriptionService = subscriptionService;
    }

    public CheckoutResponse createCheckout(
            String email,
            CheckoutRequest request
    ) {
        if (!gateway.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Mercado Pago ainda não está configurado no servidor"
            );
        }

        if (request.plan() == SubscriptionPlan.FREE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O plano FREE não possui checkout"
            );
        }

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        var currentPlan = subscriptionService.currentPlanForUser(user.getId());
        if (request.plan().rank() <= currentPlan.rank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O plano solicitado não é superior ao plano atual"
            );
        }

        var reusable = orderRepository
                .findFirstByUserIdAndPlanCodeAndBillingPeriodAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
                        user.getId(),
                        request.plan(),
                        request.billingPeriod(),
                        REUSABLE_STATUSES,
                        OffsetDateTime.now().minusMinutes(30)
                );

        if (reusable.isPresent()
                && reusable.get().getCheckoutUrl() != null
                && !reusable.get().getCheckoutUrl().isBlank()) {
            return CheckoutResponse.from(reusable.get());
        }

        var order = orderRepository.save(
                PaymentOrder.builder()
                        .externalReference("tm-" + UUID.randomUUID())
                        .user(user)
                        .planCode(request.plan())
                        .billingPeriod(request.billingPeriod())
                        .amount(pricing.price(request.plan(), request.billingPeriod()))
                        .currency("BRL")
                        .provider(PaymentProvider.MERCADO_PAGO)
                        .status(PaymentOrderStatus.CREATED)
                        .build()
        );

        try {
            var session = gateway.createPreference(order);
            order.setProviderPreferenceId(session.preferenceId());
            order.setCheckoutUrl(session.checkoutUrl());
            order.setStatus(PaymentOrderStatus.CHECKOUT_CREATED);
            return CheckoutResponse.from(orderRepository.save(order));
        } catch (RuntimeException exception) {
            order.setStatus(PaymentOrderStatus.FAILED);
            order.setProviderStatus("checkout_creation_failed");
            orderRepository.save(order);
            throw exception;
        }
    }

    public CheckoutResponse reconcileForUser(
            String email,
            String paymentId
    ) {
        var payment = gateway.getPayment(paymentId);

        var order = orderRepository
                .findByExternalReference(payment.externalReference())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ordem de pagamento não encontrada"
                ));

        if (!order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Pagamento pertence a outro usuário"
            );
        }

        return CheckoutResponse.from(settlementService.settle(payment));
    }

    public CheckoutResponse reconcileWebhook(String paymentId) {
        var payment = gateway.getPayment(paymentId);
        return CheckoutResponse.from(settlementService.settle(payment));
    }

    public CheckoutResponse orderForUser(
            String email,
            String externalReference
    ) {
        var order = orderRepository.findByExternalReference(externalReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ordem de pagamento não encontrada"
                ));

        if (!order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Ordem pertence a outro usuário"
            );
        }

        return CheckoutResponse.from(order);
    }
}

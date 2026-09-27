package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class PaymentSettlementService {

    private final PaymentOrderRepository orderRepository;
    private final SubscriptionService subscriptionService;

    public PaymentSettlementService(
            PaymentOrderRepository orderRepository,
            SubscriptionService subscriptionService
    ) {
        this.orderRepository = orderRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public PaymentOrder settle(ProviderPaymentDetails payment) {
        if (payment.externalReference() == null
                || payment.externalReference().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pagamento sem external_reference"
            );
        }

        var order = orderRepository
                .findForUpdateByExternalReference(payment.externalReference())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pagamento não pertence à TechMind"
                ));

        validateOrder(order, payment);

        if (order.getStatus() == PaymentOrderStatus.PAID) {
            return order;
        }

        order.setProviderPaymentId(payment.paymentId());
        order.setProviderStatus(payment.status());

        String status = payment.status() == null
                ? ""
                : payment.status().toLowerCase(Locale.ROOT);

        if ("approved".equals(status)) {
            if (order.getStatus() != PaymentOrderStatus.PAID) {
                order.setStatus(PaymentOrderStatus.PAID);
                order.setPaidAt(OffsetDateTime.now());
                orderRepository.save(order);

                subscriptionService.activatePaidPlan(
                        order.getUser().getId(),
                        order.getPlanCode(),
                        order.getBillingPeriod(),
                        payment.paymentId()
                );
            }

            return order;
        }

        if (status.equals("rejected")) {
            order.setStatus(PaymentOrderStatus.FAILED);
        } else if (status.equals("cancelled")
                || status.equals("canceled")
                || status.equals("refunded")
                || status.equals("charged_back")) {
            order.setStatus(PaymentOrderStatus.CANCELED);
        } else {
            order.setStatus(PaymentOrderStatus.PENDING);
        }

        return orderRepository.save(order);
    }

    private void validateOrder(
            PaymentOrder order,
            ProviderPaymentDetails payment
    ) {
        if (!"BRL".equalsIgnoreCase(payment.currency())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Moeda do pagamento divergente"
            );
        }

        if (payment.amount() == null
                || payment.amount().compareTo(order.getAmount()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Valor do pagamento divergente"
            );
        }
    }
}

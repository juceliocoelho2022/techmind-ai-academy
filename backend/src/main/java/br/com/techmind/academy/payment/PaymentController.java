package br.com.techmind.academy.payment;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public List<PaymentHistoryResponse> history(Authentication authentication) {
        return service.historyForUser(authentication.getName());
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return service.createCheckout(authentication.getName(), request);
    }

    @PostMapping("/reconcile/{paymentId}")
    public CheckoutResponse reconcile(
            Authentication authentication,
            @PathVariable String paymentId
    ) {
        return service.reconcileForUser(authentication.getName(), paymentId);
    }

    @GetMapping("/orders/{externalReference}")
    public CheckoutResponse order(
            Authentication authentication,
            @PathVariable String externalReference
    ) {
        return service.orderForUser(authentication.getName(), externalReference);
    }
}

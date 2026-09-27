package br.com.techmind.academy.subscription;

import jakarta.validation.constraints.AssertTrue;

public record CancelSubscriptionRequest(
        @AssertTrue(message = "Confirme o encerramento imediato do acesso premium")
        boolean confirmImmediate
) {
}

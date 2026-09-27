package br.com.techmind.academy.payment;

public record ProviderCheckoutSession(
        String preferenceId,
        String checkoutUrl
) {
}

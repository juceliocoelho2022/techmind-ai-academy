package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPlanPricingTest {

    private final PaymentPlanPricing pricing = new PaymentPlanPricing();

    @Test
    void shouldKeepCommercialPricesOnBackend() {
        assertThat(pricing.price(SubscriptionPlan.PRO, BillingPeriod.MONTHLY))
                .isEqualByComparingTo("49.90");
        assertThat(pricing.price(SubscriptionPlan.PRO, BillingPeriod.ANNUAL))
                .isEqualByComparingTo("479.00");
        assertThat(pricing.price(SubscriptionPlan.CAREER, BillingPeriod.MONTHLY))
                .isEqualByComparingTo("89.90");
        assertThat(pricing.price(SubscriptionPlan.CAREER, BillingPeriod.ANNUAL))
                .isEqualByComparingTo("849.00");
    }
}

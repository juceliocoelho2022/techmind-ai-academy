package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.BillingPeriod;
import br.com.techmind.academy.subscription.SubscriptionPlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentPlanPricing {

    public BigDecimal price(SubscriptionPlan plan, BillingPeriod period) {
        if (plan == SubscriptionPlan.FREE) {
            throw new IllegalArgumentException("Plano FREE não possui checkout");
        }

        return switch (plan) {
            case PRO -> period == BillingPeriod.ANNUAL
                    ? new BigDecimal("479.00")
                    : new BigDecimal("49.90");
            case CAREER -> period == BillingPeriod.ANNUAL
                    ? new BigDecimal("849.00")
                    : new BigDecimal("89.90");
            case FREE -> throw new IllegalArgumentException("Plano FREE não possui checkout");
        };
    }
}

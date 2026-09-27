package br.com.techmind.academy.payment;

import br.com.techmind.academy.subscription.*;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AdminBillingService {

    private final PaymentOrderRepository paymentRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentPlanPricing pricing;

    public AdminBillingService(
            PaymentOrderRepository paymentRepository,
            UserSubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            PaymentPlanPricing pricing
    ) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.pricing = pricing;
    }

    @Transactional(readOnly = true)
    public AdminBillingResponse summary(String adminEmail) {
        requireAdmin(adminEmail);

        var now = OffsetDateTime.now();
        var in7Days = now.plusDays(7);
        var in30Days = now.plusDays(30);

        long proMonthly = activeCount(SubscriptionPlan.PRO, BillingPeriod.MONTHLY, now);
        long proAnnual = activeCount(SubscriptionPlan.PRO, BillingPeriod.ANNUAL, now);
        long careerMonthly = activeCount(SubscriptionPlan.CAREER, BillingPeriod.MONTHLY, now);
        long careerAnnual = activeCount(SubscriptionPlan.CAREER, BillingPeriod.ANNUAL, now);

        long paidProMonthly = paidActiveCount(SubscriptionPlan.PRO, BillingPeriod.MONTHLY, now);
        long paidProAnnual = paidActiveCount(SubscriptionPlan.PRO, BillingPeriod.ANNUAL, now);
        long paidCareerMonthly = paidActiveCount(SubscriptionPlan.CAREER, BillingPeriod.MONTHLY, now);
        long paidCareerAnnual = paidActiveCount(SubscriptionPlan.CAREER, BillingPeriod.ANNUAL, now);

        long proActive = proMonthly + proAnnual;
        long careerActive = careerMonthly + careerAnnual;
        long activePremiumAccesses = proActive + careerActive;
        long activePaidSubscriptions =
                paidProMonthly + paidProAnnual + paidCareerMonthly + paidCareerAnnual;

        BigDecimal mrrEquivalent = BigDecimal.ZERO
                .add(pricing.price(SubscriptionPlan.PRO, BillingPeriod.MONTHLY)
                        .multiply(BigDecimal.valueOf(paidProMonthly)))
                .add(monthlyEquivalent(
                        pricing.price(SubscriptionPlan.PRO, BillingPeriod.ANNUAL),
                        paidProAnnual
                ))
                .add(pricing.price(SubscriptionPlan.CAREER, BillingPeriod.MONTHLY)
                        .multiply(BigDecimal.valueOf(paidCareerMonthly)))
                .add(monthlyEquivalent(
                        pricing.price(SubscriptionPlan.CAREER, BillingPeriod.ANNUAL),
                        paidCareerAnnual
                ))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal arrEquivalent = mrrEquivalent
                .multiply(BigDecimal.valueOf(12))
                .setScale(2, RoundingMode.HALF_UP);

        var recentPayments = paymentRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(order -> new AdminBillingResponse.RecentPayment(
                        order.getId(),
                        order.getExternalReference(),
                        order.getUser().getId(),
                        order.getUser().getName(),
                        order.getUser().getEmail(),
                        order.getPlanCode(),
                        order.getBillingPeriod(),
                        order.getAmount(),
                        order.getCurrency(),
                        order.getStatus(),
                        order.getCreatedAt(),
                        order.getPaidAt()
                ))
                .toList();

        var upcomingExpirations =
                subscriptionRepository
                        .findTop20ByStatusAndEndsAtBetweenOrderByEndsAtAsc(
                                SubscriptionStatus.ACTIVE,
                                now,
                                in30Days
                        )
                        .stream()
                        .map(subscription -> new AdminBillingResponse.UpcomingExpiration(
                                subscription.getId(),
                                subscription.getUser().getId(),
                                subscription.getUser().getName(),
                                subscription.getUser().getEmail(),
                                subscription.getPlanCode(),
                                subscription.getBillingPeriod(),
                                subscription.getSource(),
                                subscription.getEndsAt()
                        ))
                        .toList();

        return new AdminBillingResponse(
                now,
                zeroIfNull(paymentRepository.sumAmountByStatus(PaymentOrderStatus.PAID)),
                zeroIfNull(paymentRepository.sumAmountByStatusSince(
                        PaymentOrderStatus.PAID,
                        now.minusDays(30)
                )),
                mrrEquivalent,
                arrEquivalent,
                paymentRepository.countByStatus(PaymentOrderStatus.PAID),
                activePremiumAccesses,
                activePaidSubscriptions,
                proActive,
                careerActive,
                subscriptionRepository.countByStatusAndEndsAtBetween(
                        SubscriptionStatus.ACTIVE,
                        now,
                        in7Days
                ),
                subscriptionRepository.countByStatusAndEndsAtBetween(
                        SubscriptionStatus.ACTIVE,
                        now,
                        in30Days
                ),
                recentPayments,
                upcomingExpirations
        );
    }

    private long activeCount(
            SubscriptionPlan plan,
            BillingPeriod billingPeriod,
            OffsetDateTime now
    ) {
        return subscriptionRepository
                .countByStatusAndPlanCodeAndBillingPeriodAndEndsAtAfter(
                        SubscriptionStatus.ACTIVE,
                        plan,
                        billingPeriod,
                        now
                );
    }

    private long paidActiveCount(
            SubscriptionPlan plan,
            BillingPeriod billingPeriod,
            OffsetDateTime now
    ) {
        return subscriptionRepository
                .countByStatusAndSourceAndPlanCodeAndBillingPeriodAndEndsAtAfter(
                        SubscriptionStatus.ACTIVE,
                        SubscriptionSource.PAYMENT,
                        plan,
                        billingPeriod,
                        now
                );
    }

    private BigDecimal monthlyEquivalent(BigDecimal annualPrice, long count) {
        if (count == 0) return BigDecimal.ZERO;

        return annualPrice
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(count));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value;
    }

    private void requireAdmin(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso restrito ao administrador"
            );
        }
    }
}

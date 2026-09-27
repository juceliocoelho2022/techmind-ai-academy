package br.com.techmind.academy.subscription;

import br.com.techmind.academy.audit.AdminAuditService;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubscriptionServiceTest {

    @Test
    void shouldCreateFreeSubscriptionForNewUser() {
        var userRepository = mock(UserRepository.class);
        var subscriptionRepository = mock(UserSubscriptionRepository.class);
        var requestRepository = mock(SubscriptionUpgradeRequestRepository.class);
        var auditService = mock(AdminAuditService.class);

        var user = student();

        when(subscriptionRepository.findByUserId(user.getId()))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = new SubscriptionService(
                userRepository,
                subscriptionRepository,
                requestRepository,
                auditService
        );

        var subscription = service.ensureFreeSubscription(user);

        assertThat(subscription.getPlanCode()).isEqualTo(SubscriptionPlan.FREE);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getSource()).isEqualTo(SubscriptionSource.FREE);
        verify(subscriptionRepository).save(any(UserSubscription.class));
    }

    @Test
    void shouldCreatePendingUpgradeWithoutChangingCurrentPlan() {
        var userRepository = mock(UserRepository.class);
        var subscriptionRepository = mock(UserSubscriptionRepository.class);
        var requestRepository = mock(SubscriptionUpgradeRequestRepository.class);
        var auditService = mock(AdminAuditService.class);

        var user = student();
        var subscription = freeSubscription(user);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(subscription));
        when(requestRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                user.getId(),
                UpgradeRequestStatus.PENDING
        )).thenReturn(Optional.empty());
        when(requestRepository.save(any(SubscriptionUpgradeRequest.class)))
                .thenAnswer(invocation -> {
                    var request = invocation.getArgument(0, SubscriptionUpgradeRequest.class);
                    request.setId(99L);
                    return request;
                });

        var service = new SubscriptionService(
                userRepository,
                subscriptionRepository,
                requestRepository,
                auditService
        );

        var response = service.requestUpgrade(
                user.getEmail(),
                new UpgradeSubscriptionRequest(
                        SubscriptionPlan.PRO,
                        BillingPeriod.MONTHLY
                )
        );

        assertThat(response.plan()).isEqualTo(SubscriptionPlan.FREE);
        assertThat(response.pendingUpgrade()).isNotNull();
        assertThat(response.pendingUpgrade().requestedPlan()).isEqualTo(SubscriptionPlan.PRO);
        assertThat(response.pendingUpgrade().billingPeriod()).isEqualTo(BillingPeriod.MONTHLY);
        assertThat(subscription.getPlanCode()).isEqualTo(SubscriptionPlan.FREE);
    }

    @Test
    void shouldApproveUpgradeAndActivateRequestedPlan() {
        var userRepository = mock(UserRepository.class);
        var subscriptionRepository = mock(UserSubscriptionRepository.class);
        var requestRepository = mock(SubscriptionUpgradeRequestRepository.class);
        var auditService = mock(AdminAuditService.class);

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        var user = student();
        var subscription = freeSubscription(user);
        subscription.setId(50L);

        var request = SubscriptionUpgradeRequest.builder()
                .id(99L)
                .user(user)
                .requestedPlan(SubscriptionPlan.CAREER)
                .billingPeriod(BillingPeriod.ANNUAL)
                .status(UpgradeRequestStatus.PENDING)
                .build();

        when(userRepository.findByEmail(admin.getEmail()))
                .thenReturn(Optional.of(admin));
        when(requestRepository.findById(99L))
                .thenReturn(Optional.of(request));
        when(subscriptionRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(subscription))
                .thenReturn(subscription);
        when(requestRepository.save(request))
                .thenReturn(request);

        var service = new SubscriptionService(
                userRepository,
                subscriptionRepository,
                requestRepository,
                auditService
        );

        var response = service.approve(admin.getEmail(), 99L);

        assertThat(subscription.getPlanCode()).isEqualTo(SubscriptionPlan.CAREER);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getSource()).isEqualTo(SubscriptionSource.MANUAL);
        assertThat(subscription.getBillingPeriod()).isEqualTo(BillingPeriod.ANNUAL);
        assertThat(subscription.getEndsAt()).isNotNull();
        assertThat(request.getStatus()).isEqualTo(UpgradeRequestStatus.APPROVED);
        assertThat(response.currentPlan()).isEqualTo(SubscriptionPlan.CAREER);

        verify(auditService).record(
                eq(admin.getEmail()),
                eq("SUBSCRIPTION_APPROVED"),
                eq("USER_SUBSCRIPTION"),
                eq(50L),
                contains("CAREER")
        );
    }

    @Test
    void shouldCancelPremiumSubscriptionImmediately() {
        var userRepository = mock(UserRepository.class);
        var subscriptionRepository = mock(UserSubscriptionRepository.class);
        var requestRepository = mock(SubscriptionUpgradeRequestRepository.class);
        var auditService = mock(AdminAuditService.class);

        var user = student();
        var subscription = UserSubscription.builder()
                .id(80L)
                .user(user)
                .planCode(SubscriptionPlan.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .source(SubscriptionSource.PAYMENT)
                .billingPeriod(BillingPeriod.MONTHLY)
                .endsAt(java.time.OffsetDateTime.now().plusDays(20))
                .build();

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(subscription))
                .thenReturn(subscription);
        when(requestRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                user.getId(),
                UpgradeRequestStatus.PENDING
        )).thenReturn(Optional.empty());

        var service = new SubscriptionService(
                userRepository,
                subscriptionRepository,
                requestRepository,
                auditService
        );

        var response = service.cancelCurrent(
                user.getEmail(),
                new CancelSubscriptionRequest(true)
        );

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(subscription.getCanceledAt()).isNotNull();
        assertThat(response.status()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(service.currentPlanForUser(user.getId()))
                .isEqualTo(SubscriptionPlan.FREE);

        verify(auditService).record(
                eq(user.getEmail()),
                eq("SUBSCRIPTION_CANCELED_BY_USER"),
                eq("USER_SUBSCRIPTION"),
                eq(80L),
                contains("premium")
        );
    }

    @Test
    void shouldTreatInactivePremiumSubscriptionAsFreeForEntitlements() {
        var userRepository = mock(UserRepository.class);
        var subscriptionRepository = mock(UserSubscriptionRepository.class);
        var requestRepository = mock(SubscriptionUpgradeRequestRepository.class);
        var auditService = mock(AdminAuditService.class);

        var user = student();
        var subscription = UserSubscription.builder()
                .user(user)
                .planCode(SubscriptionPlan.PRO)
                .status(SubscriptionStatus.CANCELED)
                .source(SubscriptionSource.MANUAL)
                .build();

        when(subscriptionRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(subscription));

        var service = new SubscriptionService(
                userRepository,
                subscriptionRepository,
                requestRepository,
                auditService
        );

        assertThat(service.currentPlanForUser(user.getId()))
                .isEqualTo(SubscriptionPlan.FREE);
    }

    private User student() {
        return User.builder()
                .id(7L)
                .name("Aluno")
                .email("aluno@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.STUDENT)
                .build();
    }

    private UserSubscription freeSubscription(User user) {
        return UserSubscription.builder()
                .user(user)
                .planCode(SubscriptionPlan.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .source(SubscriptionSource.FREE)
                .build();
    }
}

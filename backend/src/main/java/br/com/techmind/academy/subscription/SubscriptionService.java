package br.com.techmind.academy.subscription;

import br.com.techmind.academy.audit.AdminAuditService;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SubscriptionService {

    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final SubscriptionUpgradeRequestRepository upgradeRequestRepository;
    private final AdminAuditService auditService;

    public SubscriptionService(
            UserRepository userRepository,
            UserSubscriptionRepository subscriptionRepository,
            SubscriptionUpgradeRequestRepository upgradeRequestRepository,
            AdminAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.upgradeRequestRepository = upgradeRequestRepository;
        this.auditService = auditService;
    }

    @Transactional
    public UserSubscription ensureFreeSubscription(User user) {
        return subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() ->
                        subscriptionRepository.save(
                                UserSubscription.builder()
                                        .user(user)
                                        .planCode(SubscriptionPlan.FREE)
                                        .status(SubscriptionStatus.ACTIVE)
                                        .source(SubscriptionSource.FREE)
                                        .billingPeriod(null)
                                        .build()
                        )
                );
    }

    @Transactional
    public MySubscriptionResponse current(String email) {
        var user = findUser(email);
        var subscription = ensureFreeSubscription(user);
        expireSubscriptionIfNeeded(subscription);

        var pending = upgradeRequestRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        UpgradeRequestStatus.PENDING
                )
                .orElse(null);

        return toMyResponse(subscription, pending);
    }

    @Transactional
    public MySubscriptionResponse requestUpgrade(
            String email,
            UpgradeSubscriptionRequest request
    ) {
        var user = findUser(email);
        var subscription = ensureFreeSubscription(user);

        if (request.plan() == SubscriptionPlan.FREE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "FREE não é um plano de upgrade"
            );
        }

        var effectiveCurrentPlan = currentPlanForUser(user.getId());

        if (request.plan().rank() <= effectiveCurrentPlan.rank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O plano solicitado não é superior ao plano atual"
            );
        }

        upgradeRequestRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        UpgradeRequestStatus.PENDING
                )
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Já existe uma solicitação de upgrade pendente"
                    );
                });

        var pending = upgradeRequestRepository.save(
                SubscriptionUpgradeRequest.builder()
                        .user(user)
                        .requestedPlan(request.plan())
                        .billingPeriod(request.billingPeriod())
                        .status(UpgradeRequestStatus.PENDING)
                        .build()
        );

        return toMyResponse(subscription, pending);
    }

    @Transactional(readOnly = true)
    public List<AdminUpgradeRequestResponse> adminRequests(
            String adminEmail,
            UpgradeRequestStatus status
    ) {
        requireAdmin(adminEmail);

        var requests = status == null
                ? upgradeRequestRepository.findAllByOrderByCreatedAtDesc()
                : upgradeRequestRepository.findByStatusOrderByCreatedAtDesc(status);

        return requests.stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public AdminUpgradeRequestResponse approve(
            String adminEmail,
            Long requestId
    ) {
        requireAdmin(adminEmail);

        var request = pendingRequest(requestId);
        var subscription = ensureFreeSubscription(request.getUser());

        var now = OffsetDateTime.now();

        subscription.setPlanCode(request.getRequestedPlan());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSource(SubscriptionSource.MANUAL);
        subscription.setBillingPeriod(request.getBillingPeriod());
        subscription.setCanceledAt(null);
        subscription.setStartedAt(now);
        subscription.setEndsAt(
                request.getBillingPeriod() == BillingPeriod.ANNUAL
                        ? now.plusYears(1)
                        : now.plusMonths(1)
        );
        subscriptionRepository.save(subscription);

        request.setStatus(UpgradeRequestStatus.APPROVED);
        request.setResolvedAt(OffsetDateTime.now());
        request.setResolvedByEmail(adminEmail);
        var savedRequest = upgradeRequestRepository.save(request);

        auditService.record(
                adminEmail,
                "SUBSCRIPTION_APPROVED",
                "USER_SUBSCRIPTION",
                subscription.getId(),
                "Plano " + subscription.getPlanCode().name()
                        + " ativado para " + request.getUser().getEmail()
        );

        return toAdminResponse(savedRequest);
    }

    @Transactional
    public AdminUpgradeRequestResponse reject(
            String adminEmail,
            Long requestId
    ) {
        requireAdmin(adminEmail);

        var request = pendingRequest(requestId);
        request.setStatus(UpgradeRequestStatus.REJECTED);
        request.setResolvedAt(OffsetDateTime.now());
        request.setResolvedByEmail(adminEmail);

        var saved = upgradeRequestRepository.save(request);

        auditService.record(
                adminEmail,
                "SUBSCRIPTION_REJECTED",
                "SUBSCRIPTION_UPGRADE_REQUEST",
                saved.getId(),
                "Upgrade para " + saved.getRequestedPlan().name()
                        + " rejeitado para " + saved.getUser().getEmail()
        );

        return toAdminResponse(saved);
    }

    @Transactional
    public MySubscriptionResponse cancelCurrent(
            String email,
            CancelSubscriptionRequest request
    ) {
        var user = findUser(email);
        var subscription = ensureFreeSubscription(user);
        expireSubscriptionIfNeeded(subscription);

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                || currentPlanForUser(user.getId()) == SubscriptionPlan.FREE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não existe assinatura premium ativa para cancelar"
            );
        }

        var now = OffsetDateTime.now();
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(now);
        subscriptionRepository.save(subscription);

        upgradeRequestRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        UpgradeRequestStatus.PENDING
                )
                .ifPresent(pending -> {
                    pending.setStatus(UpgradeRequestStatus.REJECTED);
                    pending.setResolvedAt(now);
                    pending.setResolvedByEmail("user:self-service");
                    upgradeRequestRepository.save(pending);
                });

        auditService.record(
                email,
                "SUBSCRIPTION_CANCELED_BY_USER",
                "USER_SUBSCRIPTION",
                subscription.getId(),
                "Usuário encerrou o acesso premium imediatamente"
        );

        return toMyResponse(subscription, null);
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan currentPlanForUser(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(subscription ->
                        subscription.getEndsAt() == null
                                || subscription.getEndsAt().isAfter(OffsetDateTime.now())
                )
                .map(UserSubscription::getPlanCode)
                .orElse(SubscriptionPlan.FREE);
    }

    @Transactional
    public UserSubscription activatePaidPlan(
            Long userId,
            SubscriptionPlan purchasedPlan,
            BillingPeriod billingPeriod,
            String paymentReference
    ) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        var subscription = ensureFreeSubscription(user);
        var now = OffsetDateTime.now();

        var effectiveCurrentPlan =
                subscription.getStatus() == SubscriptionStatus.ACTIVE
                        && (subscription.getEndsAt() == null || subscription.getEndsAt().isAfter(now))
                        ? subscription.getPlanCode()
                        : SubscriptionPlan.FREE;

        if (purchasedPlan.rank() >= effectiveCurrentPlan.rank()) {
            boolean sameActivePaidPlan =
                    subscription.getStatus() == SubscriptionStatus.ACTIVE
                            && subscription.getPlanCode() == purchasedPlan
                            && subscription.getSource() == SubscriptionSource.PAYMENT
                            && subscription.getEndsAt() != null
                            && subscription.getEndsAt().isAfter(now);

            var periodStart = sameActivePaidPlan
                    ? subscription.getEndsAt()
                    : now;

            subscription.setPlanCode(purchasedPlan);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setSource(SubscriptionSource.PAYMENT);
            subscription.setBillingPeriod(billingPeriod);
            subscription.setCanceledAt(null);
            subscription.setStartedAt(now);
            subscription.setEndsAt(
                    billingPeriod == BillingPeriod.ANNUAL
                            ? periodStart.plusYears(1)
                            : periodStart.plusMonths(1)
            );
            subscriptionRepository.save(subscription);
        }

        upgradeRequestRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        UpgradeRequestStatus.PENDING
                )
                .ifPresent(request -> {
                    if (purchasedPlan.rank() >= request.getRequestedPlan().rank()) {
                        request.setStatus(UpgradeRequestStatus.APPROVED);
                        request.setResolvedAt(now);
                        request.setResolvedByEmail("payment:mercado-pago");
                        upgradeRequestRepository.save(request);
                    }
                });

        auditService.record(
                "payment:mercado-pago",
                "PAYMENT_SUBSCRIPTION_ACTIVATED",
                "USER_SUBSCRIPTION",
                subscription.getId(),
                "Pagamento " + paymentReference
                        + " confirmou plano " + purchasedPlan.name()
                        + " (" + billingPeriod.name() + ") para " + user.getEmail()
        );

        return subscription;
    }

    private void expireSubscriptionIfNeeded(UserSubscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getPlanCode() != SubscriptionPlan.FREE
                && subscription.getEndsAt() != null
                && !subscription.getEndsAt().isAfter(OffsetDateTime.now())) {
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
            subscriptionRepository.save(subscription);
        }
    }

    private MySubscriptionResponse toMyResponse(
            UserSubscription subscription,
            SubscriptionUpgradeRequest pending
    ) {
        MySubscriptionResponse.PendingUpgrade pendingResponse = pending == null
                ? null
                : new MySubscriptionResponse.PendingUpgrade(
                        pending.getId(),
                        pending.getRequestedPlan(),
                        pending.getBillingPeriod(),
                        pending.getStatus(),
                        pending.getCreatedAt()
                );

        return new MySubscriptionResponse(
                subscription.getUser().getId(),
                subscription.getPlanCode(),
                subscription.getStatus(),
                subscription.getSource(),
                subscription.getBillingPeriod(),
                subscription.getStartedAt(),
                subscription.getEndsAt(),
                subscription.getCanceledAt(),
                pendingResponse
        );
    }

    private AdminUpgradeRequestResponse toAdminResponse(
            SubscriptionUpgradeRequest request
    ) {
        var currentPlan = subscriptionRepository
                .findByUserId(request.getUser().getId())
                .map(UserSubscription::getPlanCode)
                .orElse(SubscriptionPlan.FREE);

        return new AdminUpgradeRequestResponse(
                request.getId(),
                request.getUser().getId(),
                request.getUser().getName(),
                request.getUser().getEmail(),
                currentPlan,
                request.getRequestedPlan(),
                request.getBillingPeriod(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getResolvedAt(),
                request.getResolvedByEmail()
        );
    }

    private SubscriptionUpgradeRequest pendingRequest(Long requestId) {
        var request = upgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Solicitação de upgrade não encontrada"
                ));

        if (request.getStatus() != UpgradeRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A solicitação já foi processada"
            );
        }

        return request;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));
    }

    private User requireAdmin(String email) {
        var user = findUser(email);

        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso restrito ao administrador"
            );
        }

        return user;
    }
}

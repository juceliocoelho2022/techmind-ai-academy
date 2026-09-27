package br.com.techmind.academy.subscription;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionUpgradeRequestRepository
        extends JpaRepository<SubscriptionUpgradeRequest, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<SubscriptionUpgradeRequest> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            UpgradeRequestStatus status
    );

    @EntityGraph(attributePaths = "user")
    List<SubscriptionUpgradeRequest> findByStatusOrderByCreatedAtDesc(
            UpgradeRequestStatus status
    );

    @EntityGraph(attributePaths = "user")
    List<SubscriptionUpgradeRequest> findAllByOrderByCreatedAtDesc();
}

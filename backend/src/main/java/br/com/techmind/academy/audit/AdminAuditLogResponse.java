package br.com.techmind.academy.audit;

import java.time.OffsetDateTime;

public record AdminAuditLogResponse(
        Long id,
        String actorEmail,
        String action,
        String entityType,
        String entityId,
        String summary,
        OffsetDateTime createdAt
) {
    static AdminAuditLogResponse from(AdminAuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getActorEmail(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getSummary(),
                log.getCreatedAt()
        );
    }
}

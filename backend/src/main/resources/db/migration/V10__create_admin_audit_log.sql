CREATE TABLE admin_audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_email VARCHAR(200) NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(120),
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_audit_log_created_at
    ON admin_audit_log(created_at DESC);

CREATE INDEX idx_admin_audit_log_actor_email
    ON admin_audit_log(actor_email);

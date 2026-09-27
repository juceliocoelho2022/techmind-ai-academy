CREATE TABLE user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
    plan_code VARCHAR(20) NOT NULL DEFAULT 'FREE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(20) NOT NULL DEFAULT 'FREE',
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_subscription_plan CHECK (plan_code IN ('FREE', 'PRO', 'CAREER')),
    CONSTRAINT ck_subscription_status CHECK (status IN ('ACTIVE', 'CANCELED', 'PAST_DUE')),
    CONSTRAINT ck_subscription_source CHECK (source IN ('FREE', 'MANUAL', 'PAYMENT'))
);

CREATE TABLE subscription_upgrade_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    requested_plan VARCHAR(20) NOT NULL,
    billing_period VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by_email VARCHAR(200),
    CONSTRAINT ck_upgrade_plan CHECK (requested_plan IN ('PRO', 'CAREER')),
    CONSTRAINT ck_upgrade_billing_period CHECK (billing_period IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_upgrade_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE UNIQUE INDEX uq_pending_upgrade_per_user
    ON subscription_upgrade_requests(user_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_upgrade_requests_status_created
    ON subscription_upgrade_requests(status, created_at DESC);

INSERT INTO user_subscriptions (
    user_id,
    plan_code,
    status,
    source
)
SELECT
    id,
    'FREE',
    'ACTIVE',
    'FREE'
FROM app_users
ON CONFLICT (user_id) DO NOTHING;

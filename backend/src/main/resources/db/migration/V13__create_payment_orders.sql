CREATE TABLE payment_orders (
    id BIGSERIAL PRIMARY KEY,
    external_reference VARCHAR(80) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    plan_code VARCHAR(20) NOT NULL,
    billing_period VARCHAR(20) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    provider VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_preference_id VARCHAR(120),
    provider_payment_id VARCHAR(120) UNIQUE,
    checkout_url TEXT,
    provider_status VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_payment_order_plan CHECK (plan_code IN ('PRO', 'CAREER')),
    CONSTRAINT ck_payment_order_billing CHECK (billing_period IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_payment_order_provider CHECK (provider IN ('MERCADO_PAGO')),
    CONSTRAINT ck_payment_order_status CHECK (
        status IN ('CREATED', 'CHECKOUT_CREATED', 'PENDING', 'PAID', 'FAILED', 'CANCELED')
    ),
    CONSTRAINT ck_payment_order_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_payment_orders_user_created
    ON payment_orders(user_id, created_at DESC);

CREATE INDEX idx_payment_orders_status_created
    ON payment_orders(status, created_at DESC);

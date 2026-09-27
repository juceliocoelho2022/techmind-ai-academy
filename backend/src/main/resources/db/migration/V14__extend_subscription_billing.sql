ALTER TABLE user_subscriptions
    ADD COLUMN billing_period VARCHAR(20);

ALTER TABLE user_subscriptions
    ADD COLUMN canceled_at TIMESTAMP WITH TIME ZONE;

UPDATE user_subscriptions us
SET billing_period = latest.billing_period
FROM (
    SELECT DISTINCT ON (user_id)
        user_id,
        billing_period
    FROM payment_orders
    WHERE status = 'PAID'
    ORDER BY
        user_id,
        COALESCE(paid_at, created_at) DESC,
        id DESC
) latest
WHERE us.user_id = latest.user_id
  AND us.source = 'PAYMENT'
  AND us.plan_code IN ('PRO', 'CAREER');

UPDATE user_subscriptions us
SET billing_period = latest.billing_period
FROM (
    SELECT DISTINCT ON (user_id)
        user_id,
        billing_period
    FROM subscription_upgrade_requests
    WHERE status = 'APPROVED'
    ORDER BY
        user_id,
        COALESCE(resolved_at, created_at) DESC,
        id DESC
) latest
WHERE us.user_id = latest.user_id
  AND us.source = 'MANUAL'
  AND us.plan_code IN ('PRO', 'CAREER')
  AND us.billing_period IS NULL;

UPDATE user_subscriptions
SET billing_period = 'MONTHLY'
WHERE plan_code IN ('PRO', 'CAREER')
  AND billing_period IS NULL;

ALTER TABLE user_subscriptions
    ADD CONSTRAINT ck_subscription_billing_period
    CHECK (billing_period IS NULL OR billing_period IN ('MONTHLY', 'ANNUAL'));

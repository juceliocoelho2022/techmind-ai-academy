ALTER TABLE user_subscriptions
    ADD COLUMN billing_period VARCHAR(20);

ALTER TABLE user_subscriptions
    ADD COLUMN canceled_at TIMESTAMP WITH TIME ZONE;

UPDATE user_subscriptions
SET billing_period = 'MONTHLY'
WHERE plan_code IN ('PRO', 'CAREER')
  AND billing_period IS NULL;

ALTER TABLE user_subscriptions
    ADD CONSTRAINT ck_subscription_billing_period
    CHECK (billing_period IS NULL OR billing_period IN ('MONTHLY', 'ANNUAL'));

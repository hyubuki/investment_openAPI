CREATE TABLE account_restrictions (
    restriction_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    restriction_type VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_until TIMESTAMP WITH TIME ZONE,
    released_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_account_restrictions_account
        FOREIGN KEY (account_id) REFERENCES trading_accounts (account_id),
    CONSTRAINT chk_account_restrictions_type
        CHECK (restriction_type IN (
            'BUY_BLOCKED', 'SELL_BLOCKED', 'ALL_TRADING_BLOCKED', 'WITHDRAWAL_BLOCKED'
        )),
    CONSTRAINT chk_account_restrictions_reason
        CHECK (CHAR_LENGTH(TRIM(reason_code)) > 0),
    CONSTRAINT chk_account_restrictions_effective_period
        CHECK (effective_until IS NULL OR effective_until > effective_from)
);

CREATE INDEX idx_account_restrictions_decision
    ON account_restrictions (
        account_id,
        restriction_type,
        effective_from,
        effective_until,
        released_at
    );

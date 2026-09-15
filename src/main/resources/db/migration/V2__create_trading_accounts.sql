CREATE TABLE trading_accounts (
    account_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    account_number_encrypted VARCHAR(512) NOT NULL,
    account_number_hash VARCHAR(64) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_trading_accounts_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uk_trading_accounts_number_hash UNIQUE (account_number_hash),
    CONSTRAINT chk_trading_accounts_encrypted_not_empty
        CHECK (account_number_encrypted <> ''),
    CONSTRAINT chk_trading_accounts_hash_length
        CHECK (CHAR_LENGTH(account_number_hash) = 64),
    CONSTRAINT chk_trading_accounts_type
        CHECK (account_type IN ('BROKERAGE')),
    CONSTRAINT chk_trading_accounts_currency
        CHECK (CHAR_LENGTH(base_currency) = 3 AND base_currency = UPPER(base_currency)),
    CONSTRAINT chk_trading_accounts_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'RESTRICTED', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT chk_trading_accounts_closed_at
        CHECK (
            (status = 'CLOSED' AND closed_at IS NOT NULL)
            OR (status <> 'CLOSED' AND closed_at IS NULL)
        )
);

CREATE INDEX idx_trading_accounts_user_status
    ON trading_accounts (user_id, status, account_id);

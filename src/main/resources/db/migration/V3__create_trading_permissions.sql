CREATE TABLE trading_permissions (
    permission_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    market VARCHAR(16) NOT NULL,
    asset_class VARCHAR(32) NOT NULL,
    side VARCHAR(16) NOT NULL,
    order_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_until TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_trading_permissions_account
        FOREIGN KEY (account_id) REFERENCES trading_accounts (account_id),
    CONSTRAINT chk_trading_permissions_market
        CHECK (market IN ('KR', 'US')),
    CONSTRAINT chk_trading_permissions_asset_class
        CHECK (asset_class IN ('EQUITY')),
    CONSTRAINT chk_trading_permissions_side
        CHECK (side IN ('BUY', 'SELL', 'BOTH')),
    CONSTRAINT chk_trading_permissions_order_type
        CHECK (order_type IN ('LIMIT', 'MARKET')),
    CONSTRAINT chk_trading_permissions_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    CONSTRAINT chk_trading_permissions_effective_period
        CHECK (effective_until IS NULL OR effective_until > effective_from),
    CONSTRAINT uk_trading_permissions_scope_start
        UNIQUE (account_id, market, asset_class, side, order_type, effective_from)
);

CREATE INDEX idx_trading_permissions_decision
    ON trading_permissions (
        account_id,
        market,
        asset_class,
        order_type,
        status,
        side,
        effective_from,
        effective_until
    );

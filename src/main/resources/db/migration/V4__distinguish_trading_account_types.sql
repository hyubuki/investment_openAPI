ALTER TABLE trading_accounts DROP CONSTRAINT chk_trading_accounts_type;

UPDATE trading_accounts
SET account_type = 'GENERAL_BROKERAGE'
WHERE account_type = 'BROKERAGE';

ALTER TABLE trading_accounts ADD CONSTRAINT chk_trading_accounts_type
    CHECK (account_type IN ('GENERAL_BROKERAGE', 'ISA_BROKERAGE'));

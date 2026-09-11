CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    failed_login_count INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_email_not_empty CHECK (email <> ''),
    CONSTRAINT chk_users_email_normalized CHECK (email = LOWER(TRIM(email))),
    CONSTRAINT chk_users_email_bytes CHECK (OCTET_LENGTH(email) <= 100),
    CONSTRAINT chk_users_password_hash_not_empty CHECK (password_hash <> ''),
    CONSTRAINT chk_users_failed_login_count CHECK (failed_login_count >= 0),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED', 'DISABLED'))
);

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_created_at ON users (created_at);

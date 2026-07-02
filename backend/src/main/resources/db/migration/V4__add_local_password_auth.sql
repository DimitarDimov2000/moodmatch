ALTER TABLE app_users DROP CONSTRAINT chk_app_users_provider;

ALTER TABLE app_users ADD COLUMN password_hash TEXT;
ALTER TABLE app_users ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE app_users ADD CONSTRAINT uk_app_users_email UNIQUE (email);

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_auth_sessions_app_users FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT uk_auth_sessions_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_auth_sessions_user_id ON auth_sessions (user_id);
CREATE INDEX idx_auth_sessions_expires_at ON auth_sessions (expires_at);

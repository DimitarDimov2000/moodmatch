CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email VARCHAR(320),
    display_name VARCHAR(255),
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_app_users_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT chk_app_users_provider
            CHECK (provider IN ('LOCAL', 'GOOGLE', 'OIDC'))
);

INSERT INTO app_users (id, provider, provider_subject, email, display_name, avatar_url, created_at, updated_at)
SELECT
    CAST('00000000-0000-0000-0000-000000000001' AS UUID),
    'LOCAL',
    'local-demo-user',
    'local-demo@example.local',
    'Local Demo User',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM app_users
    WHERE provider = 'LOCAL'
      AND provider_subject = 'local-demo-user'
);

ALTER TABLE media_items ADD COLUMN user_id UUID;

UPDATE media_items
SET user_id = (
    SELECT id
    FROM app_users
    WHERE provider = 'LOCAL'
      AND provider_subject = 'local-demo-user'
)
WHERE user_id IS NULL;

ALTER TABLE media_items ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE media_items
    ADD CONSTRAINT fk_media_items_app_users FOREIGN KEY (user_id) REFERENCES app_users (id);

CREATE INDEX idx_media_items_user_id ON media_items (user_id);

CREATE TABLE IF NOT EXISTS user_oauth_bindings (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(128) NOT NULL,
    provider_union_id VARCHAR(128) NULL,
    provider_username VARCHAR(100) NULL,
    provider_nickname VARCHAR(100) NULL,
    provider_avatar VARCHAR(500) NULL,
    provider_email VARCHAR(150) NULL,
    email_verified TINYINT(1) NOT NULL DEFAULT 0,
    bound_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth_provider_identity (provider, provider_user_id),
    UNIQUE KEY uk_oauth_user_provider (user_id, provider),
    KEY idx_oauth_user (user_id),
    CONSTRAINT fk_user_oauth_bindings_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_oauth_binding_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(128) NULL,
    action VARCHAR(32) NOT NULL,
    message VARCHAR(255) NULL,
    ip VARCHAR(64) NULL,
    user_agent VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_oauth_logs_user_time (user_id, created_at),
    KEY idx_oauth_logs_provider_identity (provider, provider_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

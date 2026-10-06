-- B100: AI模型网关配置
-- 使用高版本号是为了兼容已有数据库中较高的历史迁移版本。

CREATE TABLE IF NOT EXISTS ai_model_providers (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    provider_key VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    api_type VARCHAR(40) NOT NULL DEFAULT 'OPENAI_COMPATIBLE',
    base_url VARCHAR(255) NOT NULL,
    api_key VARCHAR(500) DEFAULT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_model_provider_key (provider_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_model_configs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    provider_id BIGINT UNSIGNED NOT NULL,
    model_id VARCHAR(180) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    cost_multiplier DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    context_window INT DEFAULT NULL,
    capability_tags VARCHAR(255) DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_model_provider_model (provider_id, model_id),
    KEY idx_ai_model_enabled_sort (enabled, sort_order),
    CONSTRAINT fk_ai_model_configs_provider FOREIGN KEY (provider_id) REFERENCES ai_model_providers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ai_model_providers (provider_key, name, api_type, base_url, api_key, enabled, sort_order)
VALUES ('siliconflow', 'SiliconFlow', 'OPENAI_COMPATIBLE', 'https://api.siliconflow.cn/v1', '', 1, 10)
ON DUPLICATE KEY UPDATE
name = VALUES(name),
api_type = VALUES(api_type),
base_url = VALUES(base_url),
enabled = VALUES(enabled),
sort_order = VALUES(sort_order);

INSERT INTO ai_model_configs (provider_id, model_id, display_name, enabled, cost_multiplier, context_window, capability_tags, sort_order)
SELECT p.id, m.model_id, m.display_name, 1, m.cost_multiplier, m.context_window, m.capability_tags, m.sort_order
FROM ai_model_providers p
JOIN (
    SELECT 'Qwen/Qwen3-8B' AS model_id, 'Qwen3 8B（免费·通用）' AS display_name, 0.10 AS cost_multiplier, 8192 AS context_window, 'chat,code,free' AS capability_tags, 10 AS sort_order
    UNION ALL SELECT 'THUDM/GLM-4-9B-0414', 'GLM-4 9B（免费·开源）', 0.20, 8192, 'chat,code,free', 20
    UNION ALL SELECT 'Qwen/Qwen3-32B', 'Qwen3 32B（高性能）', 0.80, 32768, 'chat,code', 30
    UNION ALL SELECT 'deepseek-ai/DeepSeek-V3', 'DeepSeek V3（推理强）', 1.00, 32768, 'chat,code,reasoning', 40
    UNION ALL SELECT 'deepseek-ai/DeepSeek-V3.2', 'DeepSeek V3.2（旗舰）', 2.00, 65536, 'chat,code,reasoning', 50
    UNION ALL SELECT 'Pro/zai-org/GLM-4.7', 'GLM-4.7 Pro（旗舰）', 2.00, 65536, 'chat,code,reasoning', 60
    UNION ALL SELECT 'Pro/zai-org/GLM-5', 'GLM-5 Pro（旗舰）', 2.50, 65536, 'chat,code,reasoning', 70
    UNION ALL SELECT 'Pro/moonshotai/Kimi-K2.5', 'Kimi K2.5（长上下文）', 2.50, 128000, 'chat,code,long-context', 80
    UNION ALL SELECT 'Pro/zai-org/GLM-5.1', 'GLM-5.1 Pro（最强）', 3.00, 128000, 'chat,code,reasoning', 90
) m
WHERE p.provider_key = 'siliconflow'
ON DUPLICATE KEY UPDATE
display_name = VALUES(display_name),
cost_multiplier = VALUES(cost_multiplier),
context_window = VALUES(context_window),
capability_tags = VALUES(capability_tags),
sort_order = VALUES(sort_order);

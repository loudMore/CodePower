/**
 * 文件说明：第三方授权登录 配置类，负责系统框架、中间件或外部服务的参数装配。
 */
package com.ls.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 第三方授权登录配置 */
@Data
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    private Provider github = new Provider();
    private Provider gitee = new Provider();
    private Provider qq = new Provider();
    private Provider wechat = new Provider();
    private String frontendCallbackUrl = "http://localhost:5173/oauth/callback";

    @Data
    public static class Provider {
        private boolean enabled = false;
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";

        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank()
                    && redirectUri != null && !redirectUri.isBlank();
        }
    }
}

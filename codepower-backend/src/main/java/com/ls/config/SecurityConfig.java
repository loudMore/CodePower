/**
 * 文件说明：S ec ur it y 配置类，负责系统框架、中间件或外部服务的参数装配。
 */
package com.ls.config;

import com.ls.utils.JwtUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.DispatcherType;
import java.util.Arrays;

/**
 * 全局安全配置 —— 唯一的权限管理入口。
 *
 * 权限层级（按匹配顺序，命中即停止）：
 *   /api/auth/**, /api/oauth/**, /api/verification/** → 放行（登录注册相关）
 *   GET 题目列表/竞赛/题解/公告 → 放行（公开可读）
 *   /api/admin/** → 仅 ADMIN 角色
 *   创建/修改题目和竞赛 → SENIOR_USER 或 ADMIN
 *   其余所有请求 → 需登录认证
 *
 * 会话策略：STATELESS，不依赖服务端 Session，每次请求都需携带 JWT。
 * 密码加密：BCrypt。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 禁止 Spring Boot 将 JwtAuthenticationFilter 自动注册为 Servlet Filter
     * 只让它在 Spring Security 过滤链内执行
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/auth/**", "/api/oauth/**", "/api/verification/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/problems", "/api/problems/{id}/view", "/api/tags/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/solutions/problem/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/learning-paths", "/api/learning-paths/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/contests", "/api/contests/{id}", "/api/contests/{id}/ranking").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comments/target/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/data-analysis/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/announcements").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/code/submit/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/problem-reports/*/handle").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/problems").hasAnyRole("SENIOR_USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/problems/**").hasAnyRole("SENIOR_USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/contests").hasAnyRole("SENIOR_USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/contests/**").hasAnyRole("SENIOR_USER", "ADMIN")
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            // 无状态会话 —— 不依赖服务端 Session，每次请求都需携带 JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(401);
                    response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(403);
                    response.getWriter().write("{\"code\":403,\"message\":\"没有权限执行此操作\",\"data\":null}");
                })
            )
            // JWT过滤器放在 UsernamePasswordAuthenticationFilter 之前，确保先解析 token 再判断权限
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 全局唯一 CORS 配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

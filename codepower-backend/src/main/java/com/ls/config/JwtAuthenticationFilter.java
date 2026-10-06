/**
 * 文件说明：J wt Au th en ti ca ti on Fi lt er 配置类，负责系统框架、中间件或外部服务的参数装配。
 */
package com.ls.config;

import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器 —— 每次HTTP请求到达时拦截并验证JWT令牌。
 *
 * 认证链路：
 * 1. 从请求头 Authorization 中提取 Bearer token
 * 2. 解析JWT拿到 userId 和 username
 * 3. 优先通过 userId 直接查数据库验证用户存在、未被删除、未被禁用
 * 4. 验证通过后将用户信息写入 SecurityContextHolder，后续接口通过 @AuthenticationPrincipal 获取当前用户
 * 5. 解析失败不中断请求链，由 Spring Security 后续统一返回 401
 *
 * 注意：虽然标注了 @Component，但 SecurityConfig 禁止了 Servlet 容器的自动注册，
 * 只让它在 Spring Security 过滤链内部执行，避免重复过滤。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);
            try {
                String username = jwtUtil.extractUsername(jwt);
                Long userId = jwtUtil.extractUserId(jwt);
                logger.debug("JWT解析成功, username=" + username + ", userId=" + userId + ", uri=" + request.getRequestURI());

                if ((username != null || userId != null) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = loadUserDetails(username, userId);
                    logger.debug("UserDetails加载成功, authorities=" + userDetails.getAuthorities());

                    if (isTokenValidForPrincipal(jwt, userDetails, userId)) {
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        logger.debug("SecurityContext已设置认证信息");
                    } else {
                        logger.warn("JWT验证失败: token无效或已过期, uri=" + request.getRequestURI());
                    }
                }
            } catch (Exception e) {
                logger.error("JWT解析失败, uri=" + request.getRequestURI(), e);
            }
        } else if (authorizationHeader != null) {
            logger.warn("Authorization头格式不正确: " + authorizationHeader.substring(0, Math.min(20, authorizationHeader.length())) + "...");
        }

        filterChain.doFilter(request, response);
    }

    private UserDetails loadUserDetails(String username, Long userId) {
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null
                    && !Integer.valueOf(1).equals(user.getDeleted())
                    && !Integer.valueOf(0).equals(user.getStatus())) {
                return new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );
            }
        }
        return this.userDetailsService.loadUserByUsername(username);
    }

    private boolean isTokenValidForPrincipal(String jwt, UserDetails userDetails, Long userId) {
        if (userId != null && jwtUtil.validateTokenForUserId(jwt, userId)) {
            return true;
        }
        return jwtUtil.validateToken(jwt, userDetails);
    }
}

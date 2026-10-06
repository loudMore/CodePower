/**
 * WebSocket 握手鉴权拦截器。
 *
 * 浏览器建立 WebSocket 连接时不能设置自定义 HTTP 头，因此 JWT 通过 URL 参数传递：
 *   ws://host/ws/messages?token=<JWT>
 *
 * beforeHandshake 从 URL query 参数取出 token → 解析 JWT → 查库验证用户 → 存入 WebSocket session attributes。
 */
package com.ls.websocket;

import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Date;
import java.util.Map;

/** WebSocket 握手鉴权：浏览器无法稳定设置 Authorization 头，因此允许 token 放在查询参数。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Long userId = jwtUtil.extractUserId(token);
            String username = jwtUtil.extractUsername(token);
            Date expiration = jwtUtil.extractExpiration(token);
            if (userId == null || username == null || expiration == null || expiration.before(new Date())) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            User user = userMapper.selectById(userId);
            if (user == null
                    || Integer.valueOf(1).equals(user.getDeleted())
                    || Integer.valueOf(0).equals(user.getStatus())) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            attributes.put("userId", user.getId());
            attributes.put("username", user.getUsername());
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 鉴权失败: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手结束后无需额外清理，连接状态由 WebSocketHandler 维护。
    }

    private String resolveToken(ServerHttpRequest request) {
        String queryToken = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return stripBearerPrefix(queryToken);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return stripBearerPrefix(authHeader);
    }

    private String stripBearerPrefix(String token) {
        if (token == null) {
            return null;
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}

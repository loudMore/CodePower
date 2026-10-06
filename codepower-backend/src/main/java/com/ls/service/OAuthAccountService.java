/**
 * 文件说明：第三方账号绑定 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ls.config.OAuthProperties;
import com.ls.domain.User;
import com.ls.domain.UserOauthBinding;
import com.ls.domain.UserOauthBindingLog;
import com.ls.domain.UserProfile;
import com.ls.domain.dto.OAuthBindExistingRequest;
import com.ls.domain.dto.OAuthCreateAccountRequest;
import com.ls.mapper.UserMapper;
import com.ls.mapper.UserOauthBindingLogMapper;
import com.ls.mapper.UserOauthBindingMapper;
import com.ls.mapper.UserProfileMapper;
import com.ls.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 第三方授权登录、绑定和账号补全服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthAccountService {
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final Duration LINK_SESSION_TTL = Duration.ofMinutes(15);
    private static final Duration LOGIN_SESSION_TTL = Duration.ofMinutes(5);
    private static final String STATE_KEY_PREFIX = "oauth:state:";
    private static final String LINK_KEY_PREFIX = "oauth:link:";
    private static final String LOGIN_KEY_PREFIX = "oauth:login:";
    private static final List<String> ACTIVE_OAUTH_PROVIDERS = List.of("github", "gitee");

    private final OAuthProperties oauthProperties;
    private final RedisCacheService redisCacheService;
    private final UserOauthBindingMapper bindingMapper;
    private final UserOauthBindingLogMapper bindingLogMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final RegionService regionService;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> listProviders() {
        List<Map<String, Object>> providers = new ArrayList<>();
        providers.add(providerStatus("github", "GitHub", oauthProperties.getGithub()));
        providers.add(providerStatus("gitee", "Gitee", oauthProperties.getGitee()));
        providers.add(providerStatus("wechat", "微信", oauthProperties.getWechat()));
        providers.add(providerStatus("qq", "QQ", oauthProperties.getQq()));
        return providers;
    }

    public Map<String, Object> buildAuthorizeUrl(String provider,
                                                 String mode,
                                                 String redirect,
                                                 String authorization) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedMode = normalizeMode(mode);
        if (!isActiveOAuthProvider(normalizedProvider)) {
            throw new IllegalArgumentException(unsupportedProviderMessage(normalizedProvider));
        }
        OAuthProperties.Provider config = getProviderConfig(normalizedProvider);
        if (!config.isEnabled()) {
            throw new IllegalArgumentException(providerLabel(normalizedProvider) + " 授权登录暂未开启");
        }
        if (!config.isConfigured()) {
            throw new IllegalArgumentException(providerLabel(normalizedProvider) + " 授权应用尚未配置或仍在审核中");
        }

        Map<String, Object> stateData = new LinkedHashMap<>();
        stateData.put("provider", normalizedProvider);
        stateData.put("mode", normalizedMode);
        stateData.put("redirect", normalizeRedirect(redirect));
        if ("bind".equals(normalizedMode)) {
            User user = resolveUserFromAuthorization(authorization);
            if (user == null) {
                throw new IllegalArgumentException("请先登录后再绑定第三方账号");
            }
            stateData.put("userId", user.getId());
        }

        String state = UUID.randomUUID().toString().replace("-", "");
        redisCacheService.set(STATE_KEY_PREFIX + state, stateData, STATE_TTL);
        return Map.of("url", buildProviderAuthorizeUrl(normalizedProvider, config, state));
    }

    public String handleCallback(String provider, String code, String state, String error, String errorDescription) {
        String normalizedProvider = normalizeProvider(provider);
        if (error != null && !error.isBlank()) {
            return frontendCallbackUrl(Map.of("oauth_error", firstNonBlank(errorDescription, error)));
        }
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            return frontendCallbackUrl(Map.of("oauth_error", "授权回调参数不完整，请重新尝试"));
        }

        Map<String, Object> stateData = readMap(STATE_KEY_PREFIX + state);
        redisCacheService.delete(STATE_KEY_PREFIX + state);
        if (stateData == null || !normalizedProvider.equals(String.valueOf(stateData.get("provider")))) {
            return frontendCallbackUrl(Map.of("oauth_error", "授权状态已过期，请重新尝试"));
        }

        try {
            OAuthIdentity identity = fetchIdentity(normalizedProvider, code);
            if (identity.providerUserId == null || identity.providerUserId.isBlank()) {
                throw new IllegalStateException("第三方平台未返回稳定用户标识");
            }

            String mode = String.valueOf(stateData.get("mode"));
            if ("bind".equals(mode)) {
                Long userId = toLong(stateData.get("userId"));
                User user = userId == null ? null : userMapper.selectById(userId);
                if (user == null) {
                    return frontendCallbackUrl(Map.of("oauth_bind_error", "当前登录用户不存在，请重新登录"));
                }
                bindIdentityToUser(user, identity, "BIND");
                return frontendCallbackUrl(Map.of(
                        "oauth_bind", "success",
                        "provider", normalizedProvider
                ));
            }

            UserOauthBinding existing = findBinding(identity.provider, identity.providerUserId);
            if (existing != null) {
                User user = userMapper.selectById(existing.getUserId());
                if (user == null || !isUsableUser(user)) {
                    return frontendCallbackUrl(Map.of("oauth_error", "绑定的 CodePower 账号状态异常，请联系管理员"));
                }
                existing.setLastLoginAt(LocalDateTime.now());
                bindingMapper.updateById(existing);
                String loginId = createLoginSession(user, normalizeRedirect(String.valueOf(stateData.get("redirect"))));
                return frontendCallbackUrl(Map.of("loginId", loginId));
            }

            String sessionId = createLinkSession(identity, normalizeRedirect(String.valueOf(stateData.get("redirect"))));
            return frontendCallbackUrl(Map.of(
                    "sessionId", sessionId,
                    "provider", normalizedProvider
            ));
        } catch (Exception e) {
            log.warn("OAuth callback failed, provider={}", normalizedProvider, e);
            return frontendCallbackUrl(Map.of("oauth_error", "授权登录失败：" + firstNonBlank(e.getMessage(), "请稍后重试")));
        }
    }

    public Map<String, Object> consumeLoginSession(String loginId) {
        Map<String, Object> data = readMap(LOGIN_KEY_PREFIX + loginId);
        if (data == null) {
            throw new IllegalArgumentException("登录会话已过期，请重新授权");
        }
        redisCacheService.delete(LOGIN_KEY_PREFIX + loginId);
        return data;
    }

    public Map<String, Object> getLinkSession(String sessionId) {
        Map<String, Object> session = readMap(LINK_KEY_PREFIX + sessionId);
        if (session == null) {
            throw new IllegalArgumentException("授权会话已过期，请重新授权");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("provider", session.get("provider"));
        data.put("providerLabel", providerLabel(String.valueOf(session.get("provider"))));
        data.put("providerUsername", session.get("providerUsername"));
        data.put("providerNickname", session.get("providerNickname"));
        data.put("providerAvatar", session.get("providerAvatar"));
        data.put("providerEmail", session.get("providerEmail"));
        data.put("redirect", normalizeRedirect(String.valueOf(session.get("redirect"))));
        return data;
    }

    @Transactional
    public Map<String, Object> bindExistingAccount(String sessionId, OAuthBindExistingRequest request) {
        Map<String, Object> session = readMap(LINK_KEY_PREFIX + sessionId);
        if (session == null) {
            throw new IllegalArgumentException("授权会话已过期，请重新授权");
        }
        String account = request == null ? null : request.getAccount();
        String password = request == null ? null : request.getPassword();
        if (account == null || account.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("请输入用户名/邮箱和密码");
        }

        User user = findUserByAccount(account.trim());
        if (user == null || !isUsableUser(user) || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名/邮箱或密码错误");
        }

        OAuthIdentity identity = identityFromSession(session);
        bindIdentityToUser(user, identity, "LOGIN_BIND");
        redisCacheService.delete(LINK_KEY_PREFIX + sessionId);
        return buildLoginPayload(user, normalizeRedirect(String.valueOf(session.get("redirect"))));
    }

    @Transactional
    public Map<String, Object> createAccountAndBind(String sessionId, OAuthCreateAccountRequest request) {
        Map<String, Object> session = readMap(LINK_KEY_PREFIX + sessionId);
        if (session == null) {
            throw new IllegalArgumentException("授权会话已过期，请重新授权");
        }
        validateCreateAccountRequest(request);
        OAuthIdentity identity = identityFromSession(session);
        if (findBinding(identity.provider, identity.providerUserId) != null) {
            throw new IllegalArgumentException("该第三方账号已绑定 CodePower 账号，请直接登录");
        }

        String rawEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!authService.verifyEmailCode(rawEmail, request.getVerificationCode())) {
            throw new IllegalArgumentException("验证码错误或已失效");
        }

        String email = rawEmail;
        if (userMapper.findByUsername(request.getUsername().trim()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userMapper.findByEmail(email) != null) {
            throw new IllegalArgumentException("该邮箱已被注册，请选择绑定已有账号");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("NORMAL_USER");
        user.setStatus(1);
        userMapper.insert(user);

        String requestedRegion = regionService.normalizeRegion(request.getRegion());
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setRegion(requestedRegion != null ? requestedRegion : regionService.resolveCurrentRequestRegion());
        profile.setAvatarUrl(firstNonBlank(identity.avatar, "/avatars/avatar-1.svg"));
        userProfileMapper.insert(profile);

        bindIdentityToUser(user, identity, "REGISTER_BIND");
        notificationService.createNotification(user.getId(), "WELCOME",
                "欢迎加入 CodePower！",
                "欢迎你，" + user.getUsername() + "！你的账号已完成邮箱验证，并绑定了 "
                        + providerLabel(identity.provider) + " 授权登录。",
                null);
        redisCacheService.delete(LINK_KEY_PREFIX + sessionId);
        return buildLoginPayload(user, normalizeRedirect(String.valueOf(session.get("redirect"))));
    }

    public List<Map<String, Object>> listMyBindings(String authorization) {
        User user = requireUserFromAuthorization(authorization);
        List<UserOauthBinding> bindings = bindingMapper.selectList(
                new LambdaQueryWrapper<UserOauthBinding>()
                        .eq(UserOauthBinding::getUserId, user.getId())
                        .orderByAsc(UserOauthBinding::getProvider));
        Map<String, UserOauthBinding> bindingMap = new LinkedHashMap<>();
        for (UserOauthBinding binding : bindings) {
            bindingMap.put(binding.getProvider(), binding);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> provider : listProviders()) {
            String code = String.valueOf(provider.get("provider"));
            UserOauthBinding binding = bindingMap.get(code);
            Map<String, Object> item = new LinkedHashMap<>(provider);
            item.put("bound", binding != null);
            if (binding != null) {
                item.put("providerUsername", binding.getProviderUsername());
                item.put("providerNickname", binding.getProviderNickname());
                item.put("providerAvatar", binding.getProviderAvatar());
                item.put("providerEmail", binding.getProviderEmail());
                item.put("boundAt", binding.getBoundAt());
                item.put("lastLoginAt", binding.getLastLoginAt());
            }
            result.add(item);
        }
        return result;
    }

    @Transactional
    public void unlinkMyBinding(String provider, String authorization) {
        User user = requireUserFromAuthorization(authorization);
        String normalizedProvider = normalizeProvider(provider);
        UserOauthBinding binding = bindingMapper.selectOne(
                new LambdaQueryWrapper<UserOauthBinding>()
                        .eq(UserOauthBinding::getUserId, user.getId())
                        .eq(UserOauthBinding::getProvider, normalizedProvider));
        if (binding == null) {
            return;
        }
        bindingMapper.deleteById(binding.getId());
        writeLog(user.getId(), normalizedProvider, binding.getProviderUserId(), "UNBIND", "用户主动解绑");
    }

    private Map<String, Object> providerStatus(String code, String label, OAuthProperties.Provider provider) {
        if (!isActiveOAuthProvider(code)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("provider", code);
            data.put("label", label);
            data.put("enabled", false);
            data.put("configured", false);
            data.put("available", false);
            data.put("message", unsupportedProviderMessage(code));
            return data;
        }
        boolean configured = provider != null && provider.isConfigured();
        boolean enabled = provider != null && provider.isEnabled();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", code);
        data.put("label", label);
        data.put("enabled", enabled);
        data.put("configured", configured);
        data.put("available", enabled && configured);
        data.put("message", enabled && configured ? "可用" : (enabled ? "授权应用尚未配置完整或仍在审核中" : "待配置授权应用"));
        return data;
    }

    private String buildProviderAuthorizeUrl(String provider, OAuthProperties.Provider config, String state) {
        return switch (provider) {
            case "github" -> buildUrl("https://github.com/login/oauth/authorize", Map.of(
                    "client_id", config.getClientId(),
                    "redirect_uri", config.getRedirectUri(),
                    "scope", "read:user user:email",
                    "state", state
            ));
            case "gitee" -> buildUrl("https://gitee.com/oauth/authorize", Map.of(
                    "client_id", config.getClientId(),
                    "redirect_uri", config.getRedirectUri(),
                    "response_type", "code",
                    "scope", "user_info",
                    "state", state
            ));
            case "qq" -> buildUrl("https://graph.qq.com/oauth2.0/authorize", Map.of(
                    "response_type", "code",
                    "client_id", config.getClientId(),
                    "redirect_uri", config.getRedirectUri(),
                    "scope", "get_user_info",
                    "state", state
            ));
            case "wechat" -> buildUrl("https://open.weixin.qq.com/connect/qrconnect", Map.of(
                    "appid", config.getClientId(),
                    "redirect_uri", config.getRedirectUri(),
                    "response_type", "code",
                    "scope", "snsapi_login",
                    "state", state
            )) + "#wechat_redirect";
            default -> throw new IllegalArgumentException("不支持的授权平台");
        };
    }

    private OAuthIdentity fetchIdentity(String provider, String code) {
        OAuthProperties.Provider config = getProviderConfig(provider);
        return switch (provider) {
            case "github" -> fetchGithubIdentity(config, code);
            case "gitee" -> fetchGiteeIdentity(config, code);
            case "qq" -> fetchQqIdentity(config, code);
            case "wechat" -> fetchWechatIdentity(config, code);
            default -> throw new IllegalArgumentException("不支持的授权平台");
        };
    }

    @SuppressWarnings("unchecked")
    private OAuthIdentity fetchGithubIdentity(OAuthProperties.Provider config, String code) {
        Map<String, Object> token = postForm("https://github.com/login/oauth/access_token", Map.of(
                "client_id", config.getClientId(),
                "client_secret", config.getClientSecret(),
                "code", code,
                "redirect_uri", config.getRedirectUri()
        ));
        String accessToken = stringValue(token.get("access_token"));
        if (accessToken == null) {
            throw new IllegalStateException("GitHub 未返回 access_token");
        }
        HttpHeaders headers = bearerHeaders(accessToken);
        Map<String, Object> user = restTemplate.exchange(
                "https://api.github.com/user", HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
        List<Map<String, Object>> emails = List.of();
        try {
            ResponseEntity<List> emailResponse = restTemplate.exchange(
                    "https://api.github.com/user/emails", HttpMethod.GET, new HttpEntity<>(headers), List.class);
            emails = emailResponse.getBody() == null ? List.of() : (List<Map<String, Object>>) (List<?>) emailResponse.getBody();
        } catch (Exception e) {
            log.debug("Fetch GitHub emails failed: {}", e.getMessage());
        }
        String email = stringValue(user == null ? null : user.get("email"));
        boolean emailVerified = false;
        for (Map<String, Object> item : emails) {
            if (Boolean.TRUE.equals(item.get("primary"))) {
                email = firstNonBlank(stringValue(item.get("email")), email);
                emailVerified = Boolean.TRUE.equals(item.get("verified"));
                break;
            }
        }
        OAuthIdentity identity = new OAuthIdentity();
        identity.provider = "github";
        identity.providerUserId = stringValue(user == null ? null : user.get("id"));
        identity.username = stringValue(user == null ? null : user.get("login"));
        identity.nickname = firstNonBlank(stringValue(user == null ? null : user.get("name")), identity.username);
        identity.avatar = stringValue(user == null ? null : user.get("avatar_url"));
        identity.email = email;
        identity.emailVerified = emailVerified;
        return identity;
    }

    private OAuthIdentity fetchGiteeIdentity(OAuthProperties.Provider config, String code) {
        Map<String, Object> token = postForm("https://gitee.com/oauth/token", Map.of(
                "grant_type", "authorization_code",
                "client_id", config.getClientId(),
                "client_secret", config.getClientSecret(),
                "code", code,
                "redirect_uri", config.getRedirectUri()
        ));
        String accessToken = stringValue(token.get("access_token"));
        if (accessToken == null) {
            throw new IllegalStateException("Gitee 未返回 access_token");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> user = restTemplate.getForObject(
                buildUrl("https://gitee.com/api/v5/user", Map.of("access_token", accessToken)), Map.class);
        OAuthIdentity identity = new OAuthIdentity();
        identity.provider = "gitee";
        identity.providerUserId = stringValue(user == null ? null : user.get("id"));
        identity.username = stringValue(user == null ? null : user.get("login"));
        identity.nickname = firstNonBlank(stringValue(user == null ? null : user.get("name")), identity.username);
        identity.avatar = stringValue(user == null ? null : user.get("avatar_url"));
        identity.email = stringValue(user == null ? null : user.get("email"));
        identity.emailVerified = false;
        return identity;
    }

    private OAuthIdentity fetchQqIdentity(OAuthProperties.Provider config, String code) {
        String tokenText = restTemplate.getForObject(buildUrl("https://graph.qq.com/oauth2.0/token", Map.of(
                "grant_type", "authorization_code",
                "client_id", config.getClientId(),
                "client_secret", config.getClientSecret(),
                "code", code,
                "redirect_uri", config.getRedirectUri()
        )), String.class);
        Map<String, String> token = parseQueryString(tokenText);
        String accessToken = token.get("access_token");
        if (accessToken == null) {
            throw new IllegalStateException("QQ 未返回 access_token");
        }
        String openidText = restTemplate.getForObject(buildUrl("https://graph.qq.com/oauth2.0/me", Map.of(
                "access_token", accessToken
        )), String.class);
        Map<String, Object> openidPayload = parseJsonp(openidText);
        String openid = stringValue(openidPayload.get("openid"));
        if (openid == null) {
            throw new IllegalStateException("QQ 未返回 openid");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> user = restTemplate.getForObject(buildUrl("https://graph.qq.com/user/get_user_info", Map.of(
                "access_token", accessToken,
                "oauth_consumer_key", config.getClientId(),
                "openid", openid
        )), Map.class);
        OAuthIdentity identity = new OAuthIdentity();
        identity.provider = "qq";
        identity.providerUserId = openid;
        identity.username = openid;
        identity.nickname = stringValue(user == null ? null : user.get("nickname"));
        identity.avatar = firstNonBlank(
                stringValue(user == null ? null : user.get("figureurl_qq_2")),
                stringValue(user == null ? null : user.get("figureurl_qq_1")),
                stringValue(user == null ? null : user.get("figureurl_2")),
                stringValue(user == null ? null : user.get("figureurl_1"))
        );
        identity.emailVerified = false;
        return identity;
    }

    private OAuthIdentity fetchWechatIdentity(OAuthProperties.Provider config, String code) {
        @SuppressWarnings("unchecked")
        Map<String, Object> token = restTemplate.getForObject(buildUrl("https://api.weixin.qq.com/sns/oauth2/access_token", Map.of(
                "appid", config.getClientId(),
                "secret", config.getClientSecret(),
                "code", code,
                "grant_type", "authorization_code"
        )), Map.class);
        String accessToken = stringValue(token == null ? null : token.get("access_token"));
        String openid = stringValue(token == null ? null : token.get("openid"));
        String unionid = stringValue(token == null ? null : token.get("unionid"));
        if (accessToken == null || openid == null) {
            throw new IllegalStateException("微信未返回 access_token/openid");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> user = restTemplate.getForObject(buildUrl("https://api.weixin.qq.com/sns/userinfo", Map.of(
                "access_token", accessToken,
                "openid", openid,
                "lang", "zh_CN"
        )), Map.class);
        OAuthIdentity identity = new OAuthIdentity();
        identity.provider = "wechat";
        identity.providerUserId = firstNonBlank(unionid, openid);
        identity.unionId = unionid;
        identity.username = openid;
        identity.nickname = stringValue(user == null ? null : user.get("nickname"));
        identity.avatar = stringValue(user == null ? null : user.get("headimgurl"));
        identity.emailVerified = false;
        return identity;
    }

    private Map<String, Object> postForm(String url, Map<String, String> values) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        values.forEach(form::add);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = restTemplate.postForObject(url, new HttpEntity<>(form, headers), Map.class);
        return body == null ? Map.of() : body;
    }

    private HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private UserOauthBinding bindIdentityToUser(User user, OAuthIdentity identity, String action) {
        UserOauthBinding existingIdentity = findBinding(identity.provider, identity.providerUserId);
        if (existingIdentity != null) {
            if (!Objects.equals(existingIdentity.getUserId(), user.getId())) {
                throw new IllegalArgumentException("该 " + providerLabel(identity.provider) + " 已绑定其他 CodePower 账号");
            }
            existingIdentity.setLastLoginAt(LocalDateTime.now());
            bindingMapper.updateById(existingIdentity);
            return existingIdentity;
        }

        UserOauthBinding existingProvider = bindingMapper.selectOne(
                new LambdaQueryWrapper<UserOauthBinding>()
                        .eq(UserOauthBinding::getUserId, user.getId())
                        .eq(UserOauthBinding::getProvider, identity.provider));
        if (existingProvider != null) {
            throw new IllegalArgumentException("当前 CodePower 账号已绑定其他 "
                    + providerLabel(identity.provider) + " 账号，请先解绑后再绑定");
        }

        UserOauthBinding binding = new UserOauthBinding();
        binding.setUserId(user.getId());
        binding.setProvider(identity.provider);
        binding.setProviderUserId(identity.providerUserId);
        binding.setProviderUnionId(identity.unionId);
        binding.setProviderUsername(identity.username);
        binding.setProviderNickname(identity.nickname);
        binding.setProviderAvatar(identity.avatar);
        binding.setProviderEmail(identity.email);
        binding.setEmailVerified(identity.emailVerified ? 1 : 0);
        binding.setBoundAt(LocalDateTime.now());
        binding.setLastLoginAt(LocalDateTime.now());
        try {
            bindingMapper.insert(binding);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("该授权账号或当前平台坑位已被占用，请刷新后重试");
        }
        writeLog(user.getId(), identity.provider, identity.providerUserId, action, "绑定成功");
        return binding;
    }

    private UserOauthBinding findBinding(String provider, String providerUserId) {
        return bindingMapper.selectOne(
                new LambdaQueryWrapper<UserOauthBinding>()
                        .eq(UserOauthBinding::getProvider, provider)
                        .eq(UserOauthBinding::getProviderUserId, providerUserId));
    }

    private String createLinkSession(OAuthIdentity identity, String redirect) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("provider", identity.provider);
        session.put("providerUserId", identity.providerUserId);
        session.put("providerUnionId", identity.unionId);
        session.put("providerUsername", identity.username);
        session.put("providerNickname", identity.nickname);
        session.put("providerAvatar", identity.avatar);
        session.put("providerEmail", identity.email);
        session.put("emailVerified", identity.emailVerified);
        session.put("redirect", redirect);
        redisCacheService.set(LINK_KEY_PREFIX + sessionId, session, LINK_SESSION_TTL);
        return sessionId;
    }

    private String createLoginSession(User user, String redirect) {
        String loginId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> login = buildLoginPayload(user, redirect);
        redisCacheService.set(LOGIN_KEY_PREFIX + loginId, login, LOGIN_SESSION_TTL);
        return loginId;
    }

    private Map<String, Object> buildLoginPayload(User user, String redirect) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole(), 7 * 24 * 3600 * 1000L);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "登录成功");
        payload.put("token", token);
        payload.put("user", buildUserInfo(user));
        payload.put("redirect", normalizeRedirect(redirect));
        return payload;
    }

    private Map<String, Object> buildUserInfo(User user) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, user.getId()));
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole());
        userInfo.put("avatar", profile != null && profile.getAvatarUrl() != null
                ? profile.getAvatarUrl() : "/avatars/avatar-1.svg");
        return userInfo;
    }

    private void validateCreateAccountRequest(OAuthCreateAccountRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请填写注册信息");
        }
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();
        if (username.isEmpty() || email.isEmpty() || password == null || password.isBlank()
                || request.getVerificationCode() == null || request.getVerificationCode().isBlank()) {
            throw new IllegalArgumentException("请填写用户名、邮箱、验证码和密码");
        }
        if (!username.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$")) {
            throw new IllegalArgumentException("用户名只能包含字母、数字、下划线和中文");
        }
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("请输入有效的邮箱地址");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("密码长度至少为6位");
        }
        if (confirmPassword != null && !confirmPassword.isBlank() && !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
    }

    private User findUserByAccount(String account) {
        if (account.contains("@")) {
            return userMapper.findByEmail(account.toLowerCase(Locale.ROOT));
        }
        return userMapper.findByUsername(account);
    }

    private boolean isUsableUser(User user) {
        return user != null
                && (user.getStatus() == null || user.getStatus() == 1)
                && (user.getDeleted() == null || user.getDeleted() == 0);
    }

    private User requireUserFromAuthorization(String authorization) {
        User user = resolveUserFromAuthorization(authorization);
        if (user == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return user;
    }

    private User resolveUserFromAuthorization(String authorization) {
        try {
            if (authorization == null || authorization.isBlank()) {
                return null;
            }
            String token = authorization.replace("Bearer ", "").trim();
            Long userId = jwtUtil.extractUserId(token);
            if (userId != null) {
                return userMapper.selectById(userId);
            }
            String username = jwtUtil.extractUsername(token);
            return username == null ? null : userMapper.findByUsername(username);
        } catch (Exception e) {
            return null;
        }
    }

    private OAuthIdentity identityFromSession(Map<String, Object> session) {
        OAuthIdentity identity = new OAuthIdentity();
        identity.provider = String.valueOf(session.get("provider"));
        identity.providerUserId = stringValue(session.get("providerUserId"));
        identity.unionId = stringValue(session.get("providerUnionId"));
        identity.username = stringValue(session.get("providerUsername"));
        identity.nickname = stringValue(session.get("providerNickname"));
        identity.avatar = stringValue(session.get("providerAvatar"));
        identity.email = stringValue(session.get("providerEmail"));
        identity.emailVerified = Boolean.TRUE.equals(session.get("emailVerified"));
        return identity;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String key) {
        Object value = redisCacheService.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private OAuthProperties.Provider getProviderConfig(String provider) {
        return switch (provider) {
            case "github" -> oauthProperties.getGithub();
            case "gitee" -> oauthProperties.getGitee();
            case "qq" -> oauthProperties.getQq();
            case "wechat" -> oauthProperties.getWechat();
            default -> throw new IllegalArgumentException("不支持的授权平台");
        };
    }

    private String normalizeProvider(String provider) {
        String value = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!List.of("github", "gitee", "qq", "wechat").contains(value)) {
            throw new IllegalArgumentException("不支持的授权平台");
        }
        return value;
    }

    private String normalizeMode(String mode) {
        return "bind".equalsIgnoreCase(mode) ? "bind" : "login";
    }

    private boolean isActiveOAuthProvider(String provider) {
        return ACTIVE_OAUTH_PROVIDERS.contains(provider);
    }

    private String unsupportedProviderMessage(String provider) {
        return providerLabel(provider) + " 授权登录需要企业资质，当前仅保留入口，暂不支持个人开发者接入";
    }

    private String normalizeRedirect(String redirect) {
        if (redirect != null
                && redirect.startsWith("/")
                && !redirect.startsWith("//")
                && !redirect.startsWith("/auth")) {
            return redirect;
        }
        return "/";
    }

    private String providerLabel(String provider) {
        return switch (provider) {
            case "github" -> "GitHub";
            case "gitee" -> "Gitee";
            case "qq" -> "QQ";
            case "wechat" -> "微信";
            default -> provider;
        };
    }

    private String frontendCallbackUrl(Map<String, String> params) {
        return buildUrl(oauthProperties.getFrontendCallbackUrl(), params);
    }

    private String buildUrl(String base, Map<String, String> params) {
        StringBuilder builder = new StringBuilder(base);
        builder.append(base.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            first = false;
            builder.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private Map<String, String> parseQueryString(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null) {
            return map;
        }
        for (String part : text.split("&")) {
            int idx = part.indexOf('=');
            if (idx > 0) {
                map.put(URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8),
                        URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonp(String text) {
        try {
            if (text == null) {
                return Map.of();
            }
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return Map.of();
            }
            return objectMapper.readValue(text.substring(start, end + 1), Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void writeLog(Long userId, String provider, String providerUserId, String action, String message) {
        UserOauthBindingLog logItem = new UserOauthBindingLog();
        logItem.setUserId(userId);
        logItem.setProvider(provider);
        logItem.setProviderUserId(providerUserId);
        logItem.setAction(action);
        logItem.setMessage(message);
        bindingLogMapper.insert(logItem);
    }

    private static class OAuthIdentity {
        private String provider;
        private String providerUserId;
        private String unionId;
        private String username;
        private String nickname;
        private String avatar;
        private String email;
        private boolean emailVerified;
    }
}

/**
 * 文件说明：第三方授权登录 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.ls.domain.dto.OAuthBindExistingRequest;
import com.ls.domain.dto.OAuthCreateAccountRequest;
import com.ls.service.OAuthAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/** 第三方授权登录、绑定和解绑控制器 */
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {
    private final OAuthAccountService oauthAccountService;

    /** 查询前端可展示的授权平台状态，包括 GitHub/Gitee 可用和 QQ/微信暂不可用提示。 */
    @GetMapping("/providers")
    public ResponseEntity<?> listProviders() {
        return ResponseEntity.ok(oauthAccountService.listProviders());
    }

    /** 生成第三方平台授权地址，登录模式和绑定模式共用这一入口。 */
    @PostMapping("/{provider}/authorize-url")
    public ResponseEntity<?> buildAuthorizeUrl(@PathVariable String provider,
                                               @RequestBody(required = false) Map<String, String> body,
                                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String mode = body == null ? "login" : body.getOrDefault("mode", "login");
            String redirect = body == null ? "/" : body.getOrDefault("redirect", "/");
            return ResponseEntity.ok(oauthAccountService.buildAuthorizeUrl(provider, mode, redirect, authorization));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 第三方平台回调入口：把 code/state 交给服务层换取平台用户信息，再重定向回前端。 */
    @GetMapping("/{provider}/callback")
    public RedirectView callback(@PathVariable String provider,
                                 @RequestParam(required = false) String code,
                                 @RequestParam(required = false) String state,
                                 @RequestParam(required = false) String error,
                                 @RequestParam(name = "error_description", required = false) String errorDescription) {
        return new RedirectView(oauthAccountService.handleCallback(provider, code, state, error, errorDescription));
    }

    /** 消费一次性登录会话，前端回调页用它换取 CodePower 登录 Token。 */
    @GetMapping("/login-sessions/{loginId}")
    public ResponseEntity<?> consumeLoginSession(@PathVariable String loginId) {
        try {
            return ResponseEntity.ok(oauthAccountService.consumeLoginSession(loginId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    /** 查询待绑定会话，用于展示“已有账号绑定”或“新账号注册绑定”页面。 */
    @GetMapping("/link-sessions/{sessionId}")
    public ResponseEntity<?> getLinkSession(@PathVariable String sessionId) {
        try {
            return ResponseEntity.ok(oauthAccountService.getLinkSession(sessionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    /** 绑定已有账号：用户输入邮箱/用户名和密码，校验通过后把第三方身份绑定到该账号。 */
    @PostMapping("/link-sessions/{sessionId}/bind-existing")
    public ResponseEntity<?> bindExisting(@PathVariable String sessionId,
                                          @RequestBody OAuthBindExistingRequest request) {
        try {
            return ResponseEntity.ok(oauthAccountService.bindExistingAccount(sessionId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 注册新账号并绑定第三方授权，注册字段沿用普通注册逻辑。 */
    @PostMapping("/link-sessions/{sessionId}/create-account")
    public ResponseEntity<?> createAccount(@PathVariable String sessionId,
                                           @RequestBody OAuthCreateAccountRequest request) {
        try {
            return ResponseEntity.ok(oauthAccountService.createAccountAndBind(sessionId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 查询当前登录用户已经绑定的第三方账号。 */
    @GetMapping("/me/bindings")
    public ResponseEntity<?> listMyBindings(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            return ResponseEntity.ok(oauthAccountService.listMyBindings(authorization));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    /** 解绑当前用户指定平台的第三方账号。 */
    @DeleteMapping("/me/bindings/{provider}")
    public ResponseEntity<?> unlink(@PathVariable String provider,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            oauthAccountService.unlinkMyBinding(provider, authorization);
            return ResponseEntity.ok(Map.of("message", "解绑成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

/**
 * 文件说明：认证授权 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.ls.domain.dto.LoginRequest;
import com.ls.domain.dto.RegisterRequest;
import com.ls.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器 - 处理用户认证相关的请求
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 发送邮箱验证码
     * @param payload 包含email字段的请求体
     * @return 发送结果
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        return authService.sendRegistrationCode(email);
    }

    /**
     * 用户注册
     * @param request 注册请求（包含用户名、密码、邮箱、验证码）
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 用户登录
     * @param request 登录请求（包含用户名/邮箱、密码）
     * @return 登录结果（包含JWT令牌）
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
    
    /**
     * 请求发送密码重置验证码
     * @param payload 包含email和verificationToken字段的请求体
     * @return 发送结果
     */
    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String verificationToken = payload.get("verificationToken");
        return authService.requestPasswordReset(email, verificationToken);
    }
    
    /**
     * 重置密码
     * @param resetInfo 包含email、code和newPassword字段的请求体
     * @return 重置结果
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> resetInfo) {
        return authService.resetPassword(resetInfo);
    }
    
    /**
     * 检查用户名是否已存在
     * @param username 要检查的用户名
     * @return 存在状态
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsernameExists(@RequestParam String username) {
        return authService.checkUsernameExists(username);
    }
    
    /**
     * 检查邮箱是否已被注册
     * @param email 要检查的邮箱
     * @return 存在状态
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailExists(@RequestParam String email) {
        return authService.checkEmailExists(email);
    }
} 
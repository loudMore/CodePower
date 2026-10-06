/**
 * 文件说明：认证授权 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.ls.domain.dto.LoginRequest;
import com.ls.domain.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/** 用户认证服务（注册、登录、密码重置） */
public interface AuthService {

    /**
     * 请求发送注册验证码
     * @param email 目标邮箱
     * @return 操作结果
     */
    ResponseEntity<?> sendRegistrationCode(String email);

    /**
     * 用户注册
     * @param request 注册信息
     * @return 操作结果
     */
    ResponseEntity<?> register(RegisterRequest request);

    /**
     * 用户登录
     * @param request 登录信息
     * @return 操作结果，成功时包含token
     */
    ResponseEntity<?> login(LoginRequest request);
    
    /**
     * 请求发送密码重置验证码
     * @param email 用户邮箱
     * @param verificationToken 人机验证token
     * @return 操作结果
     */
    ResponseEntity<?> requestPasswordReset(String email, String verificationToken);
    
    /**
     * 重置密码
     * @param resetInfo 包含email、验证码和新密码的信息
     * @return 操作结果
     */
    ResponseEntity<?> resetPassword(Map<String, String> resetInfo);
    
    /**
     * 检查用户名是否已存在
     * @param username 要检查的用户名
     * @return 操作结果，包含exists字段表示是否存在
     */
    ResponseEntity<?> checkUsernameExists(String username);
    
    /**
     * 检查邮箱是否已被注册
     * @param email 要检查的邮箱
     * @return 操作结果，包含exists字段表示是否存在
     */
    ResponseEntity<?> checkEmailExists(String email);

    /** 校验邮箱验证码是否正确 */
    boolean verifyEmailCode(String email, String code);

    /** 向指定邮箱发送验证码 */
    void sendCodeToEmail(String email);
} 
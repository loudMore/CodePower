/**
 * 文件说明：登录前人机验证 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.ls.domain.dto.VerificationAttempt;
import com.ls.domain.dto.VerificationChallenge;
import com.ls.domain.dto.VerificationResult;
import com.ls.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 验证控制器 - 处理交互式验证请求
 * @author ls
 * @since 2025-07-01
 */
@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;

    /**
     * 获取验证挑战
     * @return 返回一个验证挑战数据，包含验证题目和选项
     */
    @GetMapping("/challenge")
    public ResponseEntity<VerificationChallenge> getChallenge() {
        VerificationChallenge challenge = verificationService.createChallenge();
        return ResponseEntity.ok(challenge);
    }

    /**
     * 验证用户的答案
     * @param attempt 验证尝试请求，包含挑战ID和用户选择的答案
     * @return 验证结果，包含是否成功和验证令牌
     */
    @PostMapping("/verify")
    public ResponseEntity<VerificationResult> verify(@RequestBody VerificationAttempt attempt) {
        VerificationResult result = verificationService.verify(attempt.getChallengeId(), attempt.getSelection());
        return ResponseEntity.ok(result);
    }
} 
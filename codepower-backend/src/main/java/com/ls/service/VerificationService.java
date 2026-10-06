/**
 * 文件说明：登录前人机验证 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.ls.domain.dto.VerificationChallenge;
import com.ls.domain.dto.VerificationResult;

import java.util.List;

/** 人机验证服务 */
public interface VerificationService {

    /**
     * 创建一个新的验证挑战
     * @return 验证挑战对象
     */
    VerificationChallenge createChallenge();

    /**
     * 验证用户的尝试
     * @param challengeId 挑战ID
     * @param selection 用户选择的顺序
     * @return 验证结果
     */
    VerificationResult verify(String challengeId, List<String> selection);

    /**
     * 校验验证票据是否有效
     * @param token 票据
     * @return 如果有效则为 true，否则为 false
     */
    boolean validateVerificationToken(String token);
} 
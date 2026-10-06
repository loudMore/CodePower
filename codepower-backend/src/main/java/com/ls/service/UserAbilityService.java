/**
 * 文件说明：用户标签能力 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.ls.domain.UserAbility;

import java.util.List;
import java.util.Map;

/** 用户能力模型服务（雷达图数据） */
public interface UserAbilityService {
    /** 获取用户各维度能力值列表 */
    List<UserAbility> getUserAbilities(Long userId);
    /** 获取用户能力雷达图数据 */
    Map<String, Object> getRadarChartData(Long userId);
    /** 获取用户能力画像概览 */
    Map<String, Object> getAbilityProfile(Long userId);
    /** 根据能力短板获取个性化推荐题目 */
    List<Map<String, Object>> getPersonalizedRecommendations(Long userId, int size);
    /** 用户通过题目后更新对应能力值 */
    void updateAbilityOnAccepted(Long userId, Long problemId);
    /** 刷新所有用户的能力画像 */
    void refreshAllAbilityProfiles();
}

/**
 * 文件说明：等级积分 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import java.util.List;
import java.util.Map;

/** 用户等级与经验服务 */
public interface UserLevelService {
    /** 为用户增加经验值 */
    void addExp(Long userId, int amount);

    /** 检查并重置用户每日AI积分配额 */
    void checkAndResetDailyQuota(Long userId);

    /** 消耗1点AI积分 */
    void consumeAiQuota(Long userId);

    /** 按倍率消耗AI积分 */
    void consumeAiQuota(Long userId, double cost);

    /** 只检查AI积分是否足够，不实际扣除 */
    void assertAiQuotaAvailable(Long userId, double cost);

    /** 退还1点AI积分 */
    void refundAiQuota(Long userId);

    /** 按倍率退还AI积分 */
    void refundAiQuota(Long userId, double cost);

    /** 增加用户奖励AI积分 */
    void addAiBonus(Long userId, int bonus);

    /** 获取用户等级与积分详情 */
    Map<String, Object> getUserLevelInfo(Long userId);

    /** 获取全部等级配置列表 */
    List<Map<String, Object>> getLevelConfig();
}

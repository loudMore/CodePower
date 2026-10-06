/**
 * 文件说明：等级积分 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.domain.LevelConfig;
import com.ls.domain.User;
import com.ls.mapper.LevelConfigMapper;
import com.ls.mapper.UserMapper;
import com.ls.service.NotificationService;
import com.ls.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 等级与经验服务实现 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLevelServiceImpl implements UserLevelService {

    private final UserMapper userMapper;
    private final LevelConfigMapper levelConfigMapper;
    private final NotificationService notificationService;

    // L1 本地缓存：等级配置表在运行期不会变化，缓存30分钟减少DB查询
    private final Cache<String, List<LevelConfig>> levelConfigCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(2)
            .build();

    private static final String LEVEL_CONFIGS_ASC = "ASC";
    private static final String LEVEL_CONFIGS_DESC = "DESC";

    /** 获取按指定方向排序的等级配置（优先读本地缓存） */
    private List<LevelConfig> getCachedLevelConfigs(boolean ascending) {
        String key = ascending ? LEVEL_CONFIGS_ASC : LEVEL_CONFIGS_DESC;
        List<LevelConfig> cached = levelConfigCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        LambdaQueryWrapper<LevelConfig> wrapper = new LambdaQueryWrapper<>();
        if (ascending) {
            wrapper.orderByAsc(LevelConfig::getLevel);
        } else {
            wrapper.orderByDesc(LevelConfig::getLevel);
        }
        List<LevelConfig> configs = levelConfigMapper.selectList(wrapper);
        levelConfigCache.put(key, configs);
        log.debug("等级配置缓存已刷新（{}序），共 {} 条", ascending ? "升" : "降", configs.size());
        return configs;
    }

    @Override
    @Transactional
    public void addExp(Long userId, int amount) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int newExp = user.getExp() + amount;
        user.setExp(newExp);

        // 使用本地缓存读取等级配置（降序，从高到低匹配）
        List<LevelConfig> configs = getCachedLevelConfigs(false);

        for (LevelConfig config : configs) {
            if (newExp >= config.getRequiredExp()) {
                if (config.getLevel() > user.getLevel()) {
                    int oldLevel = user.getLevel();
                    user.setLevel(config.getLevel());
                    log.info("用户 {} 升级到 Lv{} ({})", userId, config.getLevel(), config.getTitle());
                    notificationService.createNotification(userId, "LEVEL_UP",
                            "恭喜升级到 Lv." + config.getLevel() + "！",
                            "恭喜你从 Lv." + oldLevel + " 升级到 Lv." + config.getLevel() + "「" + config.getTitle() + "」！每日 AI 积分已提升至 " + config.getBaseAiQuota() + " 点，继续加油！",
                            null);
                }
                break;
            }
        }
        userMapper.updateById(user);
    }

    private int getRoleBaseQuota(String role) {
        if ("ADMIN".equals(role)) return 999;
        if ("SENIOR_USER".equals(role)) return 50;
        return 0;
    }

    @Override
    @Transactional
    public void checkAndResetDailyQuota(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        LocalDate today = LocalDate.now();
        if (user.getQuotaResetDate() == null || !user.getQuotaResetDate().equals(today)) {
            // 从本地缓存中查找对应等级配置，避免每次重置都查库
            LevelConfig config = getCachedLevelConfigs(true).stream()
                    .filter(c -> c.getLevel().equals(user.getLevel()))
                    .findFirst().orElse(null);
            int levelQuota = config != null ? config.getBaseAiQuota() : 5;
            int roleQuota = getRoleBaseQuota(user.getRole());
            user.setDailyAiQuota(levelQuota + roleQuota);
            user.setDailyAiUsed(0);
            user.setQuotaResetDate(today);
            userMapper.updateById(user);
        }
    }

    @Override
    @Transactional
    public void consumeAiQuota(Long userId) {
        consumeAiQuota(userId, 1.0);
    }

    @Override
    @Transactional
    public void consumeAiQuota(Long userId, double cost) {
        checkAndResetDailyQuota(userId);
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int costInt = (int) Math.max(1, Math.ceil(cost));
        int dailyRemaining = user.getDailyAiQuota() - user.getDailyAiUsed();
        int bonus = user.getBonusAiPoints() != null ? user.getBonusAiPoints() : 0;
        int totalAvailable = dailyRemaining + bonus;

        ensureQuotaEnough(costInt, dailyRemaining, bonus, totalAvailable);

        int remaining = costInt;
        if (dailyRemaining >= remaining) {
            user.setDailyAiUsed(user.getDailyAiUsed() + remaining);
        } else {
            user.setDailyAiUsed(user.getDailyAiQuota());
            remaining -= dailyRemaining;
            user.setBonusAiPoints(bonus - remaining);
        }
        userMapper.updateById(user);
    }

    @Override
    public void assertAiQuotaAvailable(Long userId, double cost) {
        checkAndResetDailyQuota(userId);
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int costInt = (int) Math.max(1, Math.ceil(cost));
        int dailyRemaining = user.getDailyAiQuota() - user.getDailyAiUsed();
        int bonus = user.getBonusAiPoints() != null ? user.getBonusAiPoints() : 0;
        int totalAvailable = dailyRemaining + bonus;
        ensureQuotaEnough(costInt, dailyRemaining, bonus, totalAvailable);
    }

    private void ensureQuotaEnough(int costInt, int dailyRemaining, int bonus, int totalAvailable) {
        if (totalAvailable < costInt) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "AI积分不足（每日剩余" + dailyRemaining + " + 累积" + bonus
                            + "，该模型需要" + costInt + "积分），可通过签到或完成每日任务获取更多积分");
        }
    }

    @Override
    @Transactional
    public void refundAiQuota(Long userId) {
        refundAiQuota(userId, 1.0);
    }

    @Override
    @Transactional
    public void refundAiQuota(Long userId, double cost) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        int costInt = (int) Math.max(1, Math.ceil(cost));
        if (user.getDailyAiUsed() >= costInt) {
            user.setDailyAiUsed(user.getDailyAiUsed() - costInt);
        } else {
            int refundToDaily = user.getDailyAiUsed();
            user.setDailyAiUsed(0);
            int refundToBonus = costInt - refundToDaily;
            int bonus = user.getBonusAiPoints() != null ? user.getBonusAiPoints() : 0;
            user.setBonusAiPoints(bonus + refundToBonus);
        }
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void addAiBonus(Long userId, int bonus) {
        if (bonus <= 0) return;
        checkAndResetDailyQuota(userId);
        User user = userMapper.selectById(userId);
        if (user == null) return;
        int current = user.getBonusAiPoints() != null ? user.getBonusAiPoints() : 0;
        user.setBonusAiPoints(current + bonus);
        userMapper.updateById(user);
    }

    @Override
    public Map<String, Object> getUserLevelInfo(Long userId) {
        checkAndResetDailyQuota(userId);
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");

        // 使用本地缓存读取等级配置（升序）
        List<LevelConfig> configs = getCachedLevelConfigs(true);

        // 根据当前经验重新计算等级，避免历史迁移数据里的等级值过期。
        int correctLevel = 1;
        for (int i = configs.size() - 1; i >= 0; i--) {
            if (user.getExp() >= configs.get(i).getRequiredExp()) {
                correctLevel = configs.get(i).getLevel();
                break;
            }
        }
        if (correctLevel != user.getLevel()) {
            user.setLevel(correctLevel);
            userMapper.updateById(user);
        }

        LevelConfig current = configs.stream()
                .filter(c -> c.getLevel().equals(user.getLevel()))
                .findFirst().orElse(configs.get(0));

        LevelConfig next = configs.stream()
                .filter(c -> c.getLevel() == user.getLevel() + 1)
                .findFirst().orElse(null);

        int dailyRemaining = Math.max(0, user.getDailyAiQuota() - user.getDailyAiUsed());
        int bonus = user.getBonusAiPoints() != null ? user.getBonusAiPoints() : 0;

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("level", user.getLevel());
        info.put("title", current.getTitle());
        info.put("exp", user.getExp());
        info.put("currentLevelExp", current.getRequiredExp());
        info.put("nextLevelExp", next != null ? next.getRequiredExp() : null);
        info.put("maxLevel", next == null);
        info.put("dailyAiQuota", user.getDailyAiQuota());
        info.put("dailyAiUsed", user.getDailyAiUsed());
        info.put("dailyAiRemaining", dailyRemaining);
        info.put("bonusAiPoints", bonus);
        info.put("totalAiRemaining", dailyRemaining + bonus);
        info.put("baseAiQuota", current.getBaseAiQuota());
        return info;
    }

    @Override
    public List<Map<String, Object>> getLevelConfig() {
        // 使用本地缓存读取等级配置（升序），避免每次请求都查库
        return getCachedLevelConfigs(true)
                .stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("level", c.getLevel());
                    m.put("title", c.getTitle());
                    m.put("requiredExp", c.getRequiredExp());
                    m.put("baseAiQuota", c.getBaseAiQuota());
                    return m;
                })
                .collect(Collectors.toList());
    }
}

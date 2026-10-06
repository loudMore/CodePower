/**
 * 文件说明：签到 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.domain.UserCheckIn;
import com.ls.mapper.UserCheckInMapper;
import com.ls.service.CheckInService;
import com.ls.service.DailyTaskService;
import com.ls.service.NotificationService;
import com.ls.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** 签到服务实现 */
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final UserCheckInMapper checkInMapper;
    private final UserLevelService userLevelService;
    private final DailyTaskService dailyTaskService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Map<String, Object> checkIn(Long userId) {
        LocalDate today = LocalDate.now();

        UserCheckIn existing = checkInMapper.selectOne(
                new LambdaQueryWrapper<UserCheckIn>()
                        .eq(UserCheckIn::getUserId, userId)
                        .eq(UserCheckIn::getCheckInDate, today));
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "今日已签到");
        }

        UserCheckIn yesterday = checkInMapper.selectOne(
                new LambdaQueryWrapper<UserCheckIn>()
                        .eq(UserCheckIn::getUserId, userId)
                        .eq(UserCheckIn::getCheckInDate, today.minusDays(1)));
        int streak = (yesterday != null) ? yesterday.getStreak() + 1 : 1;

        int baseExp = 10;
        int baseAi = 3;
        int bonusExp = 0;
        int bonusAi = 0;

        if (streak % 7 == 0) {
            bonusExp = 50;
            bonusAi = 5;
        } else if (streak % 3 == 0) {
            bonusExp = 15;
            bonusAi = 2;
        }

        int totalExp = baseExp + bonusExp;
        int totalAi = baseAi + bonusAi;

        UserCheckIn checkIn = new UserCheckIn();
        checkIn.setUserId(userId);
        checkIn.setCheckInDate(today);
        checkIn.setStreak(streak);
        checkIn.setExpEarned(totalExp);
        checkIn.setAiBonus(totalAi);
        checkInMapper.insert(checkIn);

        userLevelService.addExp(userId, totalExp);
        userLevelService.addAiBonus(userId, totalAi);

        dailyTaskService.incrementTaskProgress(userId, "check_in");

        if (streak == 7 || streak == 30 || streak == 100 || streak == 365) {
            notificationService.createNotification(userId, "CHECK_IN_MILESTONE",
                    "签到里程碑：连续 " + streak + " 天！",
                    "太厉害了！你已连续签到 " + streak + " 天，坚持就是胜利！",
                    null);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("streak", streak);
        result.put("expEarned", totalExp);
        result.put("aiBonus", totalAi);
        result.put("hasStreakBonus", bonusExp > 0);
        result.put("streakBonusMsg", bonusExp > 0 ?
                "连续签到" + streak + "天奖励！额外+" + bonusExp + "经验 +" + bonusAi + "AI积分" : null);
        return result;
    }

    @Override
    public Map<String, Object> getCheckInStatus(Long userId) {
        LocalDate today = LocalDate.now();

        UserCheckIn todayCheckIn = checkInMapper.selectOne(
                new LambdaQueryWrapper<UserCheckIn>()
                        .eq(UserCheckIn::getUserId, userId)
                        .eq(UserCheckIn::getCheckInDate, today));

        int streak = 0;
        if (todayCheckIn != null) {
            streak = todayCheckIn.getStreak();
        } else {
            UserCheckIn yesterday = checkInMapper.selectOne(
                    new LambdaQueryWrapper<UserCheckIn>()
                            .eq(UserCheckIn::getUserId, userId)
                            .eq(UserCheckIn::getCheckInDate, today.minusDays(1)));
            if (yesterday != null) {
                streak = yesterday.getStreak();
            }
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("checkedInToday", todayCheckIn != null);
        status.put("streak", streak);
        status.put("nextStreakBonus", getNextStreakBonusDay(streak));
        return status;
    }

    @Override
    public List<Integer> getMonthlyCheckIns(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);

        return checkInMapper.selectList(
                new LambdaQueryWrapper<UserCheckIn>()
                        .eq(UserCheckIn::getUserId, userId)
                        .ge(UserCheckIn::getCheckInDate, start)
                        .lt(UserCheckIn::getCheckInDate, end))
                .stream()
                .map(c -> c.getCheckInDate().getDayOfMonth())
                .collect(Collectors.toList());
    }

    private int getNextStreakBonusDay(int currentStreak) {
        int next3 = ((currentStreak / 3) + 1) * 3;
        int next7 = ((currentStreak / 7) + 1) * 7;
        return Math.min(next3, next7);
    }
}

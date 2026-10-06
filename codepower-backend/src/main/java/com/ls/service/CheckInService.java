/**
 * 文件说明：签到 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import java.util.List;
import java.util.Map;

/** 签到服务 */
public interface CheckInService {
    /** 执行每日签到 */
    Map<String, Object> checkIn(Long userId);

    /** 获取用户当日签到状态与连续签到天数 */
    Map<String, Object> getCheckInStatus(Long userId);

    /** 获取用户指定月份的签到日期列表 */
    List<Integer> getMonthlyCheckIns(Long userId, int year, int month);
}

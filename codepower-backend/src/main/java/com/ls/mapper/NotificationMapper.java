/**
 * 文件说明：通知 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.Notification;
import org.apache.ibatis.annotations.Mapper;

/** 系统通知 Mapper */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}

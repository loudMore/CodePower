/**
 * 文件说明：AI 消息 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.AiMessage;
import org.apache.ibatis.annotations.Mapper;

/** AI 消息 Mapper */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}

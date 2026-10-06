/**
 * 文件说明：AI 会话 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.AiConversation;
import org.apache.ibatis.annotations.Mapper;

/** AI 对话 Mapper */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}

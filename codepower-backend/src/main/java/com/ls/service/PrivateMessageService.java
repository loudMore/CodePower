/**
 * 文件说明：私信 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ls.domain.PrivateMessage;

import java.util.List;
import java.util.Map;

/** 私信服务 */
public interface PrivateMessageService extends IService<PrivateMessage> {

    /** 发送私信 */
    PrivateMessage sendMessage(Long fromUserId, Long toUserId, String content);

    /** 分页查询与某用户的私信对话记录 */
    IPage<PrivateMessage> getConversation(Long userId, Long otherUserId, int page, int size);

    /** 获取当前用户的会话列表（含最新消息摘要） */
    List<Map<String, Object>> getConversationList(Long userId);

    /** 将与某用户的私信标记为已读 */
    void markAsRead(Long userId, Long otherUserId);

    /** 获取用户未读私信总数 */
    long getUnreadCount(Long userId);
}

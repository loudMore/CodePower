/**
 * 文件说明：私信 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ls.domain.PrivateMessage;
import com.ls.domain.User;
import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.mapper.PrivateMessageMapper;
import com.ls.mapper.UserMapper;
import com.ls.service.PrivateMessageService;
import com.ls.service.UserFollowService;
import com.ls.websocket.MessageWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 私信服务实现 */
@Service
@RequiredArgsConstructor
public class PrivateMessageServiceImpl extends ServiceImpl<PrivateMessageMapper, PrivateMessage> implements PrivateMessageService {

    private final UserFollowService followService;
    private final UserMapper userMapper;
    private final MessageWebSocketHandler messageWebSocketHandler;

    private static final int NON_FOLLOW_MSG_LIMIT = 3;

    @Override
    @Transactional
    public PrivateMessage sendMessage(Long fromUserId, Long toUserId, String content) {
        if (toUserId == null || fromUserId.equals(toUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能给自己发送消息");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能为空");
        }

        User fromUser = userMapper.selectById(fromUserId);
        User toUser = userMapper.selectById(toUserId);
        if (fromUser == null || toUser == null || Integer.valueOf(1).equals(toUser.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "接收用户不存在");
        }

        boolean canUnlimited = "ADMIN".equals(fromUser.getRole()) || followService.isMutualFollow(fromUserId, toUserId);
        if (!canUnlimited) {
            canUnlimited = count(new LambdaQueryWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getFromUserId, toUserId)
                    .eq(PrivateMessage::getToUserId, fromUserId)) > 0;
        }
        if (!canUnlimited) {
            long sentCount = count(new LambdaQueryWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getFromUserId, fromUserId)
                    .eq(PrivateMessage::getToUserId, toUserId));
            if (sentCount >= NON_FOLLOW_MSG_LIMIT) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "对方未回复你，最多发送" + NON_FOLLOW_MSG_LIMIT + "条消息。对方回复或互相关注后可无限聊天。");
            }
        }

        PrivateMessage message = new PrivateMessage();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content.trim());
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        save(message);
        Runnable pushTask = () -> messageWebSocketHandler.pushPrivateMessage(message);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pushTask.run();
                }
            });
        } else {
            pushTask.run();
        }
        return message;
    }

    @Override
    public IPage<PrivateMessage> getConversation(Long userId, Long otherUserId, int page, int size) {
        Page<PrivateMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(PrivateMessage::getFromUserId, userId).eq(PrivateMessage::getToUserId, otherUserId))
                .or(w2 -> w2.eq(PrivateMessage::getFromUserId, otherUserId).eq(PrivateMessage::getToUserId, userId))
        ).orderByDesc(PrivateMessage::getCreatedAt);
        return page(pageParam, wrapper);
    }

    @Override
    public List<Map<String, Object>> getConversationList(Long userId) {
        return baseMapper.selectConversationList(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long otherUserId) {
        update(new PrivateMessage() {{ setIsRead(true); }},
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getToUserId, userId)
                        .eq(PrivateMessage::getFromUserId, otherUserId)
                        .eq(PrivateMessage::getIsRead, false));
    }

    @Override
    public long getUnreadCount(Long userId) {
        return count(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getToUserId, userId)
                .eq(PrivateMessage::getIsRead, false));
    }
}

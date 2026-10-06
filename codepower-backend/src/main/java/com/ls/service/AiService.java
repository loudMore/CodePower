/**
 * 文件说明：AI 智能辅助 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.domain.AiConversation;
import com.ls.domain.AiMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** AI 智能辅助服务 */
public interface AiService {
    /** 对明显越界、要完整答案或提示词注入的问题返回本地导师回复，不调用外部模型。 */
    Optional<AiMessage> guardChatIfNecessary(Long userId, Long conversationId, String message, Consumer<String> chunkConsumer);
    /** 发送聊天消息并获取AI回复 */
    AiMessage chat(Long userId, Long conversationId, String message, String model, String userCode, String language,
                   String testContext);
    /** 流式发送聊天消息，chunkConsumer 会实时收到模型输出片段 */
    AiMessage streamChat(Long userId, Long conversationId, String message, String model, String userCode,
                         String language, String testContext, Consumer<String> chunkConsumer);
    /** 创建新的AI对话 */
    AiConversation createConversation(Long userId, String title, Long problemId, String type);
    /** 分页查询用户的AI对话列表 */
    IPage<AiConversation> getConversations(Long userId, int page, int size);
    /** 获取指定对话的全部消息记录 */
    List<AiMessage> getMessages(Long userId, Long conversationId);
    /** 删除指定AI对话 */
    void deleteConversation(Long userId, Long conversationId);
    /** AI分析指定题目并返回解题思路 */
    Map<String, Object> analyzeProblem(Long problemId);
    /** AI根据出题方向、难度和语言自动生成题目，可指定模型 */
    Map<String, Object> generateProblem(String tags, String difficulty, String language, String model,
                                        String userPrompt, Integer exampleCount, Integer testCaseCount,
                                        Integer totalScore);
    /** AI生成多道题目候选，可指定模型 */
    List<Map<String, Object>> generateProblems(String tags, String difficulty, String language, String model,
                                               String userPrompt, Integer exampleCount, Integer testCaseCount,
                                               Integer totalScore, Integer count);
    /** AI为指定题目生成题解 */
    Map<String, Object> generateSolution(Long problemId, String prompt);
    /** AI根据草稿题目数据生成题解 */
    Map<String, Object> generateSolutionFromDraft(Map<String, String> problemData, String prompt);
    /** 获取可用的AI模型列表 */
    List<Map<String, String>> getAvailableModels();
    /** 获取指定模型的积分消耗倍率 */
    double getModelCost(String modelId);
}

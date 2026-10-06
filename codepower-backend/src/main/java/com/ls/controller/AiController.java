/**
 * 文件说明：AI 智能辅助 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.AiConversation;
import com.ls.domain.AiMessage;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.AiService;
import com.ls.service.DailyTaskService;
import com.ls.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** AI 智能辅助控制器 — 对话、题目分析、AI出题、AI题解 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final UserMapper userMapper;
    private final UserLevelService userLevelService;
    private final DailyTaskService dailyTaskService;

    private final Cache<String, AiProblemGenerationJob> problemGenerationJobs = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    /** 创建新的AI对话 */
    @PostMapping("/conversations")
    public Result<AiConversation> createConversation(@AuthenticationPrincipal UserDetails userDetails,
                                                      @RequestBody Map<String, Object> body) {
        Long userId = getUserId(userDetails);
        String title = (String) body.get("title");
        Long problemId = body.get("problemId") != null ? Long.valueOf(body.get("problemId").toString()) : null;
        String type = (String) body.get("type");
        return Result.success(aiService.createConversation(userId, title, problemId, type));
    }

    /** 获取当前用户的对话列表（分页） */
    @GetMapping("/conversations")
    public Result<IPage<AiConversation>> getConversations(@AuthenticationPrincipal UserDetails userDetails,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(userDetails);
        return Result.success(aiService.getConversations(userId, page, size));
    }

    /** 获取指定对话的消息记录 */
    @GetMapping("/conversations/{id}/messages")
    public Result<List<AiMessage>> getMessages(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        return Result.success(aiService.getMessages(userId, id));
    }

    /** 发送消息并获取AI回复（先校验额度，成功后扣除积分） */
    @PostMapping("/chat")
    public Result<AiMessage> chat(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestBody Map<String, Object> body) {
        Long userId = getUserId(userDetails);
        String model = body.get("model") != null ? body.get("model").toString() : null;
        Long conversationId = Long.valueOf(body.get("conversationId").toString());
        String message = body.get("message") != null ? body.get("message").toString() : "";
        Optional<AiMessage> guarded = aiService.guardChatIfNecessary(userId, conversationId, message, null);
        if (guarded.isPresent()) {
            return Result.success(guarded.get());
        }

        double cost = aiService.getModelCost(model);
        userLevelService.assertAiQuotaAvailable(userId, cost);
        try {
            String userCode = body.get("userCode") != null ? body.get("userCode").toString() : null;
            String language = body.get("language") != null ? body.get("language").toString() : null;
            String testContext = body.get("testContext") != null ? body.get("testContext").toString() : null;
            AiMessage result = aiService.chat(userId, conversationId, message, model, userCode, language, testContext);
            userLevelService.consumeAiQuota(userId, cost);
            dailyTaskService.incrementTaskProgress(userId, "ai_chat");
            return Result.success(result);
        } catch (Exception e) {
            log.error("AI chat调用失败，未扣除积分(cost={})", cost, e);
            throw e;
        }
    }

    /**
     * AI 流式辅导接口 —— SSE (Server-Sent Events)。
     *
     * 技术链路：
     *   前端 fetch + ReadableStream 发起 POST → 后端 SseEmitter 持有长连接
     *   → Service 用 Java HttpClient 调 AI API (stream=true)
     *   → BufferedReader 逐行读 AI 返回的 SSE → 每读到一个 delta token
     *   → 立刻通过 emitter.send("delta", token) 推送给前端 → 前端逐字显示。
     *
     * 安全与额度：
     *   先做关键词拦截（完整代码请求）和提示词注入拦截，
     *   再校验 AI 积分余额，成功后消费积分。
     *
     * SSE 推送事件格式（前端收到的每条消息）：
     *   event: state
     *   data: {"message":"正在请求模型，首段内容返回后会立即显示"}
     *
     *   event: delta
     *   data: {"content":"这"}       ← AI 逐字返回的增量文本
     *
     *   event: done
     *   data: {"id":123,"content":"这道题考查的是动态规划..."}   ← 完整回答
     *
     *   event: error
     *   data: {"message":"AI服务调用失败"}
     */
    public ResponseEntity<SseEmitter> streamChat(@AuthenticationPrincipal UserDetails userDetails,
                                                 @RequestBody Map<String, Object> body) {
        Long userId = getUserId(userDetails);
        SseEmitter emitter = new SseEmitter(240_000L);
        AtomicBoolean streamFinished = new AtomicBoolean(false);
        emitter.onCompletion(() -> streamFinished.set(true));
        emitter.onTimeout(() -> {
            streamFinished.set(true);
            emitter.complete();
        });
        emitter.onError(e -> streamFinished.set(true));
        CompletableFuture.runAsync(() -> {
            int round = 0;
            while (!streamFinished.get()) {
                try {
                    TimeUnit.SECONDS.sleep(15);
                    if (!streamFinished.get()) {
                        round++;
                        sendSse(emitter, "state", Map.of(
                                "message", round < 3 ? "模型正在分析题面和你的代码，请稍等" : "模型响应较慢，连接仍保持中"
                        ));
                    }
                } catch (Exception e) {
                    streamFinished.set(true);
                }
            }
        });
        CompletableFuture.runAsync(() -> {
            double cost = 0;
            try {
                sendSse(emitter, "state", Map.of("message", "正在读取题面、公开样例和你的代码"));
                String model = body.get("model") != null ? body.get("model").toString() : null;
                Long conversationId = Long.valueOf(body.get("conversationId").toString());
                String message = body.get("message") != null ? body.get("message").toString() : "";
                String userCode = body.get("userCode") != null ? body.get("userCode").toString() : null;
                String language = body.get("language") != null ? body.get("language").toString() : null;
                String testContext = body.get("testContext") != null ? body.get("testContext").toString() : null;

                if (isDirectCodeRequest(message)) {
                    String reply = directCodeGuardReply();
                    log.info("AI stream direct-code guard hit: userId={}, conversationId={}", userId, conversationId);
                    sendSse(emitter, "state", Map.of("message", "已识别为完整代码请求，改为给学习提示"));
                    emitLocalText(emitter, reply);
                    sendSse(emitter, "done", Map.of("content", reply));
                    streamFinished.set(true);
                    emitter.complete();
                    return;
                }

                Optional<AiMessage> guarded = aiService.guardChatIfNecessary(
                        userId,
                        conversationId,
                        message,
                        chunk -> sendSse(emitter, "delta", Map.of("content", chunk))
                );
                if (guarded.isPresent()) {
                    AiMessage result = guarded.get();
                    sendSse(emitter, "done", Map.of(
                            "id", result.getId(),
                            "content", result.getContent()
                    ));
                    streamFinished.set(true);
                    emitter.complete();
                    return;
                }

                cost = aiService.getModelCost(model);
                userLevelService.assertAiQuotaAvailable(userId, cost);
                sendSse(emitter, "state", Map.of("message", "正在请求模型，首段内容返回后会立即显示"));
                AiMessage result = aiService.streamChat(
                        userId,
                        conversationId,
                        message,
                        model,
                        userCode,
                        language,
                        testContext,
                        chunk -> sendSse(emitter, "delta", Map.of("content", chunk))
                );
                userLevelService.consumeAiQuota(userId, cost);
                dailyTaskService.incrementTaskProgress(userId, "ai_chat");
                sendSse(emitter, "done", Map.of(
                        "id", result.getId(),
                        "content", result.getContent()
                ));
                streamFinished.set(true);
                emitter.complete();
            } catch (Exception e) {
                log.error("AI stream chat调用失败，未扣除积分(cost={})", cost, e);
                sendSse(emitter, "error", Map.of(
                        "message", e.getMessage() != null ? e.getMessage() : "AI服务调用失败"
                ));
                streamFinished.set(true);
                emitter.complete();
            } finally {
                streamFinished.set(true);
            }
        });
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache, no-transform")
                .header("Connection", "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    /** 删除指定对话 */
    @DeleteMapping("/conversations/{id}")
    public Result<Void> deleteConversation(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        aiService.deleteConversation(userId, id);
        return Result.success();
    }

    /** AI分析指定题目的解题思路 */
    @PostMapping("/analyze/{problemId}")
    public Result<Map<String, Object>> analyzeProblem(@AuthenticationPrincipal UserDetails userDetails,
                                                       @PathVariable Long problemId) {
        Long userId = getUserId(userDetails);
        userLevelService.assertAiQuotaAvailable(userId, 1.0);
        try {
            Map<String, Object> result = aiService.analyzeProblem(problemId);
            userLevelService.consumeAiQuota(userId);
            dailyTaskService.incrementTaskProgress(userId, "ai_chat");
            return Result.success(result);
        } catch (Exception e) {
            throw e;
        }
    }

    /** AI根据标签和难度自动生成题目，支持指定模型 */
    @PostMapping("/generate-problem")
    public Result<Map<String, Object>> generateProblem(@AuthenticationPrincipal UserDetails userDetails,
                                                        @RequestBody Map<String, String> body) {
        Long userId = getUserId(userDetails);
        String model = body.get("model");
        double cost = aiService.getModelCost(model);
        userLevelService.assertAiQuotaAvailable(userId, cost);
        try {
            String tags = body.getOrDefault("tags", "算法");
            String difficulty = body.getOrDefault("difficulty", "普通");
            String language = body.getOrDefault("language", "C++");
            String prompt = body.getOrDefault("prompt", tags);
            Integer exampleCount = parseInteger(body.get("exampleCount"), 2);
            Integer testCaseCount = parseInteger(body.get("testCaseCount"), 5);
            Integer totalScore = parseInteger(body.get("totalScore"), 20);
            Map<String, Object> result = aiService.generateProblem(
                    tags,
                    difficulty,
                    language,
                    model,
                    prompt,
                    exampleCount,
                    testCaseCount,
                    totalScore
            );
            userLevelService.consumeAiQuota(userId, cost);
            return Result.success(result);
        } catch (Exception e) {
            throw e;
        }
    }

    /** AI生成单题或一套多题草稿，前端可应用到当前表单或批量创建私有题 */
    @PostMapping("/generate-problems")
    public Result<List<Map<String, Object>>> generateProblems(@AuthenticationPrincipal UserDetails userDetails,
                                                              @RequestBody Map<String, String> body) {
        Long userId = getUserId(userDetails);
        String model = body.get("model");
        Integer count = parseInteger(body.get("count"), 2);
        int safeCount = Math.max(1, Math.min(6, count));
        double cost = aiService.getModelCost(model) * safeCount;
        userLevelService.assertAiQuotaAvailable(userId, cost);
        try {
            String tags = body.getOrDefault("tags", "算法");
            String difficulty = body.getOrDefault("difficulty", "普通");
            String language = body.getOrDefault("language", "C++");
            String prompt = body.getOrDefault("prompt", tags);
            Integer exampleCount = parseInteger(body.get("exampleCount"), 2);
            Integer testCaseCount = parseInteger(body.get("testCaseCount"), 5);
            Integer totalScore = parseInteger(body.get("totalScore"), 20);
            List<Map<String, Object>> result;
            if (safeCount <= 1) {
                result = List.of(aiService.generateProblem(
                        tags,
                        difficulty,
                        language,
                        model,
                        prompt,
                        exampleCount,
                        testCaseCount,
                        totalScore
                ));
            } else {
                result = aiService.generateProblems(
                        tags,
                        difficulty,
                        language,
                        model,
                        prompt,
                        exampleCount,
                        testCaseCount,
                        totalScore,
                        safeCount
                );
            }
            if (result == null || result.isEmpty()) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI未生成有效题目，请调整提示词后重试");
            }
            userLevelService.consumeAiQuota(userId, cost);
            return Result.success(result);
        } catch (Exception e) {
            throw e;
        }
    }

    /** 创建 AI 出题异步任务：先校验额度，任务成功后才真正扣除积分。 */
    @PostMapping("/generate-problems/async")
    public Result<Map<String, Object>> generateProblemsAsync(@AuthenticationPrincipal UserDetails userDetails,
                                                             @RequestBody Map<String, String> body) {
        Long userId = getUserId(userDetails);
        Map<String, String> requestBody = new HashMap<>(body);
        String model = requestBody.get("model");
        Integer count = parseInteger(requestBody.get("count"), 2);
        int safeCount = Math.max(1, Math.min(6, count));
        double cost = aiService.getModelCost(model) * safeCount;
        userLevelService.assertAiQuotaAvailable(userId, cost);

        String jobId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        problemGenerationJobs.put(jobId, new AiProblemGenerationJob(
                jobId,
                userId,
                "PENDING",
                "QUEUED",
                "任务已创建，等待后端执行",
                safeCount,
                null,
                null,
                now,
                now
        ));

        // 长耗时模型调用放到后台执行，前端通过 jobId 轮询，避免反向代理请求超时。
        CompletableFuture.runAsync(() -> runProblemGenerationJob(jobId, userId, requestBody, safeCount, cost));

        return Result.success(Map.of(
                "jobId", jobId,
                "status", "PENDING",
                "count", safeCount
        ));
    }

    /** 查询 AI 出题任务状态，返回 PENDING/RUNNING/SUCCESS/FAILED 以及候选题结果。 */
    @GetMapping("/generate-problems/jobs/{jobId}")
    public Result<Map<String, Object>> getProblemGenerationJob(@AuthenticationPrincipal UserDetails userDetails,
                                                               @PathVariable String jobId) {
        Long userId = getUserId(userDetails);
        AiProblemGenerationJob job = problemGenerationJobs.getIfPresent(jobId);
        if (job == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "AI generation job not found or expired");
        }
        if (!job.userId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "No permission to view this AI generation job");
        }
        return Result.success(job.toResponse());
    }

    /** AI为指定题目生成题解 */
    @PostMapping("/generate-solution/{problemId}")
    public Result<Map<String, Object>> generateSolution(@AuthenticationPrincipal UserDetails userDetails,
                                                         @PathVariable Long problemId,
                                                         @RequestBody Map<String, String> body) {
        Long userId = getUserId(userDetails);
        userLevelService.assertAiQuotaAvailable(userId, 1.0);
        try {
            String prompt = body.getOrDefault("prompt", "请生成详细的解题思路和参考代码");
            Map<String, Object> result = aiService.generateSolution(problemId, prompt);
            userLevelService.consumeAiQuota(userId);
            return Result.success(result);
        } catch (Exception e) {
            throw e;
        }
    }

    /** AI根据草稿题目信息生成题解 */
    @PostMapping("/generate-solution-draft")
    public Result<Map<String, Object>> generateSolutionDraft(@AuthenticationPrincipal UserDetails userDetails,
                                                              @RequestBody Map<String, String> body) {
        Long userId = getUserId(userDetails);
        userLevelService.assertAiQuotaAvailable(userId, 1.0);
        try {
            String prompt = body.getOrDefault("prompt", "请生成详细的解题思路和参考代码");
            Map<String, Object> result = aiService.generateSolutionFromDraft(body, prompt);
            userLevelService.consumeAiQuota(userId);
            return Result.success(result);
        } catch (Exception e) {
            throw e;
        }
    }

    /** 获取可用的AI模型列表 */
    @GetMapping("/models")
    public Result<List<Map<String, String>>> getAvailableModels() {
        return Result.success(aiService.getAvailableModels());
    }

    private void sendSse(SseEmitter emitter, String eventName, Object data) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI流式输出中断");
        }
    }

    private boolean isDirectCodeRequest(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String compact = message.toLowerCase().replaceAll("\\s+", "");
        return containsAny(compact,
                "\u5b8c\u6574\u4ee3\u7801",
                "\u5b8c\u6574\u5b9e\u73b0",
                "\u76f4\u63a5\u7ed9\u4ee3\u7801",
                "\u76f4\u63a5\u7ed9\u6211\u4ee3\u7801",
                "\u76f4\u63a5\u5199\u4ee3\u7801",
                "\u5e2e\u6211\u5199\u4ee3\u7801",
                "\u5199\u4e00\u4e0b\u4ee3\u7801",
                "\u6c42\u4ee3\u7801",
                "\u7ed9\u6211\u4ee3\u7801",
                "\u7ed9\u7b54\u6848",
                "\u76f4\u63a5\u8fc7",
                "\u76f4\u63a5\u590d\u5236",
                "\u5e2e\u6211\u5199\u5b8c",
                "ac\u4ee3\u7801",
                "ac\u7b54\u6848",
                "\u6ee1\u5206\u4ee3\u7801",
                "\u53ef\u63d0\u4ea4",
                "\u76f4\u63a5\u7b54\u6848",
                "\u6700\u7ec8\u7b54\u6848",
                "\u6807\u51c6\u7b54\u6848",
                "\u7167\u7740\u6253",
                "\u6284\u7b54\u6848",
                "\u4e00\u4efd\u4ee3\u7801",
                "\u901a\u8fc7\u6240\u6709\u6d4b\u8bd5",
                "directcode",
                "fullcode",
                "acceptedcode");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase().replaceAll("\\s+", ""))) {
                return true;
            }
        }
        return false;
    }

    private String directCodeGuardReply() {
        return """
                这类请求我不会直接给完整可提交代码，因为那会绕过训练过程，也不利于你答辩时现场说明。

                我会按老师带练的方式继续帮你：

                1. 先把题意翻译成“输入是什么、要输出什么、要维护什么变量”。
                2. 用公开样例手算 3-6 步关键状态。
                3. 结合你编辑器里的代码，指出最该先查的一处。
                4. 最多给 3 行伪代码提示，但不写完整实现。

                你现在可以改问：`用公开样例手算我当前代码的变量变化`，或者 `只指出我当前代码最该先查的一处`。
                """;
    }

    private void emitLocalText(SseEmitter emitter, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            chunk.append(text.charAt(i));
            if (chunk.length() >= 18 || "\n。！？".indexOf(text.charAt(i)) >= 0) {
                sendSse(emitter, "delta", Map.of("content", chunk.toString()));
                chunk.setLength(0);
            }
        }
        if (!chunk.isEmpty()) {
            sendSse(emitter, "delta", Map.of("content", chunk.toString()));
        }
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void runProblemGenerationJob(String jobId, Long userId, Map<String, String> body,
                                         int safeCount, double cost) {
        updateProblemGenerationJob(jobId, old -> old.withStatus(
                "RUNNING",
                "PREPARING",
                "正在读取题库标签、难度规则和生成参数",
                null,
                null
        ));
        try {
            String model = body.get("model");
            String tags = body.getOrDefault("tags", "算法");
            String difficulty = body.getOrDefault("difficulty", "普通");
            String language = body.getOrDefault("language", "C++");
            String prompt = body.getOrDefault("prompt", tags);
            Integer exampleCount = parseInteger(body.get("exampleCount"), 2);
            Integer testCaseCount = parseInteger(body.get("testCaseCount"), 5);
            Integer totalScore = parseInteger(body.get("totalScore"), 20);

            updateProblemGenerationJob(jobId, old -> old.withStatus(
                    "RUNNING",
                    "CALLING_MODEL",
                    safeCount <= 1 ? "正在生成单题，页面会一直等真实结果" : "正在批量生成题目，页面会一直等真实结果",
                    null,
                    null
            ));
            List<Map<String, Object>> result = safeCount <= 1
                    ? List.of(aiService.generateProblem(
                            tags,
                            difficulty,
                            language,
                            model,
                            prompt,
                            exampleCount,
                            testCaseCount,
                            totalScore
                    ))
                    : aiService.generateProblems(
                            tags,
                            difficulty,
                            language,
                            model,
                            prompt,
                            exampleCount,
                            testCaseCount,
                            totalScore,
                            safeCount
                    );
            if (result == null || result.isEmpty()) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI did not generate a valid problem");
            }
            updateProblemGenerationJob(jobId, old -> old.withStatus(
                    "RUNNING",
                    "NORMALIZING",
                    "模型已返回，正在校验题面、标签、难度和评测点",
                    null,
                    null
            ));
            userLevelService.consumeAiQuota(userId, cost);
            updateProblemGenerationJob(jobId, old -> old.withStatus(
                    "SUCCEEDED",
                    "COMPLETED",
                    "题目已生成，已通过平台字段校验",
                    result,
                    null
            ));
        } catch (Exception e) {
            log.error("Async AI problem generation failed, quota was not deducted: jobId={}, userId={}",
                    jobId, userId, e);
            String errorMessage = safeErrorMessage(e);
            updateProblemGenerationJob(jobId, old -> old.withStatus(
                    "FAILED",
                    "FAILED",
                    errorMessage,
                    null,
                    errorMessage
            ));
        }
    }

    private void updateProblemGenerationJob(String jobId,
                                            java.util.function.Function<AiProblemGenerationJob, AiProblemGenerationJob> updater) {
        AiProblemGenerationJob old = problemGenerationJobs.getIfPresent(jobId);
        if (old != null) {
            problemGenerationJobs.put(jobId, updater.apply(old));
        }
    }

    private String safeErrorMessage(Exception e) {
        if (e instanceof BusinessException && e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
            return "AI出题超时，请换用更快模型或减少题目数量后重试";
        }
        return "AI出题失败，请稍后重试；本次不会扣除积分";
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return user.getId();
    }

    private record AiProblemGenerationJob(String jobId,
                                          Long userId,
                                          String status,
                                          String phase,
                                          String message,
                                          Integer count,
                                          List<Map<String, Object>> result,
                                          String error,
                                          LocalDateTime createdAt,
                                          LocalDateTime updatedAt) {

        AiProblemGenerationJob withStatus(String nextStatus, String nextPhase, String nextMessage,
                                          List<Map<String, Object>> nextResult, String nextError) {
            return new AiProblemGenerationJob(
                    jobId,
                    userId,
                    nextStatus,
                    nextPhase,
                    nextMessage,
                    count,
                    nextResult,
                    nextError,
                    createdAt,
                    LocalDateTime.now()
            );
        }

        Map<String, Object> toResponse() {
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("status", status);
            response.put("phase", phase);
            response.put("message", message);
            response.put("count", count);
            response.put("createdAt", createdAt);
            response.put("updatedAt", updatedAt);
            if (result != null) {
                response.put("result", result);
            }
            if (error != null) {
                response.put("error", error);
            }
            return response;
        }
    }
}

/**
 * 文件说明：代码测试与在线评测服务实现，负责 Judge0 请求封装、节点选择和资源限制。
 */
package com.ls.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.config.Judge0Properties;
import com.ls.domain.Problem;
import com.ls.service.CodeTestService;
import com.ls.service.ProblemService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 代码评测服务实现。
 * 负责把后端提交转换成 Judge0 请求，并在多个远程评测节点之间做健康检测、并发控制和负载选择。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeTestServiceImpl implements CodeTestService {

    private final ProblemService problemService;
    private final ObjectMapper objectMapper;
    private final Judge0Properties judge0Properties;

    private List<JudgeNodeRuntime> judgeNodes = List.of();

    /** 启动时读取 Judge0 节点配置，为每个节点创建运行时限流器。 */
    @PostConstruct
    void initJudge0Limiter() {
        List<Judge0Properties.Node> nodeConfigs = effectiveNodeConfigs();
        List<JudgeNodeRuntime> runtimes = new ArrayList<>();
        for (int i = 0; i < nodeConfigs.size(); i++) {
            Judge0Properties.Node config = nodeConfigs.get(i);
            if (!config.isEnabled() || config.getUrl() == null || config.getUrl().isBlank()) {
                continue;
            }
            if (config.getName() == null || config.getName().isBlank()) {
                config.setName("judge0-" + (i + 1));
            }
            if (config.getMaxConcurrent() <= 0) {
                config.setMaxConcurrent(Math.max(1, judge0Properties.getApi().getMaxConcurrentSubmissions()));
            }
            if (config.getWeight() <= 0) {
                config.setWeight(1);
            }
            runtimes.add(new JudgeNodeRuntime(config));
        }
        if (runtimes.isEmpty()) {
            throw new IllegalStateException("No enabled Judge0 nodes configured");
        }
        judgeNodes = List.copyOf(runtimes);
        log.info("Judge0 cluster initialized, nodes={}", judgeNodes.stream()
                .map(node -> node.config.getName() + "(" + node.config.getUrl() + ", max=" + node.config.getMaxConcurrent() + ")")
                .toList());
    }

    /** 调试运行代码：优先使用前端传入样例，没有传入时使用题目公开/调试样例。 */
    @Override
    public Map<String, Object> testCode(Long problemId, String code, Integer languageId, String stdin, String expectedOutput,
                                      Integer timeLimit, Integer memoryLimit) {
        Problem problem = problemService.getProblemById(problemId);
        if (problem == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }

        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "代码不能为空");
        }
        if (languageId == null || languageId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的语言ID");
        }

        log.debug("测试代码 - problemId={}, languageId={}, codeLength={}", problemId, languageId, code.length());

        if (stdin == null || stdin.isEmpty()) {
            stdin = encodeOptional(problem.getDebugInputExample());
            if (stdin == null) {
                stdin = encodeOptional(problem.getInputExample());
            }
        }

        if (expectedOutput == null || expectedOutput.isEmpty()) {
            expectedOutput = encodeOptional(problem.getDebugOutputExample());
            if (expectedOutput == null) {
                expectedOutput = encodeOptional(problem.getOutputExample());
            }
        }

        if (timeLimit == null) {
            timeLimit = problem.getTimeLimit();
        }
        if (memoryLimit == null) {
            memoryLimit = problem.getMemoryLimit();
        }

        Map<String, Object> result = executeCodeWithJudge0(code, languageId, stdin, expectedOutput, timeLimit, memoryLimit);
        Map<String, Object> response = new HashMap<>();
        response.put("input", stdin);
        response.put("expectedOutput", expectedOutput);
        response.put("error", result.get("error"));
        response.put("stdout", result.get("stdout"));
        response.put("stderr", result.get("stderr"));
        response.put("compile_output", result.get("compile_output"));
        response.put("status", result.get("status"));
        response.put("message", result.get("message"));
        response.put("time", result.get("time"));
        response.put("memory", result.get("memory"));
        response.put("token", result.get("token"));
        response.put("statusId", result.get("statusId"));
        response.put("statusDescription", result.get("statusDescription"));
        return response;
    }

    /** 将前端语言标识映射为 Judge0 的 language_id。 */
    @Override
    public int getJudge0LanguageId(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> 62;
            case "python" -> 71;
            case "cpp" -> 54;
            case "c" -> 50;
            case "javascript" -> 63;
            case "typescript" -> 74;
            case "assembly" -> 45;
            case "bash" -> 46;
            case "c_clang" -> 75;
            case "cpp_clang" -> 76;
            case "c_gcc7" -> 48;
            case "cpp_gcc7" -> 52;
            case "c_gcc8" -> 49;
            case "cpp_gcc8" -> 53;
            case "csharp" -> 51;
            case "go" -> 60;
            case "rust" -> 73;
            default -> 54;
        };
    }

    /**
     * 选择 Judge0 节点执行代码，失败时自动切换到其他健康节点重试。
     *
     * 调用链：CodeTestController.judgeAllTestCases() → 本方法 → acquireJudgeNode() → executeCodeOnNode()
     * 外部调用：POST {nodeUrl}/submissions?base64_encoded=true&wait=true
     * 内部重试：节点失败 → 加入黑名单 → 换下一个节点 → 直到成功或所有节点都失败
     */
    @Override
    public Map<String, Object> executeCodeWithJudge0(String sourceCode, int languageId, String stdin, String expectedOutput,
                                                  Integer timeLimit, Integer memoryLimit) {
        if (sourceCode == null || sourceCode.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "源代码不能为空");
        }
        if (languageId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的语言ID");
        }

        long deadline = System.currentTimeMillis() + Math.max(1000, judge0Properties.getApi().getAcquireTimeoutMs());
        List<String> attemptedNodes = new ArrayList<>();
        Exception lastFailure = null;

        // ========== 节点选择 + 调用 + 故障切换 主循环 ==========
        try {
            while (System.currentTimeMillis() <= deadline && attemptedNodes.size() < judgeNodes.size()) {
                // ① acquireJudgeNode：按负载+权重选节点，拿信号量
                JudgeNodeRuntime node = acquireJudgeNode(attemptedNodes, deadline);
                if (node == null) break;

                long start = System.nanoTime();
                try {
                    // ② executeCodeOnNode：发 HTTP POST 到 Judge0，等返回
                    Map<String, Object> result = executeCodeOnNode(node, sourceCode, languageId, stdin,
                            expectedOutput, timeLimit, memoryLimit);
                    // ③ markSuccess：重置连续失败计数，解除熔断
                    long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    node.markSuccess(latencyMs);
                    result.put("judgeNode", node.config.getName());
                    result.put("judgeLatencyMs", latencyMs);
                    return result;
                } catch (Judge0NodeException e) {
                    // ④ 失败 → markFailure（累计失败，≥3次熔断30秒）→ 加入黑名单 → 换节点重试
                    long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    node.markFailure(e.getMessage(), latencyMs);
                    attemptedNodes.add(node.config.getName());
                    lastFailure = e;
                    log.warn("Judge0 node failed, node={}, attempted={}, error={}",
                            node.config.getName(), attemptedNodes, e.getMessage());
                } finally {
                    node.release();
                }
            }

            String message = lastFailure != null && lastFailure.getMessage() != null
                    ? lastFailure.getMessage() : "Judge0评测队列繁忙，请稍后重试";
            return Map.of("error", "评测服务暂时不可用：" + message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("error", "Judge0评测请求已中断，请稍后重试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行代码异常", e);
            return Map.of("error", "执行代码时发生错误: " + e.getMessage());
        }
    }

    /** 对单个 Judge0 节点发起实际 HTTP 请求，并把 Judge0 返回内容解码成业务可读结果。 */
    private Map<String, Object> executeCodeOnNode(JudgeNodeRuntime node,
                                                  String sourceCode,
                                                  int languageId,
                                                  String stdin,
                                                  String expectedOutput,
                                                  Integer timeLimit,
                                                  Integer memoryLimit) {
        try {
            log.debug("调用 Judge0 - node={}, languageId={}, sourceLength={}",
                    node.config.getName(), languageId, sourceCode.length());
            int effectiveMemoryLimit = normalizeMemoryLimitKb(memoryLimit);
            int maxProcessesAndThreads = resolveMaxProcessesAndThreads(languageId);
            double cpuTimeLimit = timeLimit != null ? Math.max(0.1, timeLimit / 1000.0) : 5.0;
            double wallTimeLimit = Math.max(judge0Properties.getApi().getWallTimeLimitSeconds(), cpuTimeLimit + 1.0);

            // ========== Judge0 评测请求 JSON ==========
            // POST {nodeUrl}/submissions?base64_encoded=true&wait=true
            // 请求体示例:
            // {
            //   "source_code": "cHJpbnQoImhlbGxvIik=",    ← Base64编码的用户代码
            //   "language_id": 71,                         ← Python3=71, Java=62, Cpp=54, C=50
            //   "stdin": "MQo=",                           ← Base64编码的测试输入
            //   "expected_output": "aGVsbG8=",             ← Base64编码的期望输出
            //   "cpu_time_limit": 2.0,                     ← CPU时间上限(秒)
            //   "memory_limit": 262144,                    ← 内存上限(KB)
            //   "enable_network": false                    ← 禁止用户代码访问外网
            // }
            // 响应 JSON:
            // {
            //   "stdout": "hello",           ← Base64解码后的实际输出
            //   "status": {"id": 3},         ← 3=ACCEPTED, 4=WA, 5=TLE, 6=CE
            //   "time": "0.05",             ← 运行耗时(秒)
            //   "memory": 8192,              ← 内存占用(KB)
            //   "compile_output": "..."     ← 编译错误信息(如有)
            // }
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("source_code", sourceCode);
            requestBody.put("language_id", languageId);
            requestBody.put("stdin", stdin);
            requestBody.put("expected_output", expectedOutput);
            requestBody.put("cpu_time_limit", cpuTimeLimit);
            requestBody.put("cpu_extra_time", Math.max(0.0, judge0Properties.getApi().getCpuExtraTimeSeconds()));
            requestBody.put("wall_time_limit", wallTimeLimit);
            requestBody.put("memory_limit", effectiveMemoryLimit);
            requestBody.put("max_processes_and_or_threads", maxProcessesAndThreads);
            requestBody.put("enable_per_process_and_thread_time_limit", judge0Properties.getApi().isEnablePerProcessAndThreadTimeLimit());
            requestBody.put("enable_per_process_and_thread_memory_limit", judge0Properties.getApi().isEnablePerProcessAndThreadMemoryLimit());
            // 资源限制是评测安全的关键：CPU、墙钟、内存、进线程数量和禁网络都在这里下发给 Judge0。
            requestBody.put("enable_network", false);

            // wait=true → 同步等待 Judge0 返回完整结果（不是异步回调）。
            // 这里虽然是"同步"请求，但外层 JudgeQueueExecutor 工作线程保证了不阻塞用户页面。
            URL url = URI.create(trimTrailingSlash(node.config.getUrl()) + "/submissions?base64_encoded=true&wait=true").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            applyNodeAuth(connection, node.config);
            connection.setDoOutput(true);
            connection.setConnectTimeout(judge0Properties.getApi().getConnectTimeoutMs());
            connection.setReadTimeout(judge0Properties.getApi().getReadTimeoutMs());

            // ========== 发送请求体 ==========
            try (OutputStream os = connection.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(requestBody));
            }

            // ========== 读取 Judge0 响应 ==========
            int responseCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, responseCode);
            log.debug("Judge0 响应 - status={}, bodyLength={}", responseCode, responseBody.length());

            if (responseCode < 200 || responseCode >= 300) {
                log.error("Judge0 API调用失败: status={}, body={}", responseCode, abbreviate(responseBody));
                throw new Judge0NodeException("HTTP " + responseCode);
            }

            Map<String, Object> resultMap = objectMapper.convertValue(
                    objectMapper.readTree(responseBody),
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)
            );
            resultMap = safeBase64Decode(resultMap, "stdout");
            resultMap = safeBase64Decode(resultMap, "stderr");
            resultMap = safeBase64Decode(resultMap, "compile_output");
            resultMap = safeBase64Decode(resultMap, "message");

            if (resultMap.get("stdout") == null) {
                resultMap.put("stdout", "");
            }
            resultMap.put("decoded", true);

            if (resultMap.get("status") instanceof Map<?, ?> status) {
                resultMap.put("statusId", status.get("id"));
                resultMap.put("statusDescription", status.get("description"));
                if (Integer.valueOf(6).equals(status.get("id")) && resultMap.containsKey("compile_output")) {
                    if (resultMap.get("stdout") == null || "".equals(resultMap.get("stdout"))) {
                        resultMap.put("stdout", resultMap.get("compile_output"));
                    }
                }
            }

            return resultMap;
        } catch (Exception e) {
            throw new Judge0NodeException(e.getMessage(), e);
        }
    }

    /**
     * 汇总评测集群状态，供管理员后台展示节点健康、并发占用和节点队列。
     *
     * 返回 JSON 格式（前端 AdminView.vue 的 judgeCluster 面板展示）:
     * {
     *   "nodes": [{
     *     "name": "JudgeCluster-A",
     *     "url": "http://your-judge0-host:2358",
     *     "enabled": true,
     *     "healthy": true,             ← 健康状态 (UP + 未熔断)
     *     "status": "UP",              ← UP / DOWN / CIRCUIT_OPEN
     *     "weight": 1,                 ← 权重
     *     "maxConcurrent": 3,          ← 最大并发
     *     "inFlight": 2,              ← 当前正在执行的请求数
     *     "available": 1,             ← 剩余可用信号量
     *     "totalRequests": 500,       ← 历史总请求数
     *     "successRequests": 495,     ← 成功数
     *     "failedRequests": 5,        ← 失败数
     *     "consecutiveFailures": 0,   ← 连续失败次数(≥3熔断)
     *     "avgLatencyMs": 180,        ← 平均延迟(ms)
     *     "lastLatencyMs": 150,       ← 最近一次延迟(ms)
     *     "systemInfo": {"cpu":"4","model":"Intel...","memory":"8GB"}
     *   }],
     *   "totalCapacity": 3,           ← 总并发容量
     *   "totalInFlight": 2,           ← 总在途请求数
     *   "healthyCount": 1             ← 健康节点数
     * }
     */
    @Override
    public Map<String, Object> getJudgeClusterStatus() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        int totalCapacity = 0;
        int totalInFlight = 0;
        int totalQueueLength = 0;
        int healthyCount = 0;
        for (JudgeNodeRuntime node : judgeNodes) {
            checkNodeHealth(node);
            Map<String, Object> item = node.toStatusMap();
            nodes.add(item);
            totalCapacity += node.config.getMaxConcurrent();
            totalInFlight += node.inFlight.get();
            totalQueueLength += node.semaphore.getQueueLength();
            if (Boolean.TRUE.equals(item.get("healthy"))) {
                healthyCount++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("healthy", healthyCount > 0);
        result.put("healthyNodes", healthyCount);
        result.put("totalNodes", judgeNodes.size());
        result.put("totalCapacity", totalCapacity);
        result.put("totalInFlight", totalInFlight);
        result.put("totalAvailable", Math.max(0, totalCapacity - totalInFlight));
        result.put("totalQueueLength", totalQueueLength);
        result.put("nodes", nodes);
        return result;
    }

    /** 兼容旧配置：没有配置节点列表时，把单个 Judge0 地址包装成 JudgeCluster-A。 */
    private List<Judge0Properties.Node> effectiveNodeConfigs() {
        if (judge0Properties.getNodes() != null && !judge0Properties.getNodes().isEmpty()) {
            return judge0Properties.getNodes();
        }
        Judge0Properties.Node fallback = new Judge0Properties.Node();
        fallback.setName("JudgeCluster-A");
        fallback.setUrl(judge0Properties.getApi().getUrl());
        fallback.setWeight(1);
        fallback.setMaxConcurrent(Math.max(1, judge0Properties.getApi().getMaxConcurrentSubmissions()));
        fallback.setWorkers(Math.max(1, judge0Properties.getApi().getMaxConcurrentSubmissions()));
        fallback.setEnabled(true);
        return List.of(fallback);
    }

    /**
     * 按当前负载、权重和熔断状态选择一个可以接收任务的评测节点。
     *
     * 选择流程：
     *   1. 过滤：节点启用 + 未在本轮失败过 + 未熔断
     *   2. 排序：按 loadScore() 升序，loadScore 低的优先
     *   3. loadScore 公式：(当前并发数 inFlight / 最大并发 maxConcurrent) / 权重 weight
     *      → 权重高的节点、负载低的节点，loadScore 更小，更优先被选
     *   4. 尝试获取信号量 tryAcquire()，拿不到则试下一个节点
     *   5. 所有节点都拿不到 → sleep 30ms 后重试，直到 10 秒超时
     */
    private JudgeNodeRuntime acquireJudgeNode(List<String> attemptedNodes, long deadline) throws InterruptedException {
        while (System.currentTimeMillis() <= deadline) {
            List<JudgeNodeRuntime> candidates = judgeNodes.stream()
                    .filter(node -> node.config.isEnabled())
                    .filter(node -> !attemptedNodes.contains(node.config.getName()))
                    .filter(node -> !node.isCircuitOpen())
                    .sorted(Comparator.comparingDouble(JudgeNodeRuntime::loadScore)
                            .thenComparing(node -> node.config.getName()))
                    .toList();
            for (JudgeNodeRuntime node : candidates) {
                if (node.tryAcquire()) {
                    return node;
                }
            }
            Thread.sleep(30L);
        }
        return null;
    }

    /** 如果某个 Judge0 节点需要鉴权，则给请求补充配置中的鉴权头。 */
    private void applyNodeAuth(HttpURLConnection connection, Judge0Properties.Node node) {
        if (node.getAuthHeader() != null && !node.getAuthHeader().isBlank()
                && node.getAuthToken() != null && !node.getAuthToken().isBlank()) {
            connection.setRequestProperty(node.getAuthHeader(), node.getAuthToken());
        }
    }

    /** 轻量探测 Judge0 节点健康状态和系统信息，避免后台监控展示过期状态。 */
    private void checkNodeHealth(JudgeNodeRuntime node) {
        long now = System.currentTimeMillis();
        if (now - node.lastHealthCheckAt.get() < 10_000) {
            return;
        }
        if (!node.lastHealthCheckAt.compareAndSet(node.lastHealthCheckAt.get(), now)) {
            return;
        }
        try {
            URL url = URI.create(trimTrailingSlash(node.config.getUrl()) + "/system_info").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            applyNodeAuth(connection, node.config);
            connection.setConnectTimeout(Math.min(2000, judge0Properties.getApi().getConnectTimeoutMs()));
            connection.setReadTimeout(Math.min(3000, judge0Properties.getApi().getReadTimeoutMs()));
            int code = connection.getResponseCode();
            String body = readResponseBody(connection, code);
            if (code >= 200 && code < 300) {
                node.lastHealthStatus.set("UP");
                node.lastHealthError = null;
                node.lastSystemInfo = parseSystemInfo(body);
                node.consecutiveFailures.set(0);
                node.circuitOpenUntil.set(0);
            } else {
                node.lastHealthStatus.set("DOWN");
                node.lastHealthError = "HTTP " + code;
            }
        } catch (Exception e) {
            node.lastHealthStatus.set("DOWN");
            node.lastHealthError = e.getMessage();
        }
    }

    private Map<String, Object> parseSystemInfo(String body) {
        try {
            Map<String, Object> raw = objectMapper.convertValue(
                    objectMapper.readTree(body),
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)
            );
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("cpu", raw.get("CPU(s)"));
            info.put("model", raw.get("Model name"));
            info.put("memory", raw.get("Mem"));
            info.put("swap", raw.get("Swap"));
            info.put("architecture", raw.get("Architecture"));
            return info;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String readResponseBody(HttpURLConnection connection, int responseCode) throws Exception {
        InputStream stream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
        }
        return response.toString();
    }

    private String encodeOptional(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> safeBase64Decode(Map<String, Object> map, String fieldName) {
        if (map.containsKey(fieldName) && map.get(fieldName) != null) {
            String base64Value = (String) map.get(fieldName);
            try {
                base64Value = base64Value.replaceAll("\\s", "");
                if (base64Value.isEmpty()) {
                    map.put(fieldName, "");
                    return map;
                }
                byte[] decodedBytes = Base64.getDecoder().decode(base64Value);
                String decodedValue = new String(decodedBytes, StandardCharsets.UTF_8);
                map.put(fieldName, decodedValue);
                map.put(fieldName + "_base64", base64Value);
            } catch (Exception e) {
                log.warn("解码{}失败: {}", fieldName, e.getMessage());
                map.put(fieldName + "_decode_error", e.getMessage());
                if (!map.containsKey(fieldName + "_original")) {
                    map.put(fieldName + "_original", base64Value);
                }
            }
        }
        return map;
    }

    private int resolveMaxProcessesAndThreads(int languageId) {
        int baseLimit = Math.max(2, judge0Properties.getApi().getMaxProcessesAndThreads());
        if (languageId == 62 || languageId == 78 || languageId == 81 || languageId == 87) {
            return Math.max(baseLimit, judge0Properties.getApi().getJvmMaxProcessesAndThreads());
        }
        return baseLimit;
    }

    private int normalizeMemoryLimitKb(Integer memoryLimit) {
        if (memoryLimit == null || memoryLimit <= 0) {
            return judge0Properties.getApi().getMaxMemoryKb();
        }
        int limitKb = memoryLimit <= 4096 ? memoryLimit * 1024 : memoryLimit;
        return Math.min(limitKb, judge0Properties.getApi().getMaxMemoryKb());
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }

    private String trimTrailingSlash(String url) {
        return url == null ? "" : url.replaceAll("/+$", "");
    }

    private String maskUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.replaceAll("(?i)(https?://)([^/@:]+):([^/@]+)@", "$1***:***@");
    }

    /** Judge0 节点调用失败时的内部异常，用于触发节点失败统计和切换。 */
    private static class Judge0NodeException extends RuntimeException {
        Judge0NodeException(String message) {
            super(message);
        }

        Judge0NodeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** 单个 Judge0 节点的运行时状态：并发信号量、失败次数、延迟和健康信息。 */
    private static class JudgeNodeRuntime {
        private final Judge0Properties.Node config;
        private final Semaphore semaphore;
        private final AtomicInteger inFlight = new AtomicInteger();
        private final AtomicInteger consecutiveFailures = new AtomicInteger();
        private final AtomicLong totalRequests = new AtomicLong();
        private final AtomicLong successRequests = new AtomicLong();
        private final AtomicLong failedRequests = new AtomicLong();
        private final AtomicLong totalLatencyMs = new AtomicLong();
        private final AtomicLong lastLatencyMs = new AtomicLong();
        private final AtomicLong circuitOpenUntil = new AtomicLong();
        private final AtomicLong lastHealthCheckAt = new AtomicLong();
        private final AtomicLong lastSelectedAt = new AtomicLong();
        private final AtomicLong lastResultAt = new AtomicLong();
        private final AtomicLong lastFailureAt = new AtomicLong();
        private final AtomicLong lastSuccessAt = new AtomicLong();
        private final java.util.concurrent.atomic.AtomicReference<String> lastHealthStatus =
                new java.util.concurrent.atomic.AtomicReference<>("UNKNOWN");
        private volatile String lastError;
        private volatile String lastHealthError;
        private volatile Map<String, Object> lastSystemInfo = Map.of();

        private JudgeNodeRuntime(Judge0Properties.Node config) {
            this.config = config;
            this.semaphore = new Semaphore(Math.max(1, config.getMaxConcurrent()), true);
        }

        /** 尝试获取节点的一个并发槽位。使用公平信号量 Semaphore(permits, fair=true) 控制并发上限。 */
        private boolean tryAcquire() {
            if (!semaphore.tryAcquire()) {
                return false;
            }
            inFlight.incrementAndGet();
            totalRequests.incrementAndGet();
            lastSelectedAt.set(System.currentTimeMillis());
            return true;
        }

        private void release() {
            inFlight.decrementAndGet();
            semaphore.release();
        }

        /** 标记本次请求成功，重置连续失败计数和熔断状态。 */
        private void markSuccess(long latencyMs) {
            successRequests.incrementAndGet();
            consecutiveFailures.set(0);
            circuitOpenUntil.set(0);
            lastLatencyMs.set(Math.max(0, latencyMs));
            totalLatencyMs.addAndGet(Math.max(0, latencyMs));
            lastResultAt.set(System.currentTimeMillis());
            lastSuccessAt.set(System.currentTimeMillis());
            lastError = null;
            lastHealthStatus.set("UP");
        }

        /** 标记本次请求失败。连续失败 3 次触发熔断，30 秒后自动恢复。 */
        private void markFailure(String error, long latencyMs) {
            failedRequests.incrementAndGet();
            int failures = consecutiveFailures.incrementAndGet();
            if (failures >= 3) {
                // 连续失败达到阈值 → 熔断 30 秒，期间该节点不会被选中
                circuitOpenUntil.set(System.currentTimeMillis() + 30_000L);
            }
            lastLatencyMs.set(Math.max(0, latencyMs));
            totalLatencyMs.addAndGet(Math.max(0, latencyMs));
            lastResultAt.set(System.currentTimeMillis());
            lastFailureAt.set(System.currentTimeMillis());
            lastError = error;
            lastHealthStatus.set("DOWN");
        }

        /** 判断节点当前是否处于熔断状态。熔断中不会被 acquireJudgeNode 选为候选节点。 */
        private boolean isCircuitOpen() {
            long openUntil = circuitOpenUntil.get();
            return openUntil > 0 && System.currentTimeMillis() < openUntil;
        }

        /** 计算节点当前负载分数。值越小 = 越空闲 = 优先被选。公式：(当前并发/最大并发)/权重。 */
        private double loadScore() {
            double capacity = Math.max(1, config.getMaxConcurrent());
            double weight = Math.max(1, config.getWeight());
            return (inFlight.get() / capacity) / weight;
        }

        private Map<String, Object> toStatusMap() {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", config.getName());
            item.put("url", config.getUrl());
            item.put("enabled", config.isEnabled());
            item.put("healthy", "UP".equals(lastHealthStatus.get()) && !isCircuitOpen());
            item.put("status", isCircuitOpen() ? "CIRCUIT_OPEN" : lastHealthStatus.get());
            item.put("weight", config.getWeight());
            int displayWorkers = config.getWorkers() > 0 ? config.getWorkers() : Math.max(1, config.getMaxConcurrent());
            item.put("workers", displayWorkers);
            item.put("workersConfigured", config.getWorkers() > 0);
            item.put("maxConcurrent", config.getMaxConcurrent());
            item.put("inFlight", inFlight.get());
            item.put("available", semaphore.availablePermits());
            item.put("queueLength", semaphore.getQueueLength());
            item.put("totalRequests", totalRequests.get());
            item.put("successRequests", successRequests.get());
            item.put("failedRequests", failedRequests.get());
            item.put("consecutiveFailures", consecutiveFailures.get());
            long completed = Math.max(1, successRequests.get() + failedRequests.get());
            item.put("avgLatencyMs", totalLatencyMs.get() / completed);
            item.put("lastLatencyMs", lastLatencyMs.get());
            item.put("lastError", lastError);
            item.put("lastHealthError", lastHealthError);
            item.put("lastSelectedAt", toDateTime(lastSelectedAt.get()));
            item.put("lastSuccessAt", toDateTime(lastSuccessAt.get()));
            item.put("lastFailureAt", toDateTime(lastFailureAt.get()));
            item.put("lastHealthCheckAt", toDateTime(lastHealthCheckAt.get()));
            item.put("systemInfo", lastSystemInfo);
            return item;
        }

        private LocalDateTime toDateTime(long millis) {
            if (millis <= 0) {
                return null;
            }
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault());
        }
    }
}

/**
 * 文件说明：题目 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ls.domain.Problem;
import com.ls.domain.JudgeTestCase;
import com.ls.domain.User;
import com.ls.service.ProblemService;
import com.ls.service.ProblemSolutionService;
import com.ls.service.ContestService;
import com.ls.mapper.TestCaseMapper;
import com.ls.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 题目控制器 — 题目增删改查、标签筛选、可见性管理 */
@Slf4j
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Autowired
    private ProblemService problemService;

    @Autowired
    private ProblemSolutionService problemSolutionService;

    @Autowired
    private ContestService contestService;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private UserMapper userMapper;

    // 通过 Spring Security 上下文获取当前用户ID
    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        User user = userMapper.findByUsername(userDetails.getUsername());
        return user != null ? user.getId() : null;
    }

    private boolean hasAdminRole(UserDetails userDetails) {
        if (userDetails == null) return false;
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void applyPublicExamples(Problem problem, Map<String, Object> request) {
        List<Map<String, Object>> examples = parseExamplesFromRequest(request.get("examples"));
        if (examples.isEmpty()) {
            String input = stringValue(request.get("inputExample"));
            String output = stringValue(request.get("outputExample"));
            if (!input.isBlank() || !output.isBlank()) {
                examples.add(exampleMap(input, output, ""));
            }
        }
        if (examples.isEmpty()) {
            examples.add(exampleMap("", stringValue(request.get("outputExample")), ""));
        }

        Map<String, Object> first = examples.get(0);
        problem.setInputExample(stringValue(first.get("input")));
        problem.setOutputExample(stringValue(first.get("output")));

        if (examples.size() > 1) {
            Map<String, Object> second = examples.get(1);
            problem.setDebugInputExample(stringValue(second.get("input")));
            problem.setDebugOutputExample(stringValue(second.get("output")));
        } else {
            problem.setDebugInputExample(problem.getInputExample());
            problem.setDebugOutputExample(problem.getOutputExample());
        }

        try {
            problem.setExamples(JSON_MAPPER.writeValueAsString(examples));
        } catch (Exception e) {
            log.warn("公开样例序列化失败，回退到第一组样例: {}", e.getMessage());
            problem.setExamples(null);
        }
    }

    private List<Map<String, Object>> parseExamplesFromRequest(Object examplesObj) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (examplesObj instanceof String text && !text.isBlank()) {
                examplesObj = JSON_MAPPER.readValue(text, new TypeReference<List<Map<String, Object>>>() {});
            }
            if (examplesObj instanceof List<?> list) {
                for (Object item : list) {
                    if (!(item instanceof Map<?, ?> map)) {
                        continue;
                    }
                    String input = stringValue(firstPresent(map, "input", "inputExample", "stdin"));
                    String output = stringValue(firstPresent(map, "output", "expectedOutput", "outputExample"));
                    String explanation = stringValue(firstPresent(map, "explanation", "explain", "description"));
                    if (!input.isBlank() || !output.isBlank()) {
                        result.add(exampleMap(input, output, explanation));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析公开样例失败: {}", e.getMessage());
        }
        return result;
    }

    private List<Map<String, Object>> parseProblemExamples(Problem problem) {
        List<Map<String, Object>> examples = parseExamplesFromRequest(problem.getExamples());
        if (examples.isEmpty() && hasSampleContent(problem.getInputExample(), problem.getOutputExample())) {
            examples.add(exampleMap(problem.getInputExample(), problem.getOutputExample(), ""));
        }
        if (hasSampleContent(problem.getDebugInputExample(), problem.getDebugOutputExample())) {
            String debugInput = stringValue(problem.getDebugInputExample());
            String debugOutput = stringValue(problem.getDebugOutputExample());
            boolean duplicated = examples.stream().anyMatch(example ->
                    stringValue(example.get("input")).equals(debugInput)
                            && stringValue(example.get("output")).equals(debugOutput));
            if (!duplicated) {
                examples.add(exampleMap(debugInput, debugOutput, ""));
            }
        }
        return examples;
    }

    private Map<String, Object> exampleMap(String input, String output, String explanation) {
        Map<String, Object> item = new HashMap<>();
        item.put("input", stringValue(input));
        item.put("output", stringValue(output));
        item.put("explanation", stringValue(explanation));
        return item;
    }

    private boolean hasSampleContent(String input, String output) {
        return !stringValue(input).isBlank() || !stringValue(output).isBlank();
    }

    private Object firstPresent(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** 获取题目列表（分页、筛选） */
    @GetMapping("")
    public ResponseEntity<?> getProblems(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Map<String, Object> params) {
        try {
            return ResponseEntity.ok(problemService.getProblems(current, size, params));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "服务器错误: " + e.getMessage()));
        }
    }

    /** 根据ID获取题目管理详情（含测试用例和题解代码，仅作者和管理员可用） */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProblemById(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Problem problem = problemService.getProblemById(id);
            if (problem == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "题目不存在"));
            }

            Long userId = resolveUserId(userDetails);
            boolean isAdmin = hasAdminRole(userDetails);
            boolean isAuthor = userId != null && userId.equals(problem.getAuthorId());
            if (!isAdmin && !isAuthor) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "完整题目详情仅作者或管理员可访问，请使用做题视图"));
            }

            List<JudgeTestCase> testCases = testCaseMapper.selectByProblemId(id);
            Map<String, String> solutionCodeMap = problemSolutionService.getOfficialSolutionCodeByProblemId(id);

            Map<String, Object> response = new HashMap<>();
            response.put("id", problem.getId());
            response.put("title", problem.getTitle());
            response.put("description", problem.getDescription());
            response.put("difficulty", problem.getDifficulty());
            response.put("inputFormat", problem.getInputFormat());
            response.put("outputFormat", problem.getOutputFormat());
            response.put("inputExample", problem.getInputExample());
            response.put("outputExample", problem.getOutputExample());
            response.put("examples", parseProblemExamples(problem));
            response.put("debugInputExample", problem.getDebugInputExample());
            response.put("debugOutputExample", problem.getDebugOutputExample());
            response.put("hint", problem.getHint());
            response.put("solution", problem.getSolution());
            response.put("timeLimit", problem.getTimeLimit());
            response.put("memoryLimit", problem.getMemoryLimit());
            response.put("visibility", problem.getVisibility());
            response.put("authorId", problem.getAuthorId());
            response.put("status", problem.getStatus());
            response.put("createdAt", problem.getCreatedAt());
            response.put("updatedAt", problem.getUpdatedAt());
            response.put("deleted", problem.getDeleted());
            response.put("tags", problem.getTags());
            response.put("testCases", testCases);
            response.put("solutionCode", solutionCodeMap);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "服务器错误: " + e.getMessage()));
        }
    }

    /** 获取做题视图的题目摘要信息（隐藏测试用例和官方题解代码） */
    @GetMapping("/{id}/view")
    public ResponseEntity<?> getProblemForSolving(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Problem problem = problemService.getProblemForSolving(id);
            if (problem == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "题目不存在"));
            }

            if ("PRIVATE".equals(problem.getVisibility())) {
                Long userId = resolveUserId(userDetails);
                boolean isAdmin = hasAdminRole(userDetails);
                boolean isAuthor = userId != null && userId.equals(problem.getAuthorId());
                boolean inContest = userId != null && contestService.isUserInRunningContestWithProblem(userId, id);
                if (!isAdmin && !isAuthor && !inContest) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "私有题目，请先加入相关竞赛"));
                }
            }

            Map<String, Object> response = problemService.getProblemSummaryForView(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "服务器错误: " + e.getMessage()));
        }
    }

    /** 创建新题目（含标签、测试用例、官方题解） */
    @PostMapping("")
    public ResponseEntity<?> createProblem(@RequestBody Map<String, Object> request,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);

            Problem problem = new Problem();
            problem.setTitle((String) request.get("title"));
            problem.setDescription((String) request.get("description"));
            problem.setDifficulty((String) request.get("difficulty"));
            problem.setInputFormat((String) request.get("inputFormat"));
            problem.setOutputFormat((String) request.get("outputFormat"));
            applyPublicExamples(problem, request);
            problem.setHint((String) request.get("hint"));
            problem.setSolution((String) request.get("solution"));

            Object timeLimit = request.get("timeLimit");
            if (timeLimit instanceof Integer) {
                problem.setTimeLimit((Integer) timeLimit);
            } else if (timeLimit instanceof String) {
                problem.setTimeLimit(Integer.parseInt((String) timeLimit));
            } else if (timeLimit instanceof Number) {
                problem.setTimeLimit(((Number) timeLimit).intValue());
            } else {
                problem.setTimeLimit(1000);
            }

            Object memoryLimit = request.get("memoryLimit");
            if (memoryLimit instanceof Integer) {
                problem.setMemoryLimit((Integer) memoryLimit);
            } else if (memoryLimit instanceof String) {
                problem.setMemoryLimit(Integer.parseInt((String) memoryLimit));
            } else if (memoryLimit instanceof Number) {
                problem.setMemoryLimit(((Number) memoryLimit).intValue());
            } else {
                problem.setMemoryLimit(262144);
            }

            Object visibilityObj = request.get("visibility");
            if (visibilityObj != null) {
                problem.setVisibility((String) visibilityObj);
            } else {
                Object isPublicObj = request.get("isPublic");
                if (isPublicObj instanceof Boolean) {
                    problem.setVisibility((Boolean) isPublicObj ? "PUBLIC" : "PRIVATE");
                } else {
                    problem.setVisibility("PRIVATE");
                }
            }

            problem.setAuthorId(userId != null ? userId : 1L);
            problem.setStatus(1);

            List<String> tagNames = null;
            Object tagsObj = request.get("tags");
            if (tagsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> tagList = (List<String>) tagsObj;
                tagNames = tagList;
            } else if (tagsObj instanceof String) {
                tagNames = List.of(((String) tagsObj).split(","));
            } else {
                tagNames = List.of();
            }

            List<Map<String, Object>> testCases = new ArrayList<>();
            Object testCasesObj = request.get("testCases");
            if (testCasesObj != null) {
                if (testCasesObj instanceof String) {
                    try {
                        testCases = JSON_MAPPER.readValue((String) testCasesObj, List.class);
                    } catch (Exception e) {
                        log.debug("解析测试用例JSON字符串失败: " + e.getMessage());
                    }
                } else if (testCasesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> testCasesList = (List<Map<String, Object>>) testCasesObj;
                    testCases = testCasesList;
                }
            }

            boolean created = problemService.createProblem(problem, tagNames);

            if (created) {
                if (!testCases.isEmpty()) {
                    try {
                        for (int i = 0; i < testCases.size(); i++) {
                            Map<String, Object> testCase = testCases.get(i);
                            JudgeTestCase judgeTestCase = new JudgeTestCase();
                            judgeTestCase.setProblemId(problem.getId());
                            judgeTestCase.setInput(testCase.get("input") == null ? "" : String.valueOf(testCase.get("input")));

                            String expectedOutput = testCase.get("expectedOutput") == null ? null : String.valueOf(testCase.get("expectedOutput"));
                            if (expectedOutput == null) {
                                expectedOutput = testCase.get("output") == null ? "" : String.valueOf(testCase.get("output"));
                            }
                            judgeTestCase.setExpectedOutput(expectedOutput);

                            Object scoreObj = testCase.get("score");
                            if (scoreObj instanceof Integer) {
                                judgeTestCase.setScore((Integer) scoreObj);
                            } else if (scoreObj instanceof Number) {
                                judgeTestCase.setScore(((Number) scoreObj).intValue());
                            } else if (scoreObj instanceof String) {
                                judgeTestCase.setScore(Integer.parseInt((String) scoreObj));
                            } else {
                                judgeTestCase.setScore(10);
                            }

                            judgeTestCase.setIsHidden(true);
                            judgeTestCase.setOrderNum(i);
                            testCaseMapper.insert(judgeTestCase);
                        }
                    } catch (Exception e) {
                        log.error("保存测试用例失败", e);
                    }
                }

                Object solutionCodeObj = request.get("solutionCode");
                if (solutionCodeObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> solutionCodeMap = (Map<String, String>) solutionCodeObj;
                    try {
                        problemSolutionService.createOfficialSolutions(problem.getId(), userId != null ? userId : 1L, solutionCodeMap);
                    } catch (Exception e) {
                        log.error("官方题解创建失败", e);
                    }
                } else if (solutionCodeObj instanceof String) {
                    try {
                        Map<String, String> solutionCodeMap = JSON_MAPPER.readValue((String) solutionCodeObj, Map.class);
                        problemSolutionService.createOfficialSolutions(problem.getId(), userId != null ? userId : 1L, solutionCodeMap);
                    } catch (Exception e) {
                        log.debug("解析题解代码JSON字符串失败: " + e.getMessage());
                    }
                }

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "题目创建成功");
                responseData.put("problemId", problem.getId());
                return ResponseEntity.ok(responseData);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "题目创建失败"));
            }
        } catch (Exception e) {
            log.error("题目创建异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "服务器错误: " + e.getMessage()));
        }
    }

    /** 更新题目信息（含标签、测试用例、官方题解） */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProblem(@PathVariable Long id, @RequestBody Map<String, Object> request,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);

            Problem existingProblem = problemService.getProblemById(id);
            if (existingProblem == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "问题不存在"));
            }

            if (!existingProblem.getAuthorId().equals(userId) && !hasAdminRole(userDetails)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "无权修改此问题"));
            }

            Problem problem = new Problem();
            problem.setId(id);
            problem.setTitle((String) request.get("title"));
            problem.setDescription((String) request.get("description"));
            problem.setDifficulty((String) request.get("difficulty"));
            problem.setInputFormat((String) request.get("inputFormat"));
            problem.setOutputFormat((String) request.get("outputFormat"));
            applyPublicExamples(problem, request);
            problem.setHint((String) request.get("hint"));
            problem.setSolution((String) request.get("solution"));

            Object timeLimit = request.get("timeLimit");
            if (timeLimit instanceof Integer) {
                problem.setTimeLimit((Integer) timeLimit);
            } else if (timeLimit instanceof String) {
                problem.setTimeLimit(Integer.parseInt((String) timeLimit));
            } else if (timeLimit instanceof Number) {
                problem.setTimeLimit(((Number) timeLimit).intValue());
            } else {
                problem.setTimeLimit(existingProblem.getTimeLimit());
            }

            Object memoryLimit = request.get("memoryLimit");
            if (memoryLimit instanceof Integer) {
                problem.setMemoryLimit((Integer) memoryLimit);
            } else if (memoryLimit instanceof String) {
                problem.setMemoryLimit(Integer.parseInt((String) memoryLimit));
            } else if (memoryLimit instanceof Number) {
                problem.setMemoryLimit(((Number) memoryLimit).intValue());
            } else {
                problem.setMemoryLimit(existingProblem.getMemoryLimit());
            }

            problem.setVisibility((String) request.get("visibility"));
            problem.setAuthorId(userId);
            problem.setStatus(existingProblem.getStatus());

            List<String> tagNames = null;
            Object tagsObj = request.get("tags");
            if (tagsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> tagList = (List<String>) tagsObj;
                tagNames = tagList;
            } else if (tagsObj instanceof String) {
                tagNames = List.of(((String) tagsObj).split(","));
            } else {
                tagNames = List.of();
            }

            boolean updated = problemService.updateProblem(problem, tagNames);

            if (updated) {
                List<Map<String, Object>> testCases = new ArrayList<>();
                Object testCasesObj = request.get("testCases");

                if (testCasesObj != null) {
                    if (testCasesObj instanceof String) {
                        try {
                            testCases = JSON_MAPPER.readValue((String) testCasesObj, List.class);
                        } catch (Exception e) {
                            log.debug("解析测试用例JSON字符串失败: " + e.getMessage());
                        }
                    } else if (testCasesObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> testCasesList = (List<Map<String, Object>>) testCasesObj;
                        testCases = testCasesList;
                    }

                    if (!testCases.isEmpty()) {
                        testCaseMapper.deleteByProblemId(id);

                        for (int i = 0; i < testCases.size(); i++) {
                            Map<String, Object> testCase = testCases.get(i);
                            JudgeTestCase judgeTestCase = new JudgeTestCase();
                            judgeTestCase.setProblemId(id);
                            judgeTestCase.setInput(testCase.get("input") == null ? "" : String.valueOf(testCase.get("input")));

                            String expectedOutput = testCase.get("expectedOutput") == null ? null : String.valueOf(testCase.get("expectedOutput"));
                            if (expectedOutput == null) {
                                expectedOutput = testCase.get("output") == null ? "" : String.valueOf(testCase.get("output"));
                            }
                            judgeTestCase.setExpectedOutput(expectedOutput);

                            Object scoreObj = testCase.get("score");
                            if (scoreObj instanceof Integer) {
                                judgeTestCase.setScore((Integer) scoreObj);
                            } else if (scoreObj instanceof Number) {
                                judgeTestCase.setScore(((Number) scoreObj).intValue());
                            } else if (scoreObj instanceof String) {
                                judgeTestCase.setScore(Integer.parseInt((String) scoreObj));
                            } else {
                                judgeTestCase.setScore(10);
                            }

                            judgeTestCase.setIsHidden(true);
                            judgeTestCase.setOrderNum(i);
                            testCaseMapper.insert(judgeTestCase);
                        }
                    }
                }

                Object solutionCodeObj = request.get("solutionCode");
                if (solutionCodeObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> solutionCodeMap = (Map<String, String>) solutionCodeObj;
                    problemSolutionService.createOfficialSolutions(problem.getId(), userId, solutionCodeMap);
                } else if (solutionCodeObj instanceof String) {
                    try {
                        Map<String, String> solutionCodeMap = JSON_MAPPER.readValue((String) solutionCodeObj, Map.class);
                        problemSolutionService.createOfficialSolutions(problem.getId(), userId, solutionCodeMap);
                    } catch (Exception e) {
                        log.debug("解析题解代码JSON字符串失败: " + e.getMessage());
                    }
                }

                return ResponseEntity.ok(Map.of("success", true, "message", "题目更新成功"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "题目更新失败"));
            }
        } catch (Exception e) {
            log.error("题目更新异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "服务器错误: " + e.getMessage()));
        }
    }

    /** 删除题目（需作者或管理员权限） */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProblem(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);

            Problem existingProblem = problemService.getProblemById(id);
            if (existingProblem == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "问题不存在"));
            }

            if (!existingProblem.getAuthorId().equals(userId) && !hasAdminRole(userDetails)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "无权删除此问题"));
            }

            boolean deleted = problemService.deleteProblem(id);

            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true, "message", "题目删除成功"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "题目删除失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "服务器错误: " + e.getMessage()));
        }
    }

    /** 获取指定作者的题目列表 */
    @GetMapping("/author/{authorId}")
    public ResponseEntity<?> getProblemsByAuthor(@PathVariable Long authorId,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails != null) {
                Long currentUserId = resolveUserId(userDetails);
                if (currentUserId != null && (currentUserId.equals(authorId) || hasAdminRole(userDetails))) {
                    List<Problem> problems = problemService.getProblemsByAuthor(authorId);
                    return ResponseEntity.ok(problems);
                }
            }
            return ResponseEntity.ok(problemService.getPublicProblemsByAuthor(authorId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "服务器错误: " + e.getMessage()));
        }
    }

    /** 更新题目可见性（PUBLIC/PRIVATE） */
    @PutMapping("/{id}/visibility")
    public ResponseEntity<?> updateProblemVisibility(@PathVariable Long id, @RequestBody Map<String, String> request,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);

            Problem existingProblem = problemService.getProblemById(id);
            if (existingProblem == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "问题不存在"));
            }

            if (!existingProblem.getAuthorId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "无权修改此问题"));
            }

            String visibility = request.get("visibility");
            if (visibility == null || (!visibility.equals("PUBLIC") && !visibility.equals("PRIVATE"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "可见性参数无效，应为PUBLIC或PRIVATE"));
            }
            boolean updated = problemService.updateProblemVisibility(id, visibility);

            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "message", "题目可见性更新成功"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "题目可见性更新失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "服务器错误: " + e.getMessage()));
        }
    }

    /** 按标签名查询题目列表 */
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<?> getProblemsByTag(@PathVariable String tagName) {
        try {
            List<Problem> problems = problemService.getProblemsByTag(tagName);
            return ResponseEntity.ok(problems);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "服务器错误: " + e.getMessage()));
        }
    }
}

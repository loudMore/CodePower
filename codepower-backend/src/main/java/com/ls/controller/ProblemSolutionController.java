/**
 * 文件说明：题解 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ls.domain.Problem;
import com.ls.domain.ProblemSolution;
import com.ls.domain.Submission;
import com.ls.domain.User;
import com.ls.mapper.ProblemMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.mapper.UserMapper;
import com.ls.service.ProblemSolutionService;
import com.ls.service.UserLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 题解控制器 — 题解增删改查、官方题解、社区题解 */
@RestController
@RequestMapping("/api/solutions")
public class ProblemSolutionController {

    @Autowired
    private ProblemSolutionService problemSolutionService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private UserLevelService userLevelService;

    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        User user = userMapper.findByUsername(userDetails.getUsername());
        return user != null ? user.getId() : null;
    }

    private boolean hasAdminRole(UserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ADMIN".equals(authority.getAuthority()));
    }

    private ResponseEntity<?> validateProblemSolutionAccess(Long problemId, UserDetails userDetails) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null || Objects.equals(problem.getDeleted(), 1)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "题目不存在"));
        }
        if (!"PRIVATE".equals(problem.getVisibility())) {
            return null;
        }
        Long userId = resolveUserId(userDetails);
        boolean canManage = userId != null && (Objects.equals(problem.getAuthorId(), userId) || hasAdminRole(userDetails));
        if (!canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "私有题目的题解仅作者或管理员可查看"));
        }
        return null;
    }

    /** 获取指定题目的所有题解 */
    @GetMapping("/problem/{problemId}")
    public ResponseEntity<?> getSolutionsByProblemId(@PathVariable Long problemId,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ResponseEntity<?> blocked = validateProblemSolutionAccess(problemId, userDetails);
            if (blocked != null) return blocked;
            List<ProblemSolution> solutions = problemSolutionService.getSolutionsByProblemId(problemId);
            return ResponseEntity.ok(solutions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取题解失败: " + e.getMessage()));
        }
    }

    /** 获取指定题目的官方题解 */
    @GetMapping("/problem/{problemId}/official")
    public ResponseEntity<?> getOfficialSolutionsByProblemId(@PathVariable Long problemId,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ResponseEntity<?> blocked = validateProblemSolutionAccess(problemId, userDetails);
            if (blocked != null) return blocked;
            List<ProblemSolution> solutions = problemSolutionService.getOfficialSolutionsByProblemId(problemId);
            return ResponseEntity.ok(solutions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取官方题解失败: " + e.getMessage()));
        }
    }

    /** 按语言筛选指定题目的题解 */
    @GetMapping("/problem/{problemId}/language/{language}")
    public ResponseEntity<?> getSolutionsByProblemIdAndLanguage(
            @PathVariable Long problemId,
            @PathVariable String language,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ResponseEntity<?> blocked = validateProblemSolutionAccess(problemId, userDetails);
            if (blocked != null) return blocked;
            List<ProblemSolution> solutions = problemSolutionService.getSolutionsByProblemIdAndLanguage(problemId, language);
            return ResponseEntity.ok(solutions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取题解失败: " + e.getMessage()));
        }
    }

    /** 获取指定用户的所有题解 */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getSolutionsByUserId(@PathVariable Long userId) {
        try {
            List<ProblemSolution> solutions = problemSolutionService.getSolutionsByUserId(userId);
            return ResponseEntity.ok(solutions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取用户题解失败: " + e.getMessage()));
        }
    }

    /** 创建社区题解 */
    @PostMapping("")
    public ResponseEntity<?> createSolution(
            @RequestBody ProblemSolution solution,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "请先登录后再分享题解"));
            }
            solution.setUserId(userId);
            solution.setIsOfficial(0);
            solution.setStatus(1);

            String validationError = validateCommunitySolution(solution);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", validationError));
            }
            ResponseEntity<?> blocked = validateProblemSolutionAccess(solution.getProblemId(), userDetails);
            if (blocked != null) return blocked;

            Submission sourceSubmission = resolveAcceptedSourceSubmission(userId, solution.getProblemId(),
                    solution.getSourceSubmissionId());
            if (sourceSubmission == null || sourceSubmission.getCode() == null || sourceSubmission.getCode().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "请先提交并通过本题，再使用该次 AC 提交发布题解"));
            }
            // 题解代码只信任服务端保存的 AC 提交，不接受前端传入的任意代码，避免错误代码污染题解库。
            solution.setSourceSubmissionId(sourceSubmission.getId());
            solution.setCode(sourceSubmission.getCode());
            solution.setLanguage(sourceSubmission.getLanguage());
            solution.setRuntimeMs(sourceSubmission.getExecutionTime());
            solution.setMemoryKb(sourceSubmission.getMemoryUsed());

            RewardDecision rewardDecision = evaluateRewardEligibility(userId, solution.getProblemId(),
                    sourceSubmission, solution.getDescription());
            solution.setRewardGranted(rewardDecision.eligible() ? 1 : 0);
            solution.setRewardReason(rewardDecision.reasonCode());
            boolean success = problemSolutionService.createSolution(solution);

            if (success) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("message", "题解创建成功");
                response.put("id", solution.getId());
                response.put("rewardEligible", rewardDecision.eligible());
                if (rewardDecision.eligible()) {
                    int expReward = 30;
                    int aiReward = 3;
                    userLevelService.addExp(userId, expReward);
                    userLevelService.addAiBonus(userId, aiReward);
                    response.put("expReward", expReward);
                    response.put("aiReward", aiReward);
                    response.put("rewardReason", rewardDecision.reasonCode());
                    response.put("rewardMessage", rewardDecision.message() + "，获得 " + expReward + " 经验 + " + aiReward + " AI积分");
                }
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "题解创建失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "题解创建失败: " + e.getMessage()));
        }
    }

    private String validateCommunitySolution(ProblemSolution solution) {
        if (solution.getProblemId() == null) {
            return "缺少题目信息";
        }
        if (solution.getTitle() == null || solution.getTitle().isBlank()) {
            return "请填写题解标题";
        }
        return null;
    }

    private Submission resolveAcceptedSourceSubmission(Long userId, Long problemId, Long sourceSubmissionId) {
        if (userId == null || problemId == null || sourceSubmissionId == null) {
            return null;
        }
        Submission submission = submissionMapper.selectById(sourceSubmissionId);
        if (submission == null) {
            return null;
        }
        if (!Objects.equals(submission.getUserId(), userId)
                || !Objects.equals(submission.getProblemId(), problemId)
                || !"ACCEPTED".equals(submission.getStatus())
                || submission.getContestId() != null) {
            return null;
        }
        return submission;
    }

    private RewardDecision evaluateRewardEligibility(Long userId, Long problemId, Submission sourceSubmission,
                                                     String description) {
        if (!hasComparableSubmissionMetric(sourceSubmission)) {
            return new RewardDecision(false, null, "发布成功");
        }
        if (!hasMeaningfulExplanation(description)) {
            return new RewardDecision(false, null, "题解已发布；补充清晰思路后更容易获得质量奖励");
        }

        List<ProblemSolution> communitySolutions = problemSolutionService.getCommunitySolutionsByProblemId(problemId);
        boolean firstCommunitySolution = communitySolutions.isEmpty();
        if (firstCommunitySolution) {
            return new RewardDecision(true, "FIRST_COMMUNITY", "本题首篇社区题解发布成功");
        }

        boolean userAlreadyPublished = communitySolutions.stream()
                .anyMatch(solution -> Objects.equals(solution.getUserId(), userId));
        if (userAlreadyPublished) {
            return new RewardDecision(false, null, "你已经发布过本题题解，本次不重复发放奖励");
        }

        ProblemSolution currentBest = communitySolutions.stream()
                .filter(this::hasComparableSolutionMetric)
                .min(this::compareSolutionQuality)
                .orElse(null);
        if (currentBest != null && isSubmissionBetterThanSolution(sourceSubmission, currentBest)) {
            return new RewardDecision(true, "NEW_BEST", "题解性能基准已刷新");
        }
        return new RewardDecision(false, null, "发布成功");
    }

    private boolean hasComparableSubmissionMetric(Submission submission) {
        return submission != null
                && submission.getExecutionTime() != null && submission.getExecutionTime() > 0
                && submission.getMemoryUsed() != null && submission.getMemoryUsed() > 0;
    }

    private boolean hasComparableSolutionMetric(ProblemSolution solution) {
        return solution != null
                && solution.getRuntimeMs() != null && solution.getRuntimeMs() > 0
                && solution.getMemoryKb() != null && solution.getMemoryKb() > 0;
    }

    private int compareSolutionQuality(ProblemSolution left, ProblemSolution right) {
        int runtimeCompare = Integer.compare(nullableMetric(left.getRuntimeMs()), nullableMetric(right.getRuntimeMs()));
        if (runtimeCompare != 0) {
            return runtimeCompare;
        }
        return Integer.compare(nullableMetric(left.getMemoryKb()), nullableMetric(right.getMemoryKb()));
    }

    private boolean isSubmissionBetterThanSolution(Submission submission, ProblemSolution benchmark) {
        if (!hasComparableSubmissionMetric(submission) || !hasComparableSolutionMetric(benchmark)) {
            return false;
        }
        int runtimeCompare = Integer.compare(submission.getExecutionTime(), benchmark.getRuntimeMs());
        if (runtimeCompare != 0) {
            return runtimeCompare < 0;
        }
        return submission.getMemoryUsed() < benchmark.getMemoryKb();
    }

    private int nullableMetric(Integer value) {
        return value == null || value <= 0 ? Integer.MAX_VALUE : value;
    }

    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasMeaningfulExplanation(String html) {
        String plainText = stripHtml(html);
        if (plainText.length() < 12) {
            return false;
        }
        return !plainText.contains("先用一两句话说明") && !plainText.contains("时间复杂度：O()");
    }

    private record RewardDecision(boolean eligible, String reasonCode, String message) {}

    /** 更新题解（需作者权限） */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSolution(
            @PathVariable Long id,
            @RequestBody ProblemSolution solution,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);

            ProblemSolution existingSolution = problemSolutionService.getById(id);
            if (existingSolution == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "题解不存在"));
            }

            if (!existingSolution.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "无权修改此题解"));
            }

            solution.setId(id);
            solution.setUserId(userId);
            solution.setIsOfficial(existingSolution.getIsOfficial());
            // 更新题解时允许改标题和说明，但代码仍固定为已验证 AC 来源，避免发布后改成错误代码。
            solution.setProblemId(existingSolution.getProblemId());
            solution.setCode(existingSolution.getCode());
            solution.setLanguage(existingSolution.getLanguage());
            solution.setSourceSubmissionId(existingSolution.getSourceSubmissionId());
            solution.setRuntimeMs(existingSolution.getRuntimeMs());
            solution.setMemoryKb(existingSolution.getMemoryKb());
            solution.setRewardGranted(existingSolution.getRewardGranted());
            solution.setRewardReason(existingSolution.getRewardReason());
            solution.setStatus(existingSolution.getStatus());

            boolean success = problemSolutionService.updateSolution(solution);

            if (success) {
                return ResponseEntity.ok(Map.of("message", "题解更新成功"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "题解更新失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "题解更新失败: " + e.getMessage()));
        }
    }

    /** 删除题解（需作者权限） */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSolution(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = resolveUserId(userDetails);

            ProblemSolution existingSolution = problemSolutionService.getById(id);
            if (existingSolution == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "题解不存在"));
            }

            if (!existingSolution.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "无权删除此题解"));
            }

            boolean success = problemSolutionService.deleteSolution(id);

            if (success) {
                return ResponseEntity.ok(Map.of("message", "题解删除成功"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "题解删除失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "题解删除失败: " + e.getMessage()));
        }
    }

    /** 获取分类题解（官方+社区） */
    @GetMapping("/problem/{problemId}/categorized")
    public ResponseEntity<?> getCategorizedSolutions(@PathVariable Long problemId,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ResponseEntity<?> blocked = validateProblemSolutionAccess(problemId, userDetails);
            if (blocked != null) return blocked;
            List<ProblemSolution> officialSolutions = problemSolutionService.getOfficialSolutionsByProblemId(problemId);
            List<ProblemSolution> communitySolutions = problemSolutionService.getCommunitySolutionsByProblemId(problemId);

            Map<String, Object> result = new HashMap<>();
            result.put("official", officialSolutions);
            result.put("community", communitySolutions);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取题解失败: " + e.getMessage()));
        }
    }

    /** 获取官方题解代码（按语言分组） */
    @GetMapping("/problem/{problemId}/official-code")
    public ResponseEntity<?> getOfficialSolutionCode(@PathVariable Long problemId,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ResponseEntity<?> blocked = validateProblemSolutionAccess(problemId, userDetails);
            if (blocked != null) return blocked;
            Map<String, String> officialSolutionCode = problemSolutionService.getOfficialSolutionCodeByProblemId(problemId);
            return ResponseEntity.ok(officialSolutionCode);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取官方题解代码失败: " + e.getMessage()));
        }
    }
}

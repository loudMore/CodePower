/**
 * 文件说明：用户标签能力 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ls.domain.*;
import com.ls.mapper.*;
import com.ls.service.CheckInService;
import com.ls.service.DailyTaskService;
import com.ls.service.RedisCacheService;
import com.ls.service.UserAbilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户能力画像与个性化推荐服务。
 *
 * 核心算法三要素：
 *   1. Logistic 饱和曲线 (logisticProgress)：将不同量纲的指标压到 0-100，越往上越难涨。
 *   2. 时间衰减 (timeDecayWeight)：70 天半衰期，越近的提交权重越高。
 *   3. 非线性融合 (combineEvidence)：4 次方幂平均，高质量证据权重远大于低质量证据。
 *
 * 八维雷达图：知识覆盖、掌握深度、难度进阶、解题稳定、运行效率、空间效率、成长潜力、学习习惯。
 * 个性化推荐：根据弱项标签 + 目标难度匹配题目，缓存 2h，AC 后失效。
 *
 * 数据来源：仅统计日常训练提交（contestId IS NULL），竞赛提交不计入画像。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAbilityServiceImpl extends ServiceImpl<UserAbilityMapper, UserAbility> implements UserAbilityService {

    private static final int RECOMMENDATION_SCAN_LIMIT = 24;
    private static final double NONLINEAR_SCALE = 4.0;
    private static final double TAG_SCORE_SIGMA = 20.0;
    private static final Map<String, Integer> DIFFICULTY_ORDER = Map.of(
            "EASY", 1,
            "简单", 1,
            "MEDIUM", 2,
            "普通", 2,
            "HARD", 3,
            "困难", 3,
            "EXTREME", 4,
            "极限", 4
    );
    private static final Map<Integer, String> DIFFICULTY_LABEL = Map.of(
            1, "简单",
            2, "普通",
            3, "困难",
            4, "极限"
    );

    private static final String RECOMMEND_CACHE_PREFIX = "recommend:user:";
    private static final long RECOMMEND_CACHE_TTL_HOURS = 2;

    private final TagMapper tagMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final UserCheckInMapper userCheckInMapper;
    private final DailyTaskService dailyTaskService;
    private final CheckInService checkInService;
    private final RedisCacheService redisCacheService;

    /** 查询用户在各题目标签上的能力分，并补充标签名称。 */
    @Override
    public List<UserAbility> getUserAbilities(Long userId) {
        List<UserAbility> abilities = this.list(
                new LambdaQueryWrapper<UserAbility>().eq(UserAbility::getUserId, userId));
        if (!abilities.isEmpty()) {
            List<Long> tagIds = abilities.stream().map(UserAbility::getTagId).collect(Collectors.toList());
            Map<Long, Tag> tagMap = tagMapper.selectBatchIds(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, t -> t));
            abilities.forEach(a -> {
                Tag tag = tagMap.get(a.getTagId());
                if (tag != null) a.setTagName(tag.getName());
            });
        }
        return abilities;
    }

    /** 返回前端 ECharts 雷达图需要的指标和值。 */
    @Override
    public Map<String, Object> getRadarChartData(Long userId) {
        Map<String, Object> profile = getAbilityProfile(userId);
        return Map.of(
                "indicator", profile.getOrDefault("indicator", List.of()),
                "value", profile.getOrDefault("value", List.of()),
                "benchmarkValue", profile.getOrDefault("benchmarkValue", List.of()),
                "analysisBasis", profile.getOrDefault("analysisBasis", "基于日常训练数据综合评估")
        );
    }

    /**
     * 生成完整能力画像。
     * 这里会把日常提交记录拆成八个维度：覆盖、深度、难度、稳定性、运行效率、空间效率、潜力和习惯。
     */
    @Override
    public Map<String, Object> getAbilityProfile(Long userId) {
        List<UserAbility> abilities = getUserAbilities(userId);
        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .isNull(Submission::getContestId)
                        .orderByDesc(Submission::getCreatedAt));

        // 画像只看日常训练数据：同一道题的多次提交归到一起，用来判断“这题尝试了几次、是否最终通过”。
        Map<Long, List<Submission>> submissionsByProblem = submissions.stream()
                .filter(item -> item.getProblemId() != null)
                .collect(Collectors.groupingBy(Submission::getProblemId, LinkedHashMap::new, Collectors.toList()));

        // 每道题只取最近一次 AC 作为掌握证据，避免一个用户反复 AC 同一题把能力刷高。
        Map<Long, Submission> latestAcceptedByProblem = new LinkedHashMap<>();
        for (Submission submission : submissions) {
            if (!"ACCEPTED".equals(submission.getStatus()) || submission.getProblemId() == null) {
                continue;
            }
            latestAcceptedByProblem.putIfAbsent(submission.getProblemId(), submission);
        }

        List<Submission> solvedSnapshots = new ArrayList<>(latestAcceptedByProblem.values());
        Set<Long> solvedProblemIds = new LinkedHashSet<>(latestAcceptedByProblem.keySet());
        Set<Long> attemptedProblemIds = submissions.stream()
                .map(Submission::getProblemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int totalAttempts = submissions.size();
        int attemptedProblemCount = attemptedProblemIds.size();
        int solvedCount = solvedProblemIds.size();
        double acceptanceRate = attemptedProblemCount == 0 ? 0D : solvedCount * 100.0 / attemptedProblemCount;
        double avgAttemptsPerSolved = solvedCount == 0 ? 0D : solvedProblemIds.stream()
                .mapToInt(problemId -> submissionsByProblem.getOrDefault(problemId, List.of()).size())
                .average()
                .orElse(0D);

        Map<Long, Problem> solvedProblemMap = loadProblemMap(solvedProblemIds);
        Map<Long, List<Submission>> acceptedCohortByProblem = loadAcceptedCohortByProblem(solvedProblemIds);

        int solvedDifficultySum = solvedProblemMap.values().stream()
                .mapToInt(problem -> difficultyOrder(problem.getDifficulty()))
                .sum();
        double solvedDifficultyAvg = solvedCount == 0 ? 0D : (double) solvedDifficultySum / solvedCount;
        int maxSolvedDifficulty = solvedProblemMap.values().stream()
                .mapToInt(problem -> difficultyOrder(problem.getDifficulty()))
                .max()
                .orElse(0);

        List<Double> runtimePercentiles = solvedSnapshots.stream()
                .map(snapshot -> percentileAgainstAcceptedCohort(
                        snapshot.getExecutionTime(),
                        acceptedCohortByProblem.get(snapshot.getProblemId()),
                        true))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Double> memoryPercentiles = solvedSnapshots.stream()
                .map(snapshot -> percentileAgainstAcceptedCohort(
                        snapshot.getMemoryUsed(),
                        acceptedCohortByProblem.get(snapshot.getProblemId()),
                        false))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        double runtimePercentileAverage = average(runtimePercentiles);
        double memoryPercentileAverage = average(memoryPercentiles);
        int runtimeScore = runtimePercentiles.isEmpty() ? 0 : boundedScore(runtimePercentileAverage);
        int memoryScore = memoryPercentiles.isEmpty() ? 0 : boundedScore(memoryPercentileAverage);

        Set<Long> coveredTagIds = abilities.stream()
                .filter(item -> item.getSolvedCount() != null && item.getSolvedCount() > 0)
                .map(UserAbility::getTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int totalTags = Math.max(tagMapper.selectCount(null).intValue(), 1);

        List<Integer> tagScores = abilities.stream()
                .map(UserAbility::getAbilityScore)
                .filter(Objects::nonNull)
                .map(BigDecimal::doubleValue)
                .map(this::boundedScore)
                .collect(Collectors.toList());
        double tagAbilityAverage = tagScores.stream().mapToInt(Integer::intValue).average().orElse(0D);
        double tagAbilityVariance = tagScores.stream()
                .mapToDouble(score -> Math.pow(score - tagAbilityAverage, 2))
                .average()
                .orElse(0D);

        // 八维雷达图的分数全部压到 0-100：先标准化，再按权重融合，前端只负责展示。
        int coverageScore = boundedScore(logisticProgress((double) coveredTagIds.size() / totalTags, 0.38, 10.0));
        int difficultyMasteryScore = boundedScore(combineEvidence(
                logisticProgress(solvedDifficultyAvg / 4.0, 0.58, 8.0), 0.7,
                logisticProgress(maxSolvedDifficulty / 4.0, 0.72, 7.0), 0.3
        ));
        int proficiencyScore = boundedScore(combineEvidence(
                logisticProgress(tagAbilityAverage / 100.0, 0.56, 8.5), 0.62,
                logisticProgress(acceptanceRate / 100.0, 0.52, 7.0), 0.38
        ));
        int stabilityScore = boundedScore(combineEvidence(
                logisticProgress(acceptanceRate / 100.0, 0.5, 8.0), 0.6,
                inverseLogisticPenalty(Math.max(1D, avgAttemptsPerSolved), 1.7, 3.2), 0.4
        ));
        int growthPotentialScore = boundedScore(combineEvidence(
                inverseLogisticPenalty(tagAbilityVariance, 180.0, 0.03), 0.45,
                inverseLogisticPenalty(tagAbilityAverage, 72.0, 0.08), 0.35,
                logisticProgress((double) Math.min(solvedCount, 80) / 80.0, 0.26, 8.0), 0.2
        ));
        int habitScore = calculateHabitScore(userId);

        List<Map<String, Object>> indicators = List.of(
                indicator("知识覆盖", "按已形成有效解题记录的标签覆盖率评估知识广度"),
                indicator("掌握深度", "按标签能力均值与通过质量评估专题掌握程度"),
                indicator("难度进阶", "按已攻克题目的平均难度与最高难度评估进阶水平"),
                indicator("解题稳定", "结合通过率与单题平均尝试次数评估稳定性"),
                indicator("运行效率", "按同题 Accepted 队列中的运行耗时百分位计算"),
                indicator("空间效率", "按同题 Accepted 队列中的内存占用百分位计算"),
                indicator("成长潜力", "结合能力离散度、当前均值与训练量评估提升空间"),
                indicator("学习习惯", "基于签到连续性与每日任务完成度评估训练节奏")
        );

        List<Integer> radarValues = List.of(
                coverageScore,
                proficiencyScore,
                difficultyMasteryScore,
                stabilityScore,
                runtimeScore,
                memoryScore,
                growthPotentialScore,
                habitScore
        );
        List<Integer> benchmarkValues = List.of(75, 72, 70, 74, 68, 68, 65, 78);

        List<Map<String, Object>> strengths = new ArrayList<>();
        List<Map<String, Object>> weaknesses = new ArrayList<>();
        appendDimensionSummary(strengths, weaknesses, "知识覆盖", coverageScore,
                coverageScore >= 70 ? "已形成较广的专题覆盖，具备跨标签迁移基础" : "当前覆盖面还偏窄，建议先补齐薄弱专题",
                coveredTagIds.size() + " / " + totalTags + " 个标签已形成有效记录");
        appendDimensionSummary(strengths, weaknesses, "掌握深度", proficiencyScore,
                proficiencyScore >= 70 ? "核心标签的解题质量已经比较扎实" : "标签平均能力仍在爬坡，建议围绕高频弱项持续训练",
                abilities.isEmpty() ? "暂无标签能力沉淀" : "标签能力均值 " + rounded(tagAbilityAverage));
        appendDimensionSummary(strengths, weaknesses, "难度进阶", difficultyMasteryScore,
                difficultyMasteryScore >= 70 ? "已经能稳定触达更高难度层级" : "高难度突破还不够，适合继续阶梯式进阶",
                solvedCount == 0 ? "暂无日常通过题目" : "平均难度 " + rounded(solvedDifficultyAvg) + "，最高通过难度：" + DIFFICULTY_LABEL.getOrDefault(maxSolvedDifficulty, "未分级"));
        appendDimensionSummary(strengths, weaknesses, "解题稳定", stabilityScore,
                stabilityScore >= 70 ? "做题命中率和首轮表现比较稳定" : "重复试错偏多，需要加强审题和调试节奏",
                attemptedProblemCount == 0 ? "暂无日常提交记录" : "有效题目通过率 " + formatPercent(acceptanceRate) + "，单题平均尝试 " + rounded(avgAttemptsPerSolved) + " 次");
        appendDimensionSummary(strengths, weaknesses, "运行效率", runtimeScore,
                runtimeScore >= 70 ? "已通过题目的运行耗时表现较好" : "运行耗时还有优化空间，可加强复杂度与常数控制",
                buildPercentileEvidence(runtimePercentiles, "运行耗时"));
        appendDimensionSummary(strengths, weaknesses, "空间效率", memoryScore,
                memoryScore >= 70 ? "已通过题目的内存表现较好" : "内存占用还有优化空间，可关注数据结构与拷贝开销",
                buildPercentileEvidence(memoryPercentiles, "内存占用"));
        appendDimensionSummary(strengths, weaknesses, "成长潜力", growthPotentialScore,
                growthPotentialScore >= 70 ? "仍有较大的结构性提升空间，适合做针对性补强" : "当前能力结构较均衡，后续更适合做高质量巩固",
                abilities.isEmpty() ? "暂无足够标签能力数据" : "标签能力离散度 σ≈" + rounded(Math.sqrt(tagAbilityVariance)));
        appendDimensionSummary(strengths, weaknesses, "学习习惯", habitScore,
                habitScore >= 70 ? "训练节奏较稳定，具备持续积累趋势" : "训练连续性一般，建议先把节奏稳定下来",
                buildHabitEvidence(userId));

        List<UserAbility> sortedAbilities = abilities.stream()
                .sorted(Comparator.comparing(UserAbility::getAbilityScore, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                .limit(6)
                .collect(Collectors.toList());

        List<Map<String, Object>> tagSnapshot = sortedAbilities.stream()
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("tagId", item.getTagId());
                    map.put("tagName", item.getTagName() != null ? item.getTagName() : "未知标签");
                    map.put("score", item.getAbilityScore() == null ? 0 : item.getAbilityScore().setScale(0, RoundingMode.HALF_UP).intValue());
                    map.put("solvedCount", item.getSolvedCount() == null ? 0 : item.getSolvedCount());
                    map.put("attemptCount", item.getAttemptCount() == null ? 0 : item.getAttemptCount());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("solvedCount", solvedCount);
        overview.put("totalAttempts", totalAttempts);
        overview.put("attemptedProblemCount", attemptedProblemCount);
        overview.put("acceptanceRate", rounded(acceptanceRate));
        overview.put("coveredTagCount", coveredTagIds.size());
        overview.put("averageSolvedDifficulty", rounded(solvedDifficultyAvg));
        overview.put("highestSolvedDifficulty", DIFFICULTY_LABEL.getOrDefault(maxSolvedDifficulty, solvedCount > 0 ? "未分级" : "暂无"));
        overview.put("averageRuntimePercentile", rounded(runtimePercentileAverage));
        overview.put("averageMemoryPercentile", rounded(memoryPercentileAverage));
        overview.put("averageTagScore", rounded(tagAbilityAverage));
        overview.put("tagScoreStdDev", rounded(Math.sqrt(tagAbilityVariance)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("indicator", indicators.stream().map(item -> Map.of("name", item.get("name"), "max", 100)).collect(Collectors.toList()));
        result.put("value", radarValues);
        result.put("benchmarkValue", benchmarkValues);
        result.put("dimensions", indicators.stream().map(item -> item.get("name")).collect(Collectors.toList()));
        result.put("scores", Map.of(
                "knowledgeCoverage", coverageScore,
                "proficiency", proficiencyScore,
                "difficultyMastery", difficultyMasteryScore,
                "stability", stabilityScore,
                "runtimeEfficiency", runtimeScore,
                "memoryEfficiency", memoryScore,
                "growthPotential", growthPotentialScore,
                "habit", habitScore
        ));
        result.put("overview", overview);
        result.put("strengths", strengths);
        result.put("weaknesses", weaknesses);
        result.put("tagSnapshot", tagSnapshot);
        result.put("analysisBasis", "仅统计日常训练数据；通过题数和尝试题数按真实题目掌握情况计算，反复提交主要反映稳定性和调试节奏；主画像采用非线性饱和评分，将标签能力、通过质量、难度进阶、稳定性与训练节奏统一到 0-100 区间；运行效率与空间效率按同题历史通过记录做百分位换算，避免不同题目时空要求直接混算");
        return result;
    }

    /**
     * 个性化推荐入口。
     * 先读 Redis 缓存；未命中时根据弱项标签、目标难度、题目通过率和尝试历史重新计算推荐。
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPersonalizedRecommendations(Long userId, int size) {
        int safeSize = Math.max(1, Math.min(size, 20));

        // 优先读取缓存
        String cacheKey = RECOMMEND_CACHE_PREFIX + userId;
        Object cached = redisCacheService.get(cacheKey);
        if (cached instanceof List<?> cachedList && !cachedList.isEmpty()) {
            List<Map<String, Object>> cachedResult = (List<Map<String, Object>>) cachedList;
            return cachedResult.size() > safeSize ? cachedResult.subList(0, safeSize) : cachedResult;
        }

        Map<String, Object> profile = getAbilityProfile(userId);
        List<UserAbility> abilities = getUserAbilities(userId);
        Map<Long, UserAbility> abilityByTagId = abilities.stream()
                .filter(item -> item.getTagId() != null)
                .collect(Collectors.toMap(UserAbility::getTagId, item -> item, (left, right) -> left));

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .select(Submission::getProblemId, Submission::getStatus, Submission::getCreatedAt)
                        .eq(Submission::getUserId, userId)
                        .isNull(Submission::getContestId));

        Set<Long> solvedIds = submissions.stream()
                .filter(item -> "ACCEPTED".equals(item.getStatus()))
                .map(Submission::getProblemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> attemptedIds = submissions.stream()
                .map(Submission::getProblemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 按标签记录最近提交时间，用于间隔复习判断
        Map<Long, LocalDateTime> latestSubmissionByProblem = new HashMap<>();
        for (Submission s : submissions) {
            if (s.getProblemId() != null && s.getCreatedAt() != null) {
                latestSubmissionByProblem.merge(s.getProblemId(), s.getCreatedAt(),
                        (a, b) -> a.isAfter(b) ? a : b);
            }
        }

        List<UserAbility> sortedWeakAbilities = abilities.stream()
                .filter(a -> a.getTagId() != null)
                .sorted(Comparator.comparing(UserAbility::getAbilityScore, Comparator.nullsLast(BigDecimal::compareTo)))
                .collect(Collectors.toList());

        List<Map<String, Object>> recommended = new ArrayList<>();
        Set<Long> addedIds = new HashSet<>();
        int targetDifficulty = recommendedDifficultyLevel(profile);

        // === 改进1：轮询式多标签推荐，保证多样性 ===
        int maxWeakTags = Math.min(5, sortedWeakAbilities.size());
        int perTagQuota = Math.max(2, (int) Math.ceil((double) safeSize / Math.max(1, maxWeakTags)));
        for (int round = 0; round < perTagQuota && recommended.size() < safeSize; round++) {
            for (int t = 0; t < maxWeakTags && recommended.size() < safeSize; t++) {
                UserAbility ability = sortedWeakAbilities.get(t);
                List<Long> problemIds = problemMapper.selectProblemIdsByTagId(ability.getTagId());
                int scanned = 0;
                for (Long problemId : problemIds) {
                    if (recommended.size() >= safeSize || scanned >= RECOMMENDATION_SCAN_LIMIT) break;
                    scanned++;
                    if (problemId == null || solvedIds.contains(problemId) || addedIds.contains(problemId)) continue;
                    Problem problem = problemMapper.selectProblemWithTags(problemId);
                    if (!isPublicAvailableProblem(problem)) continue;
                    int score = scoreProblem(problem, targetDifficulty, attemptedIds.contains(problemId), abilityByTagId);
                    if (score < 45) continue;
                    recommended.add(buildRecommendationItem(problem, ability, score, targetDifficulty, attemptedIds.contains(problemId)));
                    addedIds.add(problemId);
                    break;
                }
            }
        }

        // === 改进2：间隔复习 — 对超过30天未练习的标签，推荐已做过的题巩固 ===
        if (recommended.size() < safeSize) {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            for (UserAbility ability : sortedWeakAbilities) {
                if (recommended.size() >= safeSize) break;
                List<Long> tagProblemIds = problemMapper.selectProblemIdsByTagId(ability.getTagId());
                boolean hasRecentPractice = tagProblemIds.stream()
                        .map(latestSubmissionByProblem::get)
                        .filter(Objects::nonNull)
                        .anyMatch(t -> t.isAfter(thirtyDaysAgo));
                if (hasRecentPractice) continue;
                for (Long pid : tagProblemIds) {
                    if (recommended.size() >= safeSize) break;
                    if (!solvedIds.contains(pid) || addedIds.contains(pid)) continue;
                    Problem problem = problemMapper.selectProblemWithTags(pid);
                    if (!isPublicAvailableProblem(problem)) continue;
                    Map<String, Object> item = buildRecommendationItem(problem, ability, 60, targetDifficulty, true);
                    String tagName = ability.getTagName() != null ? ability.getTagName() : "unknown";
                    item.put("reason", "标签[" + tagName + "]已超过30天未练习，推荐巩固复习");
                    item.put("reviewType", "spaced_repetition");
                    recommended.add(item);
                    addedIds.add(pid);
                    break;
                }
            }
        }

        // 随机补充
        if (recommended.size() < safeSize) {
            List<Problem> extras = problemMapper.selectList(
                    new LambdaQueryWrapper<Problem>()
                            .eq(Problem::getVisibility, "PUBLIC")
                            .eq(Problem::getStatus, 1)
                            .eq(Problem::getDeleted, 0)
                            .notIn(!solvedIds.isEmpty(), Problem::getId, solvedIds)
                            .notIn(!addedIds.isEmpty(), Problem::getId, addedIds)
                            .last("LIMIT " + (safeSize * 3)));
            for (Problem problem : extras) {
                if (recommended.size() >= safeSize) break;
                Problem fullProblem = problemMapper.selectProblemWithTags(problem.getId());
                if (!isPublicAvailableProblem(fullProblem) || addedIds.contains(fullProblem.getId())) continue;
                int score = scoreProblem(fullProblem, targetDifficulty, attemptedIds.contains(fullProblem.getId()), abilityByTagId);
                recommended.add(buildRecommendationItem(fullProblem, null, score, targetDifficulty, attemptedIds.contains(fullProblem.getId())));
                addedIds.add(fullProblem.getId());
            }
        }

        recommended.sort((a, b) -> Integer.compare((Integer) b.get("matchScore"), (Integer) a.get("matchScore")));
        List<Map<String, Object>> result = recommended.size() > safeSize ? recommended.subList(0, safeSize) : recommended;

        // 写入缓存，TTL 2小时
        redisCacheService.set(cacheKey, result, RECOMMEND_CACHE_TTL_HOURS, java.util.concurrent.TimeUnit.HOURS);

        return result;
    }

    /** 日常训练 AC 后刷新该题相关标签能力，并清除用户推荐缓存。 */
    @Override
    @Transactional
    public void updateAbilityOnAccepted(Long userId, Long problemId) {
        Problem problem = problemMapper.selectProblemWithTags(problemId);
        if (problem == null || problem.getTags() == null || problem.getTags().isEmpty()) return;

        for (Tag tag : problem.getTags()) {
            refreshAbilityByTag(userId, tag);
        }

        // 用户通过新题目后，清除个性化推荐缓存
        redisCacheService.delete(RECOMMEND_CACHE_PREFIX + userId);
    }

    /** 每天凌晨全量刷新画像，修正历史数据和时间衰减带来的能力变化。 */
    @Override
    @Transactional
    @Scheduled(cron = "0 45 3 * * ?")
    public void refreshAllAbilityProfiles() {
        List<Long> userIds = submissionMapper.selectList(
                        new LambdaQueryWrapper<Submission>()
                                .select(Submission::getUserId)
                                .isNull(Submission::getContestId)
                                .isNotNull(Submission::getUserId))
                .stream()
                .map(Submission::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        List<Tag> tags = tagMapper.selectList(null);
        log.info("开始刷新用户能力画像，用户数：{}，标签数：{}", userIds.size(), tags.size());
        for (Long userId : userIds) {
            for (Tag tag : tags) {
                refreshAbilityByTag(userId, tag);
            }
        }
        log.info("用户能力画像刷新完成，用户数：{}", userIds.size());
    }

    // 时间衰减半衰期（天），70天后权重降为50%
    private static final double DECAY_HALF_LIFE_DAYS = 70.0;
    private static final double DECAY_LAMBDA = Math.log(2) / DECAY_HALF_LIFE_DAYS;

    /** 时间衰减：越近的提交权重越高，70 天前的证据大约降到一半。 */
    private double timeDecayWeight(LocalDateTime submissionTime) {
        if (submissionTime == null) return 0.5;
        long daysDiff = java.time.Duration.between(submissionTime, LocalDateTime.now()).toDays();
        return Math.exp(-DECAY_LAMBDA * Math.max(0, daysDiff));
    }

    /**
     * 刷新某个用户在单个标签上的能力。
     * 能力分综合通过质量、题目难度、训练量和稳定性，且只统计日常训练提交。
     */
    private void refreshAbilityByTag(Long userId, Tag tag) {
        List<Long> problemIds = problemMapper.selectProblemIdsByTagId(tag.getId());
        if (problemIds == null || problemIds.isEmpty()) {
            remove(new LambdaQueryWrapper<UserAbility>()
                    .eq(UserAbility::getUserId, userId)
                    .eq(UserAbility::getTagId, tag.getId()));
            return;
        }

        List<Submission> tagSubmissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .isNull(Submission::getContestId)
                        .in(Submission::getProblemId, problemIds)
                        .orderByDesc(Submission::getCreatedAt));

        UserAbility ability = this.getOne(
                new LambdaQueryWrapper<UserAbility>()
                        .eq(UserAbility::getUserId, userId)
                        .eq(UserAbility::getTagId, tag.getId()));

        if (tagSubmissions.isEmpty()) {
            if (ability != null) {
                this.removeById(ability.getId());
            }
            return;
        }

        Map<Long, List<Submission>> submissionsByProblem = tagSubmissions.stream()
                .filter(item -> item.getProblemId() != null)
                .collect(Collectors.groupingBy(Submission::getProblemId, LinkedHashMap::new, Collectors.toList()));

        Map<Long, Submission> latestAcceptedByProblem = new LinkedHashMap<>();
        for (Submission submission : tagSubmissions) {
            if (!"ACCEPTED".equals(submission.getStatus()) || submission.getProblemId() == null) {
                continue;
            }
            latestAcceptedByProblem.putIfAbsent(submission.getProblemId(), submission);
        }

        Set<Long> solvedProblemIds = new LinkedHashSet<>(latestAcceptedByProblem.keySet());
        Map<Long, Problem> solvedProblemMap = loadProblemMap(solvedProblemIds);

        int solvedCount = solvedProblemIds.size();
        int attemptCount = tagSubmissions.size();

        // === 时间衰减加权 ===
        // 标签能力不是简单计数：越新的 AC 权重越高，越老的提交会按半衰期逐步变弱。
        double weightedSolved = 0;
        double weightedDifficultySum = 0;
        double totalSolvedWeight = 0;
        for (Long pid : solvedProblemIds) {
            Submission accepted = latestAcceptedByProblem.get(pid);
            double w = timeDecayWeight(accepted != null ? accepted.getCreatedAt() : null);
            weightedSolved += w;
            totalSolvedWeight += w;
            Problem p = solvedProblemMap.get(pid);
            if (p != null) {
                weightedDifficultySum += difficultyOrder(p.getDifficulty()) * w;
            }
        }

        double weightedAttempts = 0;
        Set<Long> countedProblems = new HashSet<>();
        for (Submission s : tagSubmissions) {
            if (s.getProblemId() != null && countedProblems.add(s.getProblemId())) {
                weightedAttempts += timeDecayWeight(s.getCreatedAt());
            }
        }

        double acceptanceRate = weightedAttempts == 0 ? 0D : weightedSolved * 100.0 / weightedAttempts;
        double avgDifficulty = totalSolvedWeight == 0 ? 0D : weightedDifficultySum / totalSolvedWeight;

        double avgAttemptsPerSolved = solvedCount == 0 ? 0D : solvedProblemIds.stream()
                .mapToInt(problemId -> submissionsByProblem.getOrDefault(problemId, List.of()).size())
                .average()
                .orElse(0D);

        // 质量、难度、训练量、稳定性四类证据共同决定该标签的能力分。
        // Logistic 会让早期进步更明显，后期逐渐饱和，避免无限刷题导致分数虚高。
        double solvedVolume = logisticProgress(Math.min(weightedSolved, 20.0) / 20.0, 0.32, 8.0);
        double qualityScore = logisticProgress(acceptanceRate / 100.0, 0.54, 7.2);
        double difficultyScore = logisticProgress(avgDifficulty / 4.0, 0.56, 8.0);
        double stabilityScore = inverseLogisticPenalty(Math.max(1D, avgAttemptsPerSolved), 1.7, 3.0);
        int abilityScore = boundedScore(combineEvidence(
                qualityScore, 0.34,
                difficultyScore, 0.28,
                solvedVolume, 0.23,
                stabilityScore, 0.15
        ));

        if (ability == null) {
            ability = new UserAbility();
            ability.setUserId(userId);
            ability.setTagId(tag.getId());
        }
        ability.setSolvedCount(solvedCount);
        ability.setAttemptCount(attemptCount);
        ability.setAbilityScore(BigDecimal.valueOf(abilityScore).setScale(2, RoundingMode.HALF_UP));
        ability.setUpdatedAt(LocalDateTime.now());

        if (ability.getId() == null) {
            this.save(ability);
        } else {
            this.updateById(ability);
        }
    }

    private Map<Long, Problem> loadProblemMap(Collection<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, List<Submission>> loadAcceptedCohortByProblem(Collection<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Submission> cohort = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .select(Submission::getProblemId, Submission::getExecutionTime, Submission::getMemoryUsed)
                        .in(Submission::getProblemId, problemIds)
                        .eq(Submission::getStatus, "ACCEPTED")
                        .isNull(Submission::getContestId));

        return cohort.stream()
                .filter(item -> item.getProblemId() != null)
                .collect(Collectors.groupingBy(Submission::getProblemId, LinkedHashMap::new, Collectors.toList()));
    }

    /** 同题百分位：运行时间和内存只和同一道题的 AC 记录比较，避免跨题混算。 */
    private Double percentileAgainstAcceptedCohort(Integer ownValue, List<Submission> cohort, boolean runtimeMetric) {
        if (ownValue == null || ownValue <= 0 || cohort == null || cohort.isEmpty()) {
            return null;
        }

        List<Integer> values = cohort.stream()
                .map(item -> runtimeMetric ? item.getExecutionTime() : item.getMemoryUsed())
                .filter(Objects::nonNull)
                .filter(value -> value > 0)
                .sorted()
                .collect(Collectors.toList());

        if (values.isEmpty()) {
            return null;
        }
        if (values.size() == 1) {
            return 100D;
        }

        int lowerCount = 0;
        int equalCount = 0;
        for (Integer value : values) {
            if (value < ownValue) {
                lowerCount++;
            } else if (value.equals(ownValue)) {
                equalCount++;
            }
        }

        double averageRank = lowerCount + Math.max(0D, (equalCount - 1) / 2.0);
        return Math.max(0D, Math.min(100D, (1 - averageRank / (values.size() - 1)) * 100.0));
    }

    private Map<String, Object> indicator(String name, String description) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("description", description);
        return result;
    }

    private void appendDimensionSummary(List<Map<String, Object>> strengths,
                                        List<Map<String, Object>> weaknesses,
                                        String name,
                                        int score,
                                        String highlight,
                                        String evidence) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("score", score);
        item.put("highlight", highlight);
        item.put("evidence", evidence);
        if (score >= 70) {
            strengths.add(item);
        } else {
            weaknesses.add(item);
        }
    }

    private int calculateHabitScore(Long userId) {
        Map<String, Object> checkInStatus = checkInService.getCheckInStatus(userId);
        int streak = ((Number) checkInStatus.getOrDefault("streak", 0)).intValue();
        List<Map<String, Object>> tasks = dailyTaskService.getDailyTasks(userId);
        long completedTasks = tasks.stream().filter(item -> Boolean.TRUE.equals(item.get("completed"))).count();
        double taskCompletionRate = tasks.isEmpty() ? 0D : completedTasks * 100.0 / tasks.size();

        LocalDate today = LocalDate.now();
        long recentCheckIns = userCheckInMapper.selectCount(
                new LambdaQueryWrapper<UserCheckIn>()
                        .eq(UserCheckIn::getUserId, userId)
                        .ge(UserCheckIn::getCheckInDate, today.minusDays(29)));
        double monthlyConsistency = Math.min(100D, recentCheckIns / 30.0 * 100.0);
        double streakScore = Math.min(100D, streak * 8.0);
        return boundedScore(0.4 * streakScore + 0.35 * monthlyConsistency + 0.25 * taskCompletionRate);
    }

    private String buildHabitEvidence(Long userId) {
        Map<String, Object> checkInStatus = checkInService.getCheckInStatus(userId);
        int streak = ((Number) checkInStatus.getOrDefault("streak", 0)).intValue();
        List<Map<String, Object>> tasks = dailyTaskService.getDailyTasks(userId);
        long completedTasks = tasks.stream().filter(item -> Boolean.TRUE.equals(item.get("completed"))).count();
        return "连续签到 " + streak + " 天，今日任务完成 " + completedTasks + " / " + tasks.size();
    }

    private String buildPercentileEvidence(List<Double> percentiles, String metricName) {
        if (percentiles == null || percentiles.isEmpty()) {
            return "暂无可用的同题 Accepted 对比数据";
        }
        double averagePercentile = average(percentiles);
        return "按同题 Accepted 队列换算，平均优于 " + rounded(averagePercentile) + "% 的" + metricName + "记录";
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0D;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
    }

    /** 根据掌握深度和难度进阶，估算下一题更适合的难度层级。 */
    private int recommendedDifficultyLevel(Map<String, Object> profile) {
        Object scoresObject = profile.get("scores");
        if (!(scoresObject instanceof Map<?, ?> scores)) {
            return 1;
        }
        Object proficiencyObject = scores.get("proficiency");
        Object masteryObject = scores.get("difficultyMastery");
        int proficiency = proficiencyObject instanceof Number ? ((Number) proficiencyObject).intValue() : 0;
        int mastery = masteryObject instanceof Number ? ((Number) masteryObject).intValue() : 0;
        // 掌握深度和难度进阶共同决定推荐难度：能力越稳，下一题难度越可以上探。
        int composite = boundedScore(proficiency * 0.45 + mastery * 0.55);
        if (composite >= 82) return 4;
        if (composite >= 65) return 3;
        if (composite >= 42) return 2;
        return 1;
    }

    /** 给候选题打匹配分：弱项标签、目标难度、通过率和是否尝试过都会影响分数。 */
    private int scoreProblem(Problem problem,
                             int targetDifficulty,
                             boolean attempted,
                             Map<Long, UserAbility> abilityByTagId) {
        // 候选题基础分从 55 起，后续根据难度贴合度、是否做过、通过率和弱项标签加减分。
        int score = 55;
        int difficulty = difficultyOrder(problem.getDifficulty());
        score += Math.max(0, 20 - Math.abs(difficulty - targetDifficulty) * 8);
        if (attempted) {
            score -= 8;
        }
        // === 改进3：用通过率做连续难度微调 ===
        BigDecimal acceptRate = problem.getAcceptRate();
        if (acceptRate != null) {
            double rate = acceptRate.doubleValue();
            double targetRate = targetDifficulty == 4 ? 15 : targetDifficulty == 3 ? 30 : targetDifficulty == 2 ? 50 : 70;
            double rateDiff = Math.abs(rate - targetRate);
            if (rateDiff <= 15) {
                score += 10;
            } else if (rateDiff <= 30) {
                score += 4;
            } else if (rate < 10) {
                score -= 6;
            }
        }
        if (problem.getTags() != null && !problem.getTags().isEmpty()) {
            int weakestTagBonus = problem.getTags().stream()
                    .map(Tag::getId)
                    .map(abilityByTagId::get)
                    .filter(Objects::nonNull)
                    .map(UserAbility::getAbilityScore)
                    .filter(Objects::nonNull)
                    .mapToInt(scoreValue -> 20 - Math.min(20, scoreValue.intValue() / 5))
                    .max()
                    .orElse(6);
            score += weakestTagBonus;
        }
        return Math.max(0, Math.min(100, score));
    }

    private Map<String, Object> buildRecommendationItem(Problem problem,
                                                        UserAbility anchorAbility,
                                                        int score,
                                                        int targetDifficulty,
                                                        boolean attempted) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", problem.getId());
        item.put("title", problem.getTitle());
        item.put("difficulty", problem.getDifficulty());
        item.put("acceptRate", problem.getAcceptRate());
        item.put("matchScore", score);
        item.put("attempted", attempted);
        item.put("reason", buildRecommendationReason(problem, anchorAbility, targetDifficulty, attempted));
        item.put("focusTags", problem.getTags() == null ? List.of() : problem.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
        return item;
    }

    /** 生成推荐理由，方便前端告诉用户“为什么推荐这道题”。 */
    private String buildRecommendationReason(Problem problem,
                                             UserAbility anchorAbility,
                                             int targetDifficulty,
                                             boolean attempted) {
        StringBuilder builder = new StringBuilder();
        if (anchorAbility != null && anchorAbility.getTagName() != null) {
            builder.append("优先补强你的弱项标签“").append(anchorAbility.getTagName()).append("”");
        } else if (problem.getTags() != null && !problem.getTags().isEmpty()) {
            builder.append("覆盖你近期值得继续训练的标签“").append(problem.getTags().get(0).getName()).append("”");
        } else {
            builder.append("适合作为下一道进阶训练题");
        }
        if (difficultyOrder(problem.getDifficulty()) == targetDifficulty) {
            builder.append("，难度与当前能力阶段匹配");
        }
        if (attempted) {
            builder.append("，你之前尝试过这题，适合回炉巩固");
        }
        return builder.toString();
    }

    private boolean isPublicAvailableProblem(Problem problem) {
        return problem != null
                && "PUBLIC".equals(problem.getVisibility())
                && Integer.valueOf(1).equals(problem.getStatus())
                && !Integer.valueOf(1).equals(problem.getDeleted());
    }

    private int difficultyOrder(String difficulty) {
        return DIFFICULTY_ORDER.getOrDefault(difficulty, 1);
    }

    private int boundedScore(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(value)));
    }

    private double rounded(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatPercent(double value) {
        return rounded(value) + "%";
    }

    /** Logistic 标准化：把不同量纲的证据压到 0-100，前期提升明显，后期逐渐饱和。 */
    private double logisticProgress(double value, double midpoint, double steepness) {
        double normalized = 1.0 / (1.0 + Math.exp(-steepness * (value - midpoint)));
        return normalized * 100.0;
    }

    /** 反向 Logistic：尝试次数、方差等越高越扣分的指标用这个函数换算。 */
    private double inverseLogisticPenalty(double value, double midpoint, double steepness) {
        double normalized = 1.0 / (1.0 + Math.exp(steepness * (value - midpoint)));
        return normalized * 100.0;
    }

    private double combineEvidence(double value1, double weight1, double value2, double weight2) {
        return combineEvidence(new double[]{value1, value2}, new double[]{weight1, weight2});
    }

    private double combineEvidence(double value1, double weight1, double value2, double weight2, double value3, double weight3) {
        return combineEvidence(new double[]{value1, value2, value3}, new double[]{weight1, weight2, weight3});
    }

    private double combineEvidence(double value1, double weight1, double value2, double weight2,
                                   double value3, double weight3, double value4, double weight4) {
        return combineEvidence(
                new double[]{value1, value2, value3, value4},
                new double[]{weight1, weight2, weight3, weight4}
        );
    }

    /** 非线性融合：高质量证据更重要，避免低质量刷题把能力分无限抬高。 */
    private double combineEvidence(double[] values, double[] weights) {
        double totalWeight = 0D;
        double weighted = 0D;
        for (int i = 0; i < values.length; i++) {
            double weight = i < weights.length ? weights[i] : 0D;
            if (weight <= 0D) {
                continue;
            }
            totalWeight += weight;
            weighted += Math.pow(Math.max(0D, values[i]) / 100.0, NONLINEAR_SCALE) * weight;
        }
        if (totalWeight == 0D) {
            return 0D;
        }
        return Math.pow(weighted / totalWeight, 1.0 / NONLINEAR_SCALE) * 100.0;
    }
}

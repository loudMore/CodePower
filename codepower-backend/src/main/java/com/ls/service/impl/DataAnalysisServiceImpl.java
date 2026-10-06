/**
 * 文件说明：数据分析 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ls.domain.LearningPath;
import com.ls.domain.LearningPathStage;
import com.ls.domain.Problem;
import com.ls.domain.Submission;
import com.ls.domain.Tag;
import com.ls.domain.User;
import com.ls.domain.UserAbility;
import com.ls.domain.UserLearningProgress;
import com.ls.domain.UserProfile;
import com.ls.mapper.LearningPathMapper;
import com.ls.mapper.LearningPathStageMapper;
import com.ls.mapper.ProblemMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.mapper.TagMapper;
import com.ls.mapper.UserAbilityMapper;
import com.ls.mapper.UserLearningProgressMapper;
import com.ls.mapper.UserMapper;
import com.ls.mapper.UserProfileMapper;
import com.ls.service.DataAnalysisService;
import com.ls.service.RedisCacheService;
import com.ls.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 数据统计与分析服务实现 */
@Service
@RequiredArgsConstructor
public class DataAnalysisServiceImpl implements DataAnalysisService {

    private static final List<String> CHART_COLORS = List.of("#409EFF", "#67c23a", "#f56c6c", "#e6a23c", "#909399");

    // L2 Redis缓存：图表数据计算量大但实时性要求不高，缓存5分钟
    private static final Duration CHART_CACHE_TTL = Duration.ofMinutes(5);

    private final UserMapper userMapper;
    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final LearningPathMapper learningPathMapper;
    private final LearningPathStageMapper learningPathStageMapper;
    private final UserLearningProgressMapper userLearningProgressMapper;
    private final UserAbilityMapper userAbilityMapper;
    private final TagMapper tagMapper;
    private final UserProfileMapper userProfileMapper;
    private final RedisCacheService redisCacheService;
    private final RegionService regionService;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMonthlyActiveUsers() {
        String cacheKey = "chart:monthly-active-users";
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) return cached;

        List<String> months = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i).withDayOfMonth(1);
            LocalDate nextMonth = month.plusMonths(1);
            months.add(month.getMonthValue() + "月");
            List<Submission> monthlySubmissions = submissionMapper.selectList(
                    new LambdaQueryWrapper<Submission>()
                            .select(Submission::getUserId)
                            .ge(Submission::getCreatedAt, month.atStartOfDay())
                            .lt(Submission::getCreatedAt, nextMonth.atStartOfDay())
                            .groupBy(Submission::getUserId));
            counts.add((long) monthlySubmissions.size());
        }
        Map<String, Object> result = Map.of("xAxisData", months, "yAxisData", counts);
        redisCacheService.set(cacheKey, result, CHART_CACHE_TTL);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPopularLearningPaths() {
        String cacheKey = "chart:popular-learning-paths";
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) return cached;

        List<LearningPath> activePaths = learningPathMapper.selectList(
                new LambdaQueryWrapper<LearningPath>()
                        .eq(LearningPath::getStatus, 1)
                        .eq(LearningPath::getDeleted, 0));
        if (activePaths.isEmpty()) {
            return Map.of("xAxisData", List.of(), "yAxisData", List.of(), "colors", CHART_COLORS);
        }

        List<UserLearningProgress> completedProgress = userLearningProgressMapper.selectList(
                        new LambdaQueryWrapper<UserLearningProgress>()
                                .select(UserLearningProgress::getPathId, UserLearningProgress::getUserId,
                                        UserLearningProgress::getStageId)
                                .in(UserLearningProgress::getPathId, activePaths.stream().map(LearningPath::getId).toList())
                                .eq(UserLearningProgress::getStatus, "COMPLETED"));
        Map<Long, Long> completedCountMap = completedProgress.stream()
                .filter(item -> item.getPathId() != null && item.getUserId() != null && item.getStageId() != null)
                .collect(Collectors.groupingBy(
                        UserLearningProgress::getPathId,
                        Collectors.mapping(this::progressIdentity,
                                Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))));

        List<LearningPath> topPaths = activePaths.stream()
                .sorted(Comparator.comparingLong((LearningPath path) -> completedCountMap.getOrDefault(path.getId(), 0L)).reversed()
                        .thenComparing(LearningPath::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .limit(5)
                .toList();

        List<String> names = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (LearningPath path : topPaths) {
            names.add(path.getTitle());
            values.add(completedCountMap.getOrDefault(path.getId(), 0L));
        }
        Map<String, Object> result = Map.of("xAxisData", names, "yAxisData", values, "colors", CHART_COLORS);
        redisCacheService.set(cacheKey, result, CHART_CACHE_TTL);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserRadarChartData() {
        String cacheKey = "chart:user-radar";
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) return cached;

        List<UserAbility> allAbilities = userAbilityMapper.selectList(new LambdaQueryWrapper<UserAbility>()
                .orderByDesc(UserAbility::getAbilityScore)
                .orderByDesc(UserAbility::getSolvedCount));
        if (allAbilities.isEmpty()) {
            return Map.of(
                    "indicator", List.of(),
                    "value", List.of(),
                    "averageValue", List.of(),
                    "analysisBasis", "基于用户标签能力表统计");
        }

        Map<Long, String> tagNameMap = tagMapper.selectBatchIds(allAbilities.stream()
                        .map(UserAbility::getTagId)
                        .filter(tagId -> tagId != null)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));

        allAbilities.forEach(ability -> ability.setTagName(
                tagNameMap.getOrDefault(ability.getTagId(), "未知标签")));

        Map<Long, List<UserAbility>> groupedByTag = allAbilities.stream()
                .collect(Collectors.groupingBy(UserAbility::getTagId));

        List<Map.Entry<Long, List<UserAbility>>> topTags = groupedByTag.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<Long, List<UserAbility>> entry) ->
                        entry.getValue().stream()
                                .map(UserAbility::getAbilityScore)
                                .filter(score -> score != null)
                                .mapToDouble(BigDecimal::doubleValue)
                                .average()
                                .orElse(0))
                        .reversed())
                .limit(6)
                .toList();

        List<Map<String, Object>> indicators = new ArrayList<>();
        List<Integer> averageValues = new ArrayList<>();
        List<Integer> benchmarkValues = new ArrayList<>();

        for (Map.Entry<Long, List<UserAbility>> entry : topTags) {
            List<UserAbility> abilities = entry.getValue();
            String tagName = abilities.stream()
                    .map(UserAbility::getTagName)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst()
                    .orElse("未知标签");
            double averageScore = abilities.stream()
                    .map(UserAbility::getAbilityScore)
                    .filter(score -> score != null)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average()
                    .orElse(0);
            double bestScore = abilities.stream()
                    .map(UserAbility::getAbilityScore)
                    .filter(score -> score != null)
                    .mapToDouble(BigDecimal::doubleValue)
                    .max()
                    .orElse(0);

            indicators.add(Map.of("name", tagName, "max", 100));
            averageValues.add((int) Math.round(averageScore));
            benchmarkValues.add((int) Math.round(Math.max(averageScore, bestScore * 0.85)));
        }

        Map<String, Object> result = Map.of(
                "indicator", indicators,
                "value", averageValues,
                "averageValue", benchmarkValues,
                "analysisBasis", "基于 user_abilities 的标签得分、解题数和尝试数聚合");
        redisCacheService.set(cacheKey, result, CHART_CACHE_TTL);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getLearningPathOverview() {
        String cacheKey = "chart:learning-path-overview";
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) return cached;

        List<LearningPath> activePaths = learningPathMapper.selectList(
                new LambdaQueryWrapper<LearningPath>()
                        .eq(LearningPath::getStatus, 1)
                        .eq(LearningPath::getDeleted, 0));
        if (activePaths.isEmpty()) {
            return Map.of(
                    "totalPaths", 0,
                    "totalStages", 0,
                    "activeLearners", 0,
                    "completedPasses", 0,
                    "pathStats", List.of());
        }

        List<Long> pathIds = activePaths.stream().map(LearningPath::getId).toList();
        List<UserLearningProgress> allProgress = userLearningProgressMapper.selectList(
                new LambdaQueryWrapper<UserLearningProgress>()
                        .select(UserLearningProgress::getPathId, UserLearningProgress::getUserId,
                                UserLearningProgress::getStageId, UserLearningProgress::getStatus)
                        .in(UserLearningProgress::getPathId, pathIds));
        List<UserLearningProgress> completedProgress = allProgress.stream()
                .filter(item -> "COMPLETED".equals(item.getStatus()))
                .toList();

        Map<Long, Long> stageCountMap = getStageCountMap(pathIds);
        Map<Long, Set<Long>> learnerMap = allProgress.stream()
                .filter(item -> item.getPathId() != null && item.getUserId() != null)
                .collect(Collectors.groupingBy(
                        UserLearningProgress::getPathId,
                        Collectors.mapping(UserLearningProgress::getUserId, Collectors.toSet())));
        Map<Long, Long> completedCountMap = completedProgress.stream()
                .filter(item -> item.getPathId() != null && item.getUserId() != null && item.getStageId() != null)
                .collect(Collectors.groupingBy(
                        UserLearningProgress::getPathId,
                        Collectors.mapping(this::progressIdentity,
                                Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))));

        List<Map<String, Object>> pathStats = activePaths.stream()
                .sorted(Comparator.comparing(LearningPath::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(path -> {
                    long totalStages = stageCountMap.getOrDefault(path.getId(), 0L);
                    long activeLearners = learnerMap.getOrDefault(path.getId(), Set.of()).size();
                    long completedStages = completedCountMap.getOrDefault(path.getId(), 0L);
                    long expectedPasses = activeLearners * totalStages;
                    int completionRate = expectedPasses > 0
                            ? BigDecimal.valueOf(completedStages * 100.0 / expectedPasses)
                            .setScale(0, RoundingMode.HALF_UP)
                            .intValue()
                            : 0;

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("pathId", path.getId());
                    item.put("activeLearners", activeLearners);
                    item.put("completedPasses", completedStages);
                    item.put("totalStages", totalStages);
                    item.put("completionRate", completionRate);
                    return item;
                })
                .toList();

        Map<Long, Map<String, Object>> pathStatMap = pathStats.stream()
                .collect(Collectors.toMap(item -> ((Number) item.get("pathId")).longValue(), Function.identity()));

        long totalStages = stageCountMap.values().stream().mapToLong(Long::longValue).sum();
        long activeLearners = learnerMap.values().stream()
                .flatMap(Set::stream)
                .distinct()
                .count();
        long completedPasses = completedCountMap.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Object> result = Map.of(
                "totalPaths", activePaths.size(),
                "totalStages", totalStages,
                "activeLearners", activeLearners,
                "completedPasses", completedPasses,
                "pathStats", pathStatMap.values().stream().toList());
        redisCacheService.set(cacheKey, result, CHART_CACHE_TTL);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserDistributionMapData() {
        String cacheKey = "chart:user-distribution-map";
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) return cached;

        List<UserProfile> profiles = userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                .select(UserProfile::getRegion)
                .isNotNull(UserProfile::getRegion));
        if (profiles.isEmpty()) {
            return Map.of("mapData", List.of());
        }

        Map<String, Long> regionCountMap = profiles.stream()
                .map(UserProfile::getRegion)
                .filter(region -> region != null && !region.isBlank())
                .map(String::trim)
                .map(regionService::normalizeRegion)
                .filter(region -> region != null && !region.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<Map<String, Object>> data = regionCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .toList();
        Map<String, Object> result = Map.of("mapData", data);
        redisCacheService.set(cacheKey, result, CHART_CACHE_TTL);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserGrowthTrendData() {
        String cacheKey = "chart:user-growth-trend:v2";
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) return cached;

        List<String> months = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i).withDayOfMonth(1);
            LocalDate nextMonth = month.plusMonths(1);
            months.add(String.valueOf(month.getMonthValue()));

            // 首页展示的是“截至该月末的平台注册用户总量”，不是每月新增人数。
            // 这样当前总用户 12 人时，最近月份最多也只会显示 12，避免把累计值误读成新增值。
            long cumulativeUsers = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getStatus, 1)
                            .lt(User::getCreatedAt, nextMonth.atStartOfDay()));
            counts.add(cumulativeUsers);
        }
        Map<String, Object> result = Map.of("xAxisData", months, "yAxisData", counts);
        redisCacheService.set(cacheKey, result, CHART_CACHE_TTL);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProblemDifficultyDistributionData() {
        String cacheKey = "chart:problem-difficulty-distribution";
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) return cached;

        List<Problem> allProblems = problemMapper.selectList(
                new LambdaQueryWrapper<Problem>()
                        .select(Problem::getDifficulty)
                        .eq(Problem::getStatus, 1)
                        .eq(Problem::getDeleted, 0));
        Map<String, Long> grouped = allProblems.stream()
                .collect(Collectors.groupingBy(p -> p.getDifficulty() != null ? p.getDifficulty() : "UNKNOWN",
                        Collectors.counting()));
        Map<String, String> difficultyNames = Map.of("简单", "简单", "普通", "普通", "困难", "困难", "极限", "极限",
                "EASY", "简单", "MEDIUM", "普通", "HARD", "困难", "EXTREME", "极限");
        Map<String, String> difficultyColors = Map.of("简单", "#67C23A", "普通", "#E6A23C", "困难", "#409EFF", "极限", "#F56C6C",
                "EASY", "#67C23A", "MEDIUM", "#E6A23C", "HARD", "#409EFF", "EXTREME", "#F56C6C");
        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            String key = entry.getKey();
            data.add(Map.of(
                    "name", difficultyNames.getOrDefault(key, key),
                    "value", entry.getValue(),
                    "color", difficultyColors.getOrDefault(key, "#909399")));
        }
        Map<String, Object> result = Map.of("data", data);
        redisCacheService.set(cacheKey, result, CHART_CACHE_TTL);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserActiveHoursData() {
        String cacheKey = "chart:user-active-hours:v2";
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) return cached;

        List<String> hours = List.of("0-2", "2-4", "4-6", "6-8", "8-10", "10-12", "12-14", "14-16", "16-18", "18-20", "20-22", "22-24");
        List<Long> counts = new ArrayList<>();
        Set<Long> activeUserIds = userMapper.selectList(
                        new LambdaQueryWrapper<User>()
                                .select(User::getId)
                                .eq(User::getStatus, 1))
                .stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        List<Submission> recentSubmissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .select(Submission::getUserId, Submission::getCreatedAt)
                        .isNotNull(Submission::getUserId)
                        .isNotNull(Submission::getCreatedAt)
                        .ge(Submission::getCreatedAt, LocalDateTime.now().minusDays(30)));

        // 这里统计“近30天每个时段出现过提交行为的用户数”，不是提交次数。
        // 同一个用户在同一时段重复提交只记 1 人，保证单个柱子的数值不会超过平台用户规模。
        Map<Integer, Long> hourMap = recentSubmissions.stream()
                .filter(s -> activeUserIds.contains(s.getUserId()))
                .collect(Collectors.groupingBy(
                        s -> s.getCreatedAt().getHour() / 2,
                        Collectors.mapping(Submission::getUserId,
                                Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))));
        for (int i = 0; i < 12; i++) {
            counts.add(hourMap.getOrDefault(i, 0L));
        }
        Map<String, Object> result = Map.of("xAxisData", hours, "yAxisData", counts);
        redisCacheService.set(cacheKey, result, CHART_CACHE_TTL);
        return result;
    }

    private Map<Long, Long> getStageCountMap(List<Long> pathIds) {
        if (pathIds.isEmpty()) {
            return Map.of();
        }
        return learningPathStageMapper.selectList(
                        new LambdaQueryWrapper<LearningPathStage>()
                                .select(LearningPathStage::getId, LearningPathStage::getPathId)
                                .in(LearningPathStage::getPathId, pathIds))
                .stream()
                .collect(Collectors.groupingBy(LearningPathStage::getPathId, Collectors.counting()));
    }

    private String progressIdentity(UserLearningProgress progress) {
        return progress.getUserId() + ":" + progress.getStageId();
    }
}

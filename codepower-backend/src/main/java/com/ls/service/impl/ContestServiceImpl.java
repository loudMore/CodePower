/**
 * 文件说明：竞赛 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.domain.*;
import com.ls.event.ProblemCacheEvictEvent;
import com.ls.mapper.*;
import com.ls.service.ContestService;
import com.ls.service.NotificationService;
import com.ls.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 竞赛服务实现。
 * 负责竞赛报名、访问控制、题目缓存、排名计算、私有题导入和竞赛缓存失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContestServiceImpl extends ServiceImpl<ContestMapper, Contest> implements ContestService {

    private final ContestProblemMapper contestProblemMapper;
    private final ContestRegistrationMapper registrationMapper;
    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;
    private final NotificationService notificationService;
    private final ProblemSetItemMapper problemSetItemMapper;
    private final ProblemSetMapper problemSetMapper;
    private final RedisCacheService redisCacheService;
    private final UserProfileMapper userProfileMapper;
    private final Cache<Long, List<ContestProblem>> contestProblemListCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(20000)
            .build();

    private static final int PENALTY_MINUTES_PER_WRONG = 20;
    private static final Duration CONTEST_LIST_CACHE_TTL = Duration.ofSeconds(60);
    private static final Duration CONTEST_DETAIL_CACHE_TTL = Duration.ofMinutes(2);
    private static final Duration CONTEST_RANKING_CACHE_TTL = Duration.ofSeconds(15);
    private static final Duration CONTEST_PROBLEMS_REDIS_TTL = Duration.ofHours(6);

    /** 查询公开竞赛列表，并使用 Redis 短期缓存减轻列表访问压力。 */
    @Override
    @SuppressWarnings("unchecked")
    public IPage<Contest> listContests(String status, String type, int page, int size, Long viewerId) {
        String cacheKey = buildContestListCacheKey(status, type, page, size, viewerId);
        Page<Contest> cached = redisCacheService.get(cacheKey, Page.class);
        if (cached != null) {
            return cached;
        }

        User viewer = viewerId == null ? null : userMapper.selectById(viewerId);
        String viewerRole = viewer == null ? null : viewer.getRole();
        boolean adminViewer = "ADMIN".equals(viewerRole);
        List<Long> joinedContestIds = viewerId == null || adminViewer
                ? List.of()
                : registrationMapper.selectList(new LambdaQueryWrapper<ContestRegistration>()
                        .select(ContestRegistration::getContestId)
                        .eq(ContestRegistration::getUserId, viewerId))
                .stream()
                .map(ContestRegistration::getContestId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        QueryWrapper<Contest> wrapper = new QueryWrapper<Contest>()
                .eq(status != null, "status", status)
                .eq(type != null, "type", type);
        if (!adminViewer) {
            wrapper.and(w -> {
                w.eq("is_public", 1).or().isNull("is_public");
                if (viewerId != null) {
                    w.or().eq("creator_id", viewerId);
                    if (!joinedContestIds.isEmpty()) {
                        w.or().in("id", joinedContestIds);
                    }
                }
            });
        }
        applyContestListOrder(wrapper);
        Page<Contest> result = (Page<Contest>) this.page(new Page<>(page, size), wrapper);
        autoUpdateStatuses(result.getRecords());
        batchFillContestInfo(result.getRecords());
        result.getRecords().forEach(c -> {
            fillContestAccessInfo(c, viewerId);
            applyViewerSensitiveFields(c, viewerId, viewerRole);
        });
        redisCacheService.set(cacheKey, result, CONTEST_LIST_CACHE_TTL);
        return result;
    }

    /** 获取竞赛详情，同时填充报名状态、访问权限和动态状态。 */
    @Override
    public Contest getContestDetail(Long contestId, Long userId) {
        Contest contest = this.getById(contestId);
        if (contest == null) throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");

        autoUpdateStatus(contest);
        batchFillContestInfo(List.of(contest));
        assertCanViewContest(contest, userId);
        fillContestAccessInfo(contest, userId);
        applyViewerSensitiveFields(contest, userId, resolveUserRole(userId));
        return contest;
    }

    /** 报名公开竞赛，校验密码、人数上限和重复报名。 */
    @Override
    @Transactional
    public void register(Long userId, Long contestId, String password) {
        Contest contest = this.getById(contestId);
        if (contest == null) throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");

        autoUpdateStatus(contest);

        if (Objects.equals(contest.getCreatorId(), userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "竞赛创建者已默认参赛，无需重复报名");
        }

        if ("ENDED".equals(contest.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "竞赛已结束，无法报名");
        }
        if (isInviteOnly(contest)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "非公开竞赛请使用邀请码加入");
        }
        if (contest.getPassword() != null && !contest.getPassword().isEmpty()
                && !contest.getPassword().equals(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "竞赛密码错误");
        }

        ContestRegistration existing = registrationMapper.selectOne(
                new LambdaQueryWrapper<ContestRegistration>()
                        .eq(ContestRegistration::getContestId, contestId)
                        .eq(ContestRegistration::getUserId, userId));
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已报名该竞赛，不可重复参加");
        }

        long count = registrationMapper.selectCount(
                new LambdaQueryWrapper<ContestRegistration>().eq(ContestRegistration::getContestId, contestId));
        if (hasParticipantLimit(contest) && count >= contest.getMaxParticipants()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报名人数已满");
        }

        ContestRegistration reg = new ContestRegistration();
        reg.setContestId(contestId);
        reg.setUserId(userId);
        reg.setRegisteredAt(LocalDateTime.now());
        registrationMapper.insert(reg);
        evictContestCaches(contestId);

        notificationService.createNotification(userId, "CONTEST_REGISTER",
                "报名成功：" + contest.getTitle(),
                "你已成功报名「" + contest.getTitle() + "」，比赛时间：" + contest.getStartTime() + "，请准时参加！",
                contestId);
    }

    /** 校验竞赛提交是否合法：竞赛进行中、用户可参赛、题目属于竞赛、语言被允许。 */
    @Override
    public void validateContestSubmission(Long contestId, Long userId, Long problemId, String language) {
        Contest contest = this.getById(contestId);
        if (contest == null) throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");

        autoUpdateStatus(contest);

        if (!"RUNNING".equals(contest.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "UPCOMING".equals(contest.getStatus()) ? "竞赛尚未开始" : "竞赛已结束，无法提交");
        }

        ContestRegistration registration = getOrCreateParticipantRegistration(contest, userId);
        if (registration == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "你尚未报名该竞赛");
        }
        if (isParticipantTimeExpired(contest, registration, LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "个人考试时长已到，无法继续提交");
        }
        if (problemId == null || contestProblemMapper.selectCount(
                new LambdaQueryWrapper<ContestProblem>()
                        .eq(ContestProblem::getContestId, contestId)
                        .eq(ContestProblem::getProblemId, problemId)) == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该题目不属于当前竞赛，无法按竞赛提交");
        }
        touchContestParticipation(registration, true);

        if (contest.getAllowedLanguages() != null && !contest.getAllowedLanguages().isEmpty()) {
            Set<String> allowed = Arrays.stream(contest.getAllowedLanguages().split(","))
                    .map(String::trim).map(String::toLowerCase).collect(Collectors.toSet());
            if (!allowed.contains(language.toLowerCase())) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "该竞赛不允许使用 " + language + " 语言，允许的语言：" + contest.getAllowedLanguages());
            }
        }
    }

    /** 获取竞赛题目列表，使用 Caffeine + Redis 缓存提升切题和排名页加载速度。 */
    @Override
    @SuppressWarnings("unchecked")
    public List<ContestProblem> getContestProblems(Long contestId) {
        List<ContestProblem> cached = contestProblemListCache.getIfPresent(contestId);
        if (cached != null) {
            return cached;
        }
        String cacheKey = buildContestProblemsCacheKey(contestId);
        List<ContestProblem> redisCached = redisCacheService.get(cacheKey, List.class);
        if (redisCached != null) {
            contestProblemListCache.put(contestId, redisCached);
            return redisCached;
        }

        List<ContestProblem> problems = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .eq(ContestProblem::getContestId, contestId)
                        .orderByAsc(ContestProblem::getSortOrder));
        if (!problems.isEmpty()) {
            List<Long> pids = problems.stream().map(ContestProblem::getProblemId).collect(Collectors.toList());
            Map<Long, Problem> pMap = problemMapper.selectProblemSummariesByIds(pids).stream()
                    .collect(Collectors.toMap(Problem::getId, p -> p));
            problems.forEach(cp -> {
                Problem p = pMap.get(cp.getProblemId());
                if (p != null) {
                    cp.setProblemTitle(p.getTitle());
                    cp.setDifficulty(p.getDifficulty());
                }
            });
        }
        contestProblemListCache.put(contestId, problems);
        redisCacheService.set(cacheKey, problems, CONTEST_PROBLEMS_REDIS_TTL);
        return problems;
    }

    /** 清理竞赛详情、题目列表、排名和列表缓存。 */
    @Override
    public void evictContestCaches(Long contestId) {
        contestProblemListCache.invalidate(contestId);
        redisCacheService.deleteKeys(List.of(
                buildContestDetailCacheKey(contestId),
                buildContestProblemsCacheKey(contestId)
        ));
        redisCacheService.deleteByPattern(buildContestRankingCacheKey(contestId) + "*");
        redisCacheService.deleteByPattern("contest:list:*");
    }

    /** 题目被修改或上下线时，清理竞赛题目缓存，避免旧标题和旧可见性继续展示。 */
    @EventListener
    public void handleProblemCacheEvictEvent(ProblemCacheEvictEvent event) {
        contestProblemListCache.invalidateAll();
        redisCacheService.deleteByPattern("contest:problems:*");
    }

    /** 计算竞赛排名：按题目关系表分值、首次 AC、错误罚时和筛选视角生成榜单。 */
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRanking(Long contestId, Long currentUserId, String filter) {
        String normalizedFilter = normalizeRankingFilter(filter);
        String viewerKey = currentUserId == null ? "guest" : String.valueOf(currentUserId);
        String cacheKey = buildContestRankingCacheKey(contestId) + ":" + normalizedFilter + ":u:" + viewerKey;
        Contest contest = this.getById(contestId);
        if (contest == null) throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");

        autoUpdateStatus(contest);
        assertCanViewContest(contest, currentUserId);

        Object cached = redisCacheService.get(cacheKey);
        if (cached instanceof List) {
            return (List<Map<String, Object>>) cached;
        }

        List<ContestRegistration> registrations = registrationMapper.selectList(
                new LambdaQueryWrapper<ContestRegistration>()
                        .select(ContestRegistration::getId, ContestRegistration::getContestId,
                                ContestRegistration::getUserId, ContestRegistration::getRegisteredAt,
                                ContestRegistration::getEnteredAt, ContestRegistration::getLastSubmissionAt)
                        .eq(ContestRegistration::getContestId, contestId)
                        .orderByAsc(ContestRegistration::getRegisteredAt));
        if (registrations.isEmpty()) return new ArrayList<>();

        if (Objects.equals(contest.getCreatorId(), currentUserId)) {
            registrations = ensureCreatorRegistration(contest, registrations);
        }

        List<Long> userIds = registrations.stream().map(ContestRegistration::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = loadUserSummaryMap(userIds);
        Map<Long, String> avatarMap = loadAvatarMap(userIds);

        List<ContestProblem> contestProblems = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .select(ContestProblem::getContestId, ContestProblem::getProblemId,
                                ContestProblem::getSortOrder, ContestProblem::getScore)
                        .eq(ContestProblem::getContestId, contestId)
                        .orderByAsc(ContestProblem::getSortOrder));
        if (contestProblems.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> contestProblemIds = contestProblems.stream().map(ContestProblem::getProblemId).collect(Collectors.toSet());

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .select(Submission::getId, Submission::getUserId, Submission::getProblemId, Submission::getStatus,
                                Submission::getScore, Submission::getLanguage, Submission::getCreatedAt)
                        .eq(Submission::getContestId, contestId)
                        .in(Submission::getUserId, userIds)
                        .orderByAsc(Submission::getCreatedAt));

        Map<Long, Map<Long, List<Submission>>> submissionsByUserAndProblem = submissions.stream()
                .filter(s -> contestProblemIds.contains(s.getProblemId()))
                .collect(Collectors.groupingBy(Submission::getUserId,
                        Collectors.groupingBy(Submission::getProblemId)));

        Map<Long, List<Submission>> submissionsByUser = submissions.stream()
                .filter(s -> contestProblemIds.contains(s.getProblemId()))
                .collect(Collectors.groupingBy(Submission::getUserId));

        LocalDateTime contestStart = contest.getStartTime();
        List<Map<String, Object>> ranking = new ArrayList<>(registrations.size());

        for (ContestRegistration reg : registrations) {
            Long uid = reg.getUserId();
            User user = userMap.get(uid);
            Map<Long, List<Submission>> subsByProblem = submissionsByUserAndProblem.getOrDefault(uid, Collections.emptyMap());
            List<Submission> userSubmissions = submissionsByUser.getOrDefault(uid, Collections.emptyList());

        /**
     * ACM 赛制排名计算（适配变体）。
     *
     * 计分规则：
     *   AC 的题：得分 = 题目设定分值，罚时 = 首次AC时间(分钟) + 首次AC前的错误次数 × 20
     *   未 AC 的题：得分 = 测试点通过最高百分比 × 题目分值（部分分）
     *
     * 排序规则（四级比较）：
     *   1. 总分降序 → 2. 通过题数降序 → 3. 总罚时升序 → 4. 最后提交时间升序
     *
     * 与标准 ACM 的差别：
     *   - 先比总分而非先比题数（因为系统支持每题设置不同分值）
     *   - 未 AC 的题给部分分（因为系统有隐藏测试点评分）
     */
            List<Map<String, Object>> problemDetails = new ArrayList<>(contestProblems.size());

            for (ContestProblem cp : contestProblems) {
                Long pid = cp.getProblemId();
                List<Submission> pSubs = subsByProblem.getOrDefault(pid, Collections.emptyList());
                List<Submission> finalSubs = pSubs.stream()
                        .filter(this::isFinalContestSubmission)
                        .toList();
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("problemId", pid);
                detail.put("attempts", finalSubs.size());

                // ACM 计榜以每题第一次通过为准：同一选手反复提交同一道题，不会因为重复 AC 反复占位或累加成绩。
                Submission firstAc = null;
                long wrongBefore = 0;
                for (Submission submission : finalSubs) {
                    if ("ACCEPTED".equals(submission.getStatus())) {
                        firstAc = submission;
                        break;
                    }
                    wrongBefore++;
                }

                if (firstAc != null) {
                    solvedCount++;
                    long acMinutes = Math.max(0, Duration.between(contestStart, firstAc.getCreatedAt()).toMinutes());
                    long penalty = acMinutes + wrongBefore * PENALTY_MINUTES_PER_WRONG;
                    totalPenalty += penalty;
                    int score = cp.getScore() != null ? cp.getScore() : 100;
                    totalScore += score;
                    detail.put("accepted", true);
                    detail.put("acTime", acMinutes);
                    detail.put("wrongAttempts", wrongBefore);
                    detail.put("penalty", penalty);
                    detail.put("score", score);
                } else {
                    int bestScore = bestContestProblemScore(finalSubs, cp);
                    totalScore += bestScore;
                    detail.put("accepted", false);
                    detail.put("wrongAttempts", (long) finalSubs.size());
                    detail.put("score", bestScore);
                }
                problemDetails.add(detail);
            }

            boolean entered = reg.getEnteredAt() != null || !userSubmissions.isEmpty();
            boolean hasSubmission = !userSubmissions.isEmpty();
            LocalDateTime lastSubmitAt = reg.getLastSubmissionAt();
            if (lastSubmitAt == null && hasSubmission) {
                lastSubmitAt = userSubmissions.get(userSubmissions.size() - 1).getCreatedAt();
            }
            boolean shouldInclude = shouldIncludeRankingEntry(normalizedFilter, entered, hasSubmission, currentUserId, uid);
            if (!shouldInclude) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", uid);
            item.put("username", user != null ? user.getUsername() : "未知用户");
            item.put("avatar", avatarMap.getOrDefault(uid, "/avatars/avatar-1.svg"));
            item.put("solvedCount", solvedCount);
            item.put("totalPenalty", totalPenalty);
            item.put("totalScore", totalScore);
            item.put("registeredAt", reg.getRegisteredAt());
            item.put("enteredAt", entered ? firstNonNull(reg.getEnteredAt(), userSubmissions.isEmpty() ? null : userSubmissions.get(0).getCreatedAt()) : null);
            item.put("lastSubmissionAt", lastSubmitAt);
            item.put("entered", entered);
            item.put("hasSubmission", hasSubmission);
            item.put("isCreator", Objects.equals(contest.getCreatorId(), uid));
            item.put("isCurrentUser", Objects.equals(currentUserId, uid));
            item.put("problems", problemDetails);
            ranking.add(item);
        }

        ranking.sort((a, b) -> {
            int cmp = Integer.compare((int) b.get("totalScore"), (int) a.get("totalScore"));
            if (cmp != 0) return cmp;
            cmp = Integer.compare((int) b.get("solvedCount"), (int) a.get("solvedCount"));
            if (cmp != 0) return cmp;
            cmp = Long.compare((long) a.get("totalPenalty"), (long) b.get("totalPenalty"));
            if (cmp != 0) return cmp;
            LocalDateTime aTime = (LocalDateTime) a.get("lastSubmissionAt");
            LocalDateTime bTime = (LocalDateTime) b.get("lastSubmissionAt");
            if (aTime == null && bTime == null) return 0;
            if (aTime == null) return 1;
            if (bTime == null) return -1;
            return aTime.compareTo(bTime);
        });

        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).put("rank", i + 1);
        }
        redisCacheService.set(cacheKey, ranking, CONTEST_RANKING_CACHE_TTL);
        return ranking;
    }

    @Override
    @Transactional
    public Contest createContest(Contest contest) {
        if (contest.getStartTime() == null || contest.getEndTime() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "竞赛开始和结束时间不能为空");
        }
        if (!contest.getEndTime().isAfter(contest.getStartTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        if (contest.getType() == null) contest.setType("PRACTICE");
        normalizeContestDuration(contest);
        if (contest.getIsPublic() == null) contest.setIsPublic(1);
        normalizeParticipantLimit(contest);
        if (contest.getAllowPaste() == null) contest.setAllowPaste(1);
        contest.setInviteCode(generateUniqueInviteCode());
        contest.setStatus(computeStatus(contest));
        // 管理员创建的竞赛自动标记为官方
        User creator = userMapper.selectById(contest.getCreatorId());
        if (creator != null && "ADMIN".equals(creator.getRole())) {
            contest.setIsOfficial(1);
        } else if (contest.getIsOfficial() == null) {
            contest.setIsOfficial(0);
        }
        contest.setCreatedAt(LocalDateTime.now());
        contest.setUpdatedAt(LocalDateTime.now());
        this.save(contest);

        ContestRegistration creatorRegistration = new ContestRegistration();
        creatorRegistration.setContestId(contest.getId());
        creatorRegistration.setUserId(contest.getCreatorId());
        creatorRegistration.setRegisteredAt(LocalDateTime.now());
        registrationMapper.insert(creatorRegistration);

        evictContestCaches(contest.getId());
        fillContestAccessInfo(contest, contest.getCreatorId());
        applyViewerSensitiveFields(contest, contest.getCreatorId(), creator != null ? creator.getRole() : null);
        return contest;
    }

    @Override
    @Transactional
    public Contest updateContest(Long contestId, Contest update) {
        Contest contest = this.getById(contestId);
        if (contest == null) throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");

        if (update.getTitle() != null) contest.setTitle(update.getTitle());
        if (update.getDescription() != null) contest.setDescription(update.getDescription());
        if (update.getType() != null) contest.setType(update.getType());
        if (update.getStartTime() != null) contest.setStartTime(update.getStartTime());
        if (update.getEndTime() != null) contest.setEndTime(update.getEndTime());
        if (update.getDurationMinutes() != null) contest.setDurationMinutes(update.getDurationMinutes());
        if (update.getPassword() != null) contest.setPassword(update.getPassword());
        if (update.getMaxParticipants() != null) contest.setMaxParticipants(update.getMaxParticipants());
        if (update.getAllowedLanguages() != null) contest.setAllowedLanguages(update.getAllowedLanguages());
        if (update.getIsPublic() != null) contest.setIsPublic(update.getIsPublic());
        if (update.getAllowPaste() != null) contest.setAllowPaste(update.getAllowPaste());
        if (update.getIsOfficial() != null) contest.setIsOfficial(update.getIsOfficial());
        normalizeContestDuration(contest);
        normalizeParticipantLimit(contest);
        contest.setStatus(computeStatus(contest));
        contest.setUpdatedAt(LocalDateTime.now());
        this.updateById(contest);
        evictContestCaches(contestId);
        applyViewerSensitiveFields(contest, contest.getCreatorId(), resolveUserRole(contest.getCreatorId()));
        return contest;
    }

    @Override
    @Transactional
    public void deleteContest(Long contestId) {
        Contest contest = this.getById(contestId);
        if (contest == null) throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");
        this.removeById(contestId);
        evictContestCaches(contestId);
    }

    @Override
    @Transactional
    public void addProblems(Long contestId, List<ContestProblem> problems, Long userId, String userRole) {
        Contest contest = this.getById(contestId);
        if (contest == null) throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");
        validateContestProblemsAttachable(problems, userId, userRole);
        for (ContestProblem cp : problems) {
            cp.setContestId(contestId);
            ContestProblem existing = contestProblemMapper.selectOne(
                    new LambdaQueryWrapper<ContestProblem>()
                            .eq(ContestProblem::getContestId, contestId)
                            .eq(ContestProblem::getProblemId, cp.getProblemId()));
            if (existing == null) {
                contestProblemMapper.insert(cp);
            }
        }
        evictContestCaches(contestId);
    }

    @Override
    @Transactional
    public void removeProblems(Long contestId, List<Long> problemIds) {
        Contest contest = this.getById(contestId);
        if (contest == null) throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");
        for (Long pid : problemIds) {
            contestProblemMapper.delete(
                    new LambdaQueryWrapper<ContestProblem>()
                            .eq(ContestProblem::getContestId, contestId)
                            .eq(ContestProblem::getProblemId, pid));
        }
        evictContestCaches(contestId);
    }

    @Override
    @Transactional
    public void updateContestStatus(Long contestId, String status) {
        Contest contest = this.getById(contestId);
        if (contest == null) throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");
        contest.setStatus(status);
        contest.setUpdatedAt(LocalDateTime.now());
        this.updateById(contest);
        evictContestCaches(contestId);
    }

    @Override
    public boolean isUserInRunningContestWithProblem(Long userId, Long problemId) {
        List<ContestProblem> cps = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>().eq(ContestProblem::getProblemId, problemId));
        if (cps.isEmpty()) return false;

        List<Long> contestIds = cps.stream().map(ContestProblem::getContestId).distinct().collect(Collectors.toList());

        List<Contest> runningContests = this.list(
                new LambdaQueryWrapper<Contest>()
                        .in(Contest::getId, contestIds)
                        .eq(Contest::getStatus, "RUNNING"));

        for (Contest c : runningContests) {
            if (!"RUNNING".equals(computeStatus(c))) continue;

            if (canAccessContestWorkspace(c, userId)) return true;
        }
        return false;
    }

    @Override
    @Transactional
    public int importFromProblemSet(Long contestId, Long problemSetId, Long userId, String userRole) {
        Contest contest = this.getById(contestId);
        if (contest == null) throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");

        ProblemSet problemSet = problemSetMapper.selectById(problemSetId);
        if (problemSet == null) throw new BusinessException(ResultCode.NOT_FOUND, "题目集不存在");
        if (!Objects.equals(problemSet.getIsPublic(), 1)
                && !"ADMIN".equals(userRole)
                && !Objects.equals(problemSet.getCreatorId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "私有题目集仅创建者或管理员可导入竞赛");
        }

        List<ProblemSetItem> items = problemSetItemMapper.selectList(
                new LambdaQueryWrapper<ProblemSetItem>()
                        .eq(ProblemSetItem::getSetId, problemSetId)
                        .orderByAsc(ProblemSetItem::getSortOrder));
        if (items.isEmpty()) return 0;

        List<Long> problemIds = items.stream()
                .map(ProblemSetItem::getProblemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (problemIds.isEmpty()) return 0;

        Map<Long, Problem> problemMap = problemMapper.selectProblemSummariesByIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity(), (a, b) -> a));

        List<ContestProblem> existingProblems = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>().eq(ContestProblem::getContestId, contestId));
        Set<Long> existingPids = existingProblems.stream()
                .map(ContestProblem::getProblemId)
                .collect(Collectors.toSet());
        int maxOrder = existingProblems.stream()
                .mapToInt(cp -> cp.getSortOrder() != null ? cp.getSortOrder() : 0)
                .max()
                .orElse(0);

        int imported = 0;
        for (ProblemSetItem item : items) {
            Long problemId = item.getProblemId();
            if (problemId == null || existingPids.contains(problemId)) continue;
            Problem problem = problemMap.get(problemId);
            if (problem == null || !canAttachProblemToContest(problem, userId, userRole)) continue;
            ContestProblem cp = new ContestProblem();
            cp.setContestId(contestId);
            cp.setProblemId(problemId);
            cp.setSortOrder(++maxOrder);
            cp.setScore(100);
            contestProblemMapper.insert(cp);
            existingPids.add(problemId);
            imported++;
        }
        if (imported > 0) {
            evictContestCaches(contestId);
        }
        return imported;
    }

    private void validateContestProblemsAttachable(List<ContestProblem> contestProblems, Long userId, String userRole) {
        if (contestProblems == null || contestProblems.isEmpty()) {
            return;
        }
        List<Long> problemIds = contestProblems.stream()
                .map(ContestProblem::getProblemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (problemIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要加入竞赛的题目");
        }

        Map<Long, Problem> problemMap = problemMapper.selectProblemSummariesByIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity(), (a, b) -> a));
        if (problemMap.size() != problemIds.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部分题目不存在或已删除");
        }

        List<Long> deniedPrivateIds = problemMap.values().stream()
                .filter(problem -> !canAttachProblemToContest(problem, userId, userRole))
                .map(Problem::getId)
                .collect(Collectors.toList());
        if (!deniedPrivateIds.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "只能将公开题目或自己创建的私有题目加入竞赛");
        }
    }

    private boolean canAttachProblemToContest(Problem problem, Long userId, String userRole) {
        if (problem == null) {
            return false;
        }
        return "PUBLIC".equals(problem.getVisibility())
                || "ADMIN".equals(userRole)
                || (userId != null && Objects.equals(problem.getAuthorId(), userId));
    }

    @Override
    public Contest joinByInviteCode(Long userId, String inviteCode) {
        Contest contest = this.getOne(
                new LambdaQueryWrapper<Contest>().eq(Contest::getInviteCode, inviteCode));
        if (contest == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "邀请码无效，未找到对应竞赛");
        }
        autoUpdateStatus(contest);
        if ("ENDED".equals(contest.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "竞赛已结束，无法加入");
        }
        if (Objects.equals(contest.getCreatorId(), userId)) {
            fillContestAccessInfo(contest, userId);
            applyViewerSensitiveFields(contest, userId, resolveUserRole(userId));
            return contest;
        }
        ContestRegistration existing = registrationMapper.selectOne(
                new LambdaQueryWrapper<ContestRegistration>()
                        .eq(ContestRegistration::getContestId, contest.getId())
                        .eq(ContestRegistration::getUserId, userId));
        if (existing != null) {
            fillContestAccessInfo(contest, userId);
            applyViewerSensitiveFields(contest, userId, resolveUserRole(userId));
            return contest;
        }
        long count = registrationMapper.selectCount(
                new LambdaQueryWrapper<ContestRegistration>()
                        .eq(ContestRegistration::getContestId, contest.getId()));
        if (hasParticipantLimit(contest) && count >= contest.getMaxParticipants()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报名人数已满");
        }
        ContestRegistration reg = new ContestRegistration();
        reg.setContestId(contest.getId());
        reg.setUserId(userId);
        reg.setRegisteredAt(LocalDateTime.now());
        registrationMapper.insert(reg);
        evictContestCaches(contest.getId());

        notificationService.createNotification(userId, "CONTEST_REGISTER",
                "报名成功：" + contest.getTitle(),
                "你已通过邀请码加入「" + contest.getTitle() + "」，比赛时间：" + contest.getStartTime() + "，请准时参加！",
                contest.getId());
        fillContestAccessInfo(contest, userId);
        applyViewerSensitiveFields(contest, userId, resolveUserRole(userId));
        return contest;
    }

    @Override
    public IPage<Contest> listMyContests(Long userId, String filter, String status, String type, int page, int size) {
        String viewerRole = resolveUserRole(userId);
        if ("created".equals(filter)) {
            QueryWrapper<Contest> wrapper = new QueryWrapper<Contest>()
                    .eq("creator_id", userId)
                    .eq(status != null, "status", status)
                    .eq(type != null, "type", type);
            applyContestListOrder(wrapper);
            IPage<Contest> result = this.page(new Page<>(page, size), wrapper);
            autoUpdateStatuses(result.getRecords());
            batchFillContestInfo(result.getRecords());
            result.getRecords().forEach(c -> {
                fillContestAccessInfo(c, userId);
                applyViewerSensitiveFields(c, userId, viewerRole);
            });
            return result;
        }
        List<ContestRegistration> regs = registrationMapper.selectList(
                new LambdaQueryWrapper<ContestRegistration>().eq(ContestRegistration::getUserId, userId));
        if (regs.isEmpty()) {
            return new Page<>(page, size);
        }
        List<Long> contestIds = regs.stream().map(ContestRegistration::getContestId).collect(Collectors.toList());
        QueryWrapper<Contest> wrapper = new QueryWrapper<Contest>()
                .in("id", contestIds)
                .eq(status != null, "status", status)
                .eq(type != null, "type", type);
        applyContestListOrder(wrapper);
        IPage<Contest> result = this.page(new Page<>(page, size), wrapper);
        autoUpdateStatuses(result.getRecords());
        batchFillContestInfo(result.getRecords());
        result.getRecords().forEach(c -> {
            fillContestAccessInfo(c, userId);
            applyViewerSensitiveFields(c, userId, viewerRole);
        });
        return result;
    }

    @Override
    public boolean canAccessContestWorkspace(Long contestId, Long userId) {
        Contest contest = this.getById(contestId);
        if (contest == null) {
            return false;
        }
        autoUpdateStatus(contest);
        return canAccessContestWorkspace(contest, userId);
    }

    @Override
    public boolean canAccessContestWorkspace(Contest contest, Long userId) {
        if (contest == null || userId == null) {
            return false;
        }
        autoUpdateStatus(contest);
        if (!"RUNNING".equals(contest.getStatus())) {
            return false;
        }
        User user = userMapper.selectById(userId);
        boolean privileged = Objects.equals(contest.getCreatorId(), userId)
                || (user != null && "ADMIN".equals(user.getRole()));
        if (privileged) {
            return true;
        }
        ContestRegistration registration = getOrCreateParticipantRegistration(contest, userId);
        return registration != null && !isParticipantTimeExpired(contest, registration, LocalDateTime.now());
    }

    private void fillContestAccessInfo(Contest contest, Long userId) {
        ContestRegistration registration = getOrCreateParticipantRegistration(contest, userId);
        boolean registered = registration != null;
        boolean isCreator = userId != null && Objects.equals(contest.getCreatorId(), userId);
        User user = userId != null ? userMapper.selectById(userId) : null;
        boolean isAdmin = user != null && "ADMIN".equals(user.getRole());
        boolean privileged = isCreator || isAdmin;
        boolean inviteOnly = isInviteOnly(contest);
        boolean participantExpired = isParticipantTimeExpired(contest, registration, LocalDateTime.now());
        boolean canRegister = userId != null && !registered && !"ENDED".equals(contest.getStatus()) && !inviteOnly;
        // 进行中：已报名用户可访问；已结束：已报名用户和管理员均可回顾题目与提交记录
        boolean canAccessWorkspace = "RUNNING".equals(contest.getStatus())
                && ((registered && !participantExpired) || privileged);
        if ("ENDED".equals(contest.getStatus()) && (registered || privileged)) {
            canAccessWorkspace = true;
        }

        // 仅在比赛进行中时记录首次入场时间
        if (registration != null && canAccessWorkspace && "RUNNING".equals(contest.getStatus())
                && registration.getEnteredAt() == null) {
            touchContestParticipation(registration, false);
        }

        contest.setRegistered(registered);
        contest.setPrivilegedParticipant(privileged && (!registered || participantExpired));
        contest.setCanRegister(canRegister);
        contest.setCanAccessWorkspace(canAccessWorkspace);
        contest.setParticipantTimeExpired(participantExpired);
        fillParticipantDeadline(contest, registration);
        if (isCreator && !registered) {
            contest.setRegistered(true);
        }
    }

    private ContestRegistration getOrCreateParticipantRegistration(Contest contest, Long userId) {
        if (contest == null || userId == null) {
            return null;
        }
        ContestRegistration registration = registrationMapper.selectOne(
                new LambdaQueryWrapper<ContestRegistration>()
                        .eq(ContestRegistration::getContestId, contest.getId())
                        .eq(ContestRegistration::getUserId, userId));
        if (registration != null) {
            return registration;
        }
        if (!Objects.equals(contest.getCreatorId(), userId)) {
            return null;
        }
        ContestRegistration creatorRegistration = new ContestRegistration();
        creatorRegistration.setContestId(contest.getId());
        creatorRegistration.setUserId(userId);
        creatorRegistration.setRegisteredAt(firstNonNull(contest.getCreatedAt(), LocalDateTime.now()));
        registrationMapper.insert(creatorRegistration);
        evictContestCaches(contest.getId());
        return creatorRegistration;
    }

    private List<ContestRegistration> ensureCreatorRegistration(Contest contest, List<ContestRegistration> registrations) {
        boolean hasCreator = registrations.stream().anyMatch(reg -> Objects.equals(reg.getUserId(), contest.getCreatorId()));
        if (hasCreator) {
            return registrations;
        }
        ContestRegistration creatorRegistration = getOrCreateParticipantRegistration(contest, contest.getCreatorId());
        if (creatorRegistration == null) {
            return registrations;
        }
        List<ContestRegistration> merged = new ArrayList<>(registrations);
        merged.add(creatorRegistration);
        merged.sort(Comparator.comparing(ContestRegistration::getRegisteredAt, Comparator.nullsLast(LocalDateTime::compareTo)));
        return merged;
    }

    private void touchContestParticipation(ContestRegistration registration, boolean hasSubmission) {
        boolean changed = false;
        LocalDateTime now = LocalDateTime.now();
        if (registration.getEnteredAt() == null) {
            registration.setEnteredAt(now);
            changed = true;
        }
        if (hasSubmission) {
            registration.setLastSubmissionAt(now);
            changed = true;
        }
        if (changed) {
            registrationMapper.updateById(registration);
            evictContestCaches(registration.getContestId());
        }
    }

    private boolean shouldIncludeRankingEntry(String filter, boolean entered, boolean hasSubmission, Long currentUserId, Long rankingUserId) {
        if (Objects.equals(currentUserId, rankingUserId)) {
            return true;
        }
        if ("all".equals(filter)) {
            return true;
        }
        if ("submitted".equals(filter)) {
            return hasSubmission;
        }
        return entered || hasSubmission;
    }

    private boolean isFinalContestSubmission(Submission submission) {
        if (submission == null || submission.getStatus() == null) {
            return false;
        }
        return !"PENDING".equals(submission.getStatus()) && !"RUNNING".equals(submission.getStatus());
    }

    private int bestContestProblemScore(List<Submission> submissions, ContestProblem contestProblem) {
        if (submissions == null || submissions.isEmpty()) {
            return 0;
        }
        int problemScore = contestProblem != null && contestProblem.getScore() != null
                ? contestProblem.getScore()
                : 100;
        int bestPercent = submissions.stream()
                .map(Submission::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        bestPercent = Math.max(0, Math.min(100, bestPercent));
        return Math.max(0, Math.min(problemScore, (int) Math.round(bestPercent * problemScore / 100.0)));
    }

    private String normalizeRankingFilter(String filter) {
        if (filter == null) {
            return "active";
        }
        String value = filter.trim().toLowerCase(Locale.ROOT);
        if ("all".equals(value) || "submitted".equals(value) || "active".equals(value)) {
            return value;
        }
        return "active";
    }

    private Map<Long, String> loadAvatarMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, UserProfile> profileMap = userProfileMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));
        return userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Function.identity(),
                        userId -> {
                            UserProfile profile = profileMap.get(userId);
                            return profile != null && profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()
                                    ? profile.getAvatarUrl()
                                    : "/avatars/avatar-1.svg";
                        }
                ));
    }

    private Map<Long, User> loadUserSummaryMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(
                        new LambdaQueryWrapper<User>()
                                .select(User::getId, User::getUsername, User::getRole)
                                .in(User::getId, ids))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private LocalDateTime firstNonNull(LocalDateTime first, LocalDateTime second) {
        return first != null ? first : second;
    }

    private void applyContestListOrder(QueryWrapper<Contest> wrapper) {
        wrapper.orderByAsc("CASE WHEN end_time IS NOT NULL AND end_time < NOW() THEN 1 ELSE 0 END")
                .orderByDesc("COALESCE(is_official, 0)")
                .orderByDesc("id");
    }

    private void normalizeParticipantLimit(Contest contest) {
        if (contest != null && contest.getMaxParticipants() != null && contest.getMaxParticipants() <= 0) {
            contest.setMaxParticipants(null);
        }
    }

    private void normalizeContestDuration(Contest contest) {
        if (contest == null) {
            return;
        }
        if (!"EXAM".equals(contest.getType()) && isOpenEndedContest(contest)) {
            contest.setDurationMinutes(0);
            return;
        }
        if (contest.getDurationMinutes() == null && contest.getStartTime() != null && contest.getEndTime() != null) {
            long minutes = Duration.between(contest.getStartTime(), contest.getEndTime()).toMinutes();
            contest.setDurationMinutes((int) Math.max(0, Math.min(Integer.MAX_VALUE, minutes)));
        }
    }

    private boolean isOpenEndedContest(Contest contest) {
        return contest != null
                && contest.getEndTime() != null
                && contest.getEndTime().getYear() >= 2099;
    }

    private boolean hasParticipantLimit(Contest contest) {
        return contest != null && contest.getMaxParticipants() != null && contest.getMaxParticipants() > 0;
    }

    private boolean isInviteOnly(Contest contest) {
        return contest != null && contest.getIsPublic() != null && contest.getIsPublic() == 0;
    }

    private String resolveUserRole(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getRole();
    }

    private boolean isContestManager(Contest contest, Long userId, String userRole) {
        return contest != null
                && userId != null
                && (Objects.equals(contest.getCreatorId(), userId) || "ADMIN".equals(userRole));
    }

    private boolean isContestParticipant(Long contestId, Long userId) {
        if (contestId == null || userId == null) {
            return false;
        }
        return registrationMapper.selectCount(new LambdaQueryWrapper<ContestRegistration>()
                .eq(ContestRegistration::getContestId, contestId)
                .eq(ContestRegistration::getUserId, userId)) > 0;
    }

    private void assertCanViewContest(Contest contest, Long userId) {
        if (contest == null || !isInviteOnly(contest)) {
            return;
        }
        String userRole = resolveUserRole(userId);
        if (isContestManager(contest, userId, userRole) || isContestParticipant(contest.getId(), userId)) {
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "该竞赛为非公开竞赛，请通过邀请码加入后查看");
    }

    private void applyViewerSensitiveFields(Contest contest, Long viewerId, String viewerRole) {
        if (contest == null) {
            return;
        }
        boolean canManage = isContestManager(contest, viewerId, viewerRole);
        contest.setCanManage(canManage);
        if (!canManage) {
            contest.setInviteCode(null);
        }
    }

    private LocalDateTime getParticipantDeadline(Contest contest, ContestRegistration registration) {
        if (contest == null || registration == null || registration.getEnteredAt() == null) {
            return null;
        }
        if (!"EXAM".equals(contest.getType()) || contest.getDurationMinutes() == null || contest.getDurationMinutes() <= 0) {
            return null;
        }
        return registration.getEnteredAt().plusMinutes(contest.getDurationMinutes());
    }

    private boolean isParticipantTimeExpired(Contest contest, ContestRegistration registration, LocalDateTime now) {
        LocalDateTime deadline = getParticipantDeadline(contest, registration);
        return deadline != null && now.isAfter(deadline);
    }

    private void fillParticipantDeadline(Contest contest, ContestRegistration registration) {
        LocalDateTime deadline = getParticipantDeadline(contest, registration);
        contest.setParticipantDeadline(deadline);
        if (deadline == null) {
            contest.setParticipantRemainingSeconds(null);
            return;
        }
        long remainingSeconds = Math.max(0, Duration.between(LocalDateTime.now(), deadline).getSeconds());
        contest.setParticipantRemainingSeconds(remainingSeconds);
    }

    private String generateUniqueInviteCode() {
        for (int i = 0; i < 10; i++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            Contest existing = this.getOne(
                    new LambdaQueryWrapper<Contest>().eq(Contest::getInviteCode, code));
            if (existing == null) return code;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String computeStatus(Contest contest) {
        LocalDateTime now = LocalDateTime.now();
        if (contest == null || contest.getStartTime() == null) return "DRAFT";
        if (now.isBefore(contest.getStartTime())) return "UPCOMING";
        if (contest.getEndTime() == null || isOpenEndedContest(contest)) return "RUNNING";
        if (now.isAfter(contest.getEndTime())) return "ENDED";
        return "RUNNING";
    }

    private void autoUpdateStatus(Contest contest) {
        String newStatus = computeStatus(contest);
        if (!newStatus.equals(contest.getStatus()) && !"DRAFT".equals(contest.getStatus())) {
            contest.setStatus(newStatus);
            contest.setUpdatedAt(LocalDateTime.now());
            this.updateById(contest);
        }
    }

    private void autoUpdateStatuses(List<Contest> contests) {
        for (Contest c : contests) {
            autoUpdateStatus(c);
        }
    }

    private void batchFillContestInfo(List<Contest> contests) {
        if (contests.isEmpty()) return;

        List<Long> contestIds = contests.stream().map(Contest::getId).collect(Collectors.toList());
        Set<Long> creatorIds = contests.stream().map(Contest::getCreatorId).collect(Collectors.toSet());

        Map<Long, User> creatorMap = loadUserSummaryMap(creatorIds);

        List<ContestProblem> allCp = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .select(ContestProblem::getContestId, ContestProblem::getProblemId)
                        .in(ContestProblem::getContestId, contestIds));
        Map<Long, Long> problemCountMap = allCp.stream()
                .collect(Collectors.groupingBy(ContestProblem::getContestId, Collectors.counting()));

        List<ContestRegistration> allRegs = registrationMapper.selectList(
                new LambdaQueryWrapper<ContestRegistration>()
                        .select(ContestRegistration::getContestId, ContestRegistration::getUserId)
                        .in(ContestRegistration::getContestId, contestIds));
        Map<Long, Long> participantCountMap = allRegs.stream()
                .collect(Collectors.groupingBy(ContestRegistration::getContestId, Collectors.counting()));

        for (Contest c : contests) {
            User creator = creatorMap.get(c.getCreatorId());
            if (creator != null) {
                c.setCreatorName(creator.getUsername());
                // 管理员创建的竞赛自动标记为官方
                if ("ADMIN".equals(creator.getRole()) && !Integer.valueOf(1).equals(c.getIsOfficial())) {
                    c.setIsOfficial(1);
                }
            }
            c.setProblemCount(problemCountMap.getOrDefault(c.getId(), 0L).intValue());
            c.setParticipantCount(participantCountMap.getOrDefault(c.getId(), 0L).intValue());
        }
    }

    private String buildContestListCacheKey(String status, String type, int page, int size, Long viewerId) {
        return "contest:list:"
                + (status == null ? "all" : status)
                + ":"
                + (type == null ? "all" : type)
                + ":"
                + page
                + ":"
                + size
                + ":viewer:"
                + (viewerId == null ? "guest" : viewerId);
    }

    private String buildContestDetailCacheKey(Long contestId) {
        return "contest:detail:" + contestId;
    }

    private String buildContestProblemsCacheKey(Long contestId) {
        return "contest:problems:" + contestId;
    }

    private String buildContestRankingCacheKey(Long contestId) {
        return "contest:ranking:" + contestId;
    }
}

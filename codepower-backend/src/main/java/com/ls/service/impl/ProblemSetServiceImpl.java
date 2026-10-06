/**
 * 文件说明：题目集 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.ls.service.ProblemSetService;
import com.ls.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 题目集服务实现 */
@Service
@RequiredArgsConstructor
public class ProblemSetServiceImpl extends ServiceImpl<ProblemSetMapper, ProblemSet> implements ProblemSetService {

    private final ProblemSetItemMapper itemMapper;
    private final ProblemMapper problemMapper;
    private final UserMapper userMapper;
    private final RedisCacheService redisCacheService;
    private static final Duration PROBLEM_SET_LIST_REDIS_TTL = Duration.ofHours(2);
    private static final Duration PROBLEM_SET_DETAIL_REDIS_TTL = Duration.ofHours(6);
    private static final Duration PROBLEM_SET_ITEMS_REDIS_TTL = Duration.ofHours(6);
    private final Cache<String, IPage<ProblemSet>> problemSetListCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(20000)
            .build();
    private final Cache<Long, List<ProblemSetItem>> problemSetItemsCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(20000)
            .build();
    private static final String ACCESS_AVAILABLE = "AVAILABLE";
    private static final String ACCESS_INACCESSIBLE = "INACCESSIBLE";

    @Override
    @SuppressWarnings("unchecked")
    public IPage<ProblemSet> listProblemSets(String type, Boolean mine, Boolean onlyPublic,
                                             int page, int size, Long userId, String userRole) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        String cacheKey = buildProblemSetListCacheKey(type, mine, onlyPublic, safePage, safeSize, userId, userRole);
        IPage<ProblemSet> cached = problemSetListCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        Page<ProblemSet> redisCached = redisCacheService.get(cacheKey, Page.class);
        if (redisCached != null) {
            problemSetListCache.put(cacheKey, redisCached);
            return redisCached;
        }

        LambdaQueryWrapper<ProblemSet> wrapper = new LambdaQueryWrapper<ProblemSet>()
                .orderByDesc(ProblemSet::getCreatedAt);

        if (type != null && !type.isBlank()) {
            wrapper.eq(ProblemSet::getType, type);
        }

        boolean queryMine = Boolean.TRUE.equals(mine);
        boolean queryOnlyPublic = Boolean.TRUE.equals(onlyPublic);

        if (queryMine) {
            if (userId == null) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
            }
            wrapper.eq(ProblemSet::getCreatorId, userId);
        } else if (queryOnlyPublic) {
            wrapper.eq(ProblemSet::getIsPublic, 1);
        } else {
            wrapper.and(w -> w
                    .eq(ProblemSet::getIsPublic, 1)
                    .or(userId != null, o -> o.eq(ProblemSet::getCreatorId, userId)));
        }

        IPage<ProblemSet> result = this.page(new Page<>(safePage, safeSize), wrapper);
        batchFillCreatorName(result.getRecords());
        normalizeVisibleProblemCounts(result.getRecords(), userId, userRole);
        problemSetListCache.put(cacheKey, result);
        redisCacheService.set(cacheKey, result, PROBLEM_SET_LIST_REDIS_TTL);
        return result;
    }

    @Override
    public ProblemSet getDetail(Long id) {
        String cacheKey = buildProblemSetDetailCacheKey(id);
        ProblemSet cached = redisCacheService.get(cacheKey, ProblemSet.class);
        if (cached != null) {
            return cached;
        }
        ProblemSet ps = this.getById(id);
        if (ps == null) throw new BusinessException(ResultCode.NOT_FOUND, "题目集不存在");
        batchFillCreatorName(List.of(ps));
        redisCacheService.set(cacheKey, ps, PROBLEM_SET_DETAIL_REDIS_TTL);
        return ps;
    }

    @Override
    public ProblemSet getDetail(Long id, Long userId, String userRole) {
        ProblemSet ps = getDetail(id);
        checkViewPermission(ps, userId, userRole);
        if (canManageProblemSet(ps, userId, userRole)) {
            return ps;
        }
        ProblemSet view = copyProblemSet(ps);
        view.setProblemCount(getVisibleProblemSetItemsForViewer(id, userId, userRole).size());
        return view;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProblemSetItem> getItems(Long setId) {
        List<ProblemSetItem> cached = problemSetItemsCache.getIfPresent(setId);
        if (cached != null) {
            return cached;
        }
        String cacheKey = buildProblemSetItemsCacheKey(setId);
        List<ProblemSetItem> redisCached = redisCacheService.get(cacheKey, List.class);
        if (redisCached != null) {
            problemSetItemsCache.put(setId, redisCached);
            return redisCached;
        }

        ProblemSet ps = this.getById(setId);
        if (ps == null) throw new BusinessException(ResultCode.NOT_FOUND, "题目集不存在");

        List<ProblemSetItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ProblemSetItem>()
                        .eq(ProblemSetItem::getSetId, setId)
                        .orderByAsc(ProblemSetItem::getSortOrder));

        if (!items.isEmpty()) {
            List<Long> problemIds = items.stream()
                    .map(ProblemSetItem::getProblemId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, Problem> problemMap = problemIds.isEmpty() ? Collections.emptyMap() :
                    problemMapper.selectProblemSummariesByIds(problemIds).stream()
                            .collect(Collectors.toMap(Problem::getId, p -> p));
            items.forEach(item -> {
                Problem p = problemMap.get(item.getProblemId());
                if (p != null) {
                    item.setProblemTitle(p.getTitle());
                    item.setDifficulty(p.getDifficulty());
                    item.setVisibility(p.getVisibility());
                    item.setProblemAuthorId(p.getAuthorId());
                }
            });
        }
        problemSetItemsCache.put(setId, items);
        redisCacheService.set(cacheKey, items, PROBLEM_SET_ITEMS_REDIS_TTL);
        return items;
    }

    @Override
    public List<ProblemSetItem> getItems(Long setId, Long userId, String userRole) {
        ProblemSet ps = this.getById(setId);
        if (ps == null) throw new BusinessException(ResultCode.NOT_FOUND, "题目集不存在");
        checkViewPermission(ps, userId, userRole);
        List<ProblemSetItem> items = annotateProblemSetItems(getItems(setId), userId, userRole);
        if (canManageProblemSet(ps, userId, userRole)) {
            return items;
        }
        return items.stream()
                .filter(item -> ACCESS_AVAILABLE.equals(item.getAccessStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProblemSet create(ProblemSet problemSet, Long userId, String userRole, int userLevel) {
        if ("PRIVATE".equals(problemSet.getType())) {
            if (!"ADMIN".equals(userRole) && !"SENIOR_USER".equals(userRole)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "创建私人题目集需要高级用户（Lv.2+）权限");
            }
            if (!"ADMIN".equals(userRole) && userLevel < 2) {
                throw new BusinessException(ResultCode.FORBIDDEN, "创建私人题目集需要至少 Lv.2");
            }
        } else if ("OFFICIAL".equals(problemSet.getType())) {
            if (!"ADMIN".equals(userRole)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只有管理员可以创建官方题目集");
            }
        } else {
            problemSet.setType("PRIVATE");
        }

        problemSet.setCreatorId(userId);
        problemSet.setProblemCount(0);
        if (problemSet.getIsPublic() == null) problemSet.setIsPublic(1);
        problemSet.setCreatedAt(LocalDateTime.now());
        problemSet.setUpdatedAt(LocalDateTime.now());
        this.save(problemSet);
        evictProblemSetListCache();
        return problemSet;
    }

    @Override
    @Transactional
    public ProblemSet update(Long id, ProblemSet update, Long userId, String userRole) {
        ProblemSet ps = this.getById(id);
        if (ps == null) throw new BusinessException(ResultCode.NOT_FOUND, "题目集不存在");
        checkPermission(ps, userId, userRole);

        if (update.getTitle() != null) ps.setTitle(update.getTitle());
        if (update.getDescription() != null) ps.setDescription(update.getDescription());
        if (update.getIsPublic() != null) {
            if (Objects.equals(update.getIsPublic(), 1)) {
                assertProblemSetContainsOnlyPublicProblems(id);
            }
            ps.setIsPublic(update.getIsPublic());
        }
        ps.setUpdatedAt(LocalDateTime.now());
        this.updateById(ps);
        evictProblemSetListCache();
        evictProblemSetDetailCache(id);
        return ps;
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId, String userRole) {
        ProblemSet ps = this.getById(id);
        if (ps == null) throw new BusinessException(ResultCode.NOT_FOUND, "题目集不存在");
        checkPermission(ps, userId, userRole);
        this.removeById(id);
        evictProblemSetListCache();
        evictProblemSetDetailCache(id);
        evictProblemSetItemsCache(id);
    }

    @Override
    @Transactional
    public void addItems(Long setId, List<ProblemSetItem> items, Long userId, String userRole) {
        ProblemSet ps = this.getById(setId);
        if (ps == null) throw new BusinessException(ResultCode.NOT_FOUND, "题目集不存在");
        checkPermission(ps, userId, userRole);

        int currentMax = 0;
        List<ProblemSetItem> existing = itemMapper.selectList(
                new LambdaQueryWrapper<ProblemSetItem>().eq(ProblemSetItem::getSetId, setId));
        Set<Long> existingIds = existing.stream().map(ProblemSetItem::getProblemId).collect(Collectors.toSet());
        if (!existing.isEmpty()) {
            currentMax = existing.stream().mapToInt(i -> i.getSortOrder() != null ? i.getSortOrder() : 0).max().orElse(0);
        }

        List<Long> newProblemIds = items.stream()
                .map(ProblemSetItem::getProblemId)
                .filter(pid -> !existingIds.contains(pid))
                .collect(Collectors.toList());
        Map<Long, Problem> problemMap = newProblemIds.isEmpty() ? Collections.emptyMap() :
                problemMapper.selectProblemSummariesByIds(newProblemIds).stream()
                        .collect(Collectors.toMap(Problem::getId, p -> p));

        int added = 0;
        for (ProblemSetItem item : items) {
            if (existingIds.contains(item.getProblemId())) continue;
            Problem problem = problemMap.get(item.getProblemId());
            if (problem == null) continue;
            boolean isPublicProblem = "PUBLIC".equals(problem.getVisibility());
            if (Objects.equals(ps.getIsPublic(), 1) && !isPublicProblem) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "公开题目集只能包含公开题目，请先将私有题逐题设为公开，或改用私有题目集");
            }
            if (!isPublicProblem && !"ADMIN".equals(userRole) && !Objects.equals(problem.getAuthorId(), userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能添加公开题目或自己创建的私有题目");
            }
            item.setSetId(setId);
            if (item.getSortOrder() == null) item.setSortOrder(++currentMax);
            item.setAddedAt(LocalDateTime.now());
            itemMapper.insert(item);
            existingIds.add(item.getProblemId());
            added++;
        }

        ps.setProblemCount(ps.getProblemCount() + added);
        ps.setUpdatedAt(LocalDateTime.now());
        this.updateById(ps);
        if (added > 0) {
            evictProblemSetListCache();
            evictProblemSetDetailCache(setId);
            evictProblemSetItemsCache(setId);
        }
    }

    @Override
    @Transactional
    public void removeItems(Long setId, List<Long> problemIds, Long userId, String userRole) {
        ProblemSet ps = this.getById(setId);
        if (ps == null) throw new BusinessException(ResultCode.NOT_FOUND, "题目集不存在");
        checkPermission(ps, userId, userRole);

        int removed = 0;
        for (Long pid : problemIds) {
            int r = itemMapper.delete(new LambdaQueryWrapper<ProblemSetItem>()
                    .eq(ProblemSetItem::getSetId, setId)
                    .eq(ProblemSetItem::getProblemId, pid));
            removed += r;
        }
        ps.setProblemCount(Math.max(0, ps.getProblemCount() - removed));
        ps.setUpdatedAt(LocalDateTime.now());
        this.updateById(ps);
        if (removed > 0) {
            evictProblemSetListCache();
            evictProblemSetDetailCache(setId);
            evictProblemSetItemsCache(setId);
        }
    }

    @EventListener
    public void handleProblemCacheEvictEvent(ProblemCacheEvictEvent event) {
        problemSetItemsCache.invalidateAll();
        redisCacheService.deleteByPattern("problem-set:items:*");
    }

    private void checkPermission(ProblemSet ps, Long userId, String userRole) {
        if ("ADMIN".equals(userRole)) return;
        if (!ps.getCreatorId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此题目集");
        }
    }

    private void checkViewPermission(ProblemSet ps, Long userId, String userRole) {
        if (Objects.equals(ps.getIsPublic(), 1)) return;
        if ("ADMIN".equals(userRole)) return;
        if (userId != null && Objects.equals(ps.getCreatorId(), userId)) return;
        throw new BusinessException(ResultCode.FORBIDDEN, "私有题目集仅创建者或管理员可查看");
    }

    private boolean canManageProblemSet(ProblemSet ps, Long userId, String userRole) {
        return "ADMIN".equals(userRole) || (userId != null && Objects.equals(ps.getCreatorId(), userId));
    }

    private List<ProblemSetItem> getVisibleProblemSetItemsForViewer(Long setId, Long userId, String userRole) {
        return annotateProblemSetItems(getItems(setId), userId, userRole).stream()
                .filter(item -> ACCESS_AVAILABLE.equals(item.getAccessStatus()))
                .collect(Collectors.toList());
    }

    private List<ProblemSetItem> annotateProblemSetItems(List<ProblemSetItem> items, Long userId, String userRole) {
        return items.stream()
                .map(item -> {
                    ProblemSetItem view = copyProblemSetItem(item);
                    boolean accessible = canAccessProblemReference(view, userId, userRole);
                    view.setAccessStatus(accessible ? ACCESS_AVAILABLE : ACCESS_INACCESSIBLE);
                    view.setInvalidReason(accessible ? null : buildInvalidProblemReferenceReason(view));
                    return view;
                })
                .collect(Collectors.toList());
    }

    private boolean canAccessProblemReference(ProblemSetItem item, Long userId, String userRole) {
        if (item == null || item.getProblemId() == null || item.getVisibility() == null) {
            return false;
        }
        if ("PUBLIC".equals(item.getVisibility())) {
            return true;
        }
        return "ADMIN".equals(userRole)
                || (userId != null && Objects.equals(item.getProblemAuthorId(), userId));
    }

    private boolean canAccessProblem(Problem problem, Long userId, String userRole) {
        if (problem == null) {
            return false;
        }
        return "PUBLIC".equals(problem.getVisibility())
                || "ADMIN".equals(userRole)
                || (userId != null && Objects.equals(problem.getAuthorId(), userId));
    }

    private String buildInvalidProblemReferenceReason(ProblemSetItem item) {
        if (item == null || item.getVisibility() == null) {
            return "题目不存在或已删除";
        }
        if ("PRIVATE".equals(item.getVisibility())) {
            return "题目已转为私有，当前账号无权访问";
        }
        return "当前账号无权访问该题目";
    }

    private ProblemSetItem copyProblemSetItem(ProblemSetItem source) {
        ProblemSetItem copy = new ProblemSetItem();
        copy.setSetId(source.getSetId());
        copy.setProblemId(source.getProblemId());
        copy.setSortOrder(source.getSortOrder());
        copy.setAddedAt(source.getAddedAt());
        copy.setProblemTitle(source.getProblemTitle());
        copy.setDifficulty(source.getDifficulty());
        copy.setVisibility(source.getVisibility());
        copy.setProblemAuthorId(source.getProblemAuthorId());
        copy.setAccessStatus(source.getAccessStatus());
        copy.setInvalidReason(source.getInvalidReason());
        return copy;
    }

    private ProblemSet copyProblemSet(ProblemSet source) {
        ProblemSet copy = new ProblemSet();
        copy.setId(source.getId());
        copy.setTitle(source.getTitle());
        copy.setDescription(source.getDescription());
        copy.setType(source.getType());
        copy.setCreatorId(source.getCreatorId());
        copy.setIsPublic(source.getIsPublic());
        copy.setProblemCount(source.getProblemCount());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setDeleted(source.getDeleted());
        copy.setCreatorName(source.getCreatorName());
        return copy;
    }

    private void assertProblemSetContainsOnlyPublicProblems(Long setId) {
        List<ProblemSetItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ProblemSetItem>()
                        .eq(ProblemSetItem::getSetId, setId));
        if (items.isEmpty()) return;

        List<Long> problemIds = items.stream()
                .map(ProblemSetItem::getProblemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (problemIds.isEmpty()) return;

        List<Problem> problems = problemMapper.selectProblemSummariesByIds(problemIds);
        List<Long> privateIds = problems.stream()
                .filter(problem -> !"PUBLIC".equals(problem.getVisibility()))
                .map(Problem::getId)
                .collect(Collectors.toList());
        if (!privateIds.isEmpty()) {
            String preview = privateIds.stream()
                    .limit(5)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            String suffix = privateIds.size() > 5 ? " 等" : "";
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "题目集中仍有私有题（#" + preview + suffix + "），公开前请先逐题调整为公开状态");
        }
    }

    private void batchFillCreatorName(List<ProblemSet> sets) {
        if (sets.isEmpty()) return;
        Set<Long> creatorIds = sets.stream().map(ProblemSet::getCreatorId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        sets.forEach(ps -> {
            User u = userMap.get(ps.getCreatorId());
            if (u != null) ps.setCreatorName(u.getUsername());
        });
    }

    private void normalizeVisibleProblemCounts(List<ProblemSet> sets, Long userId, String userRole) {
        if (sets.isEmpty()) return;
        List<Long> setIds = sets.stream()
                .filter(ps -> !canManageProblemSet(ps, userId, userRole))
                .map(ProblemSet::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (setIds.isEmpty()) return;

        List<ProblemSetItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ProblemSetItem>()
                        .select(ProblemSetItem::getSetId, ProblemSetItem::getProblemId)
                        .in(ProblemSetItem::getSetId, setIds));
        if (items.isEmpty()) {
            sets.stream()
                    .filter(ps -> setIds.contains(ps.getId()))
                    .forEach(ps -> ps.setProblemCount(0));
            return;
        }

        List<Long> problemIds = items.stream()
                .map(ProblemSetItem::getProblemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Problem> problemMap = problemIds.isEmpty() ? Collections.emptyMap() :
                problemMapper.selectProblemSummariesByIds(problemIds).stream()
                        .collect(Collectors.toMap(Problem::getId, problem -> problem, (a, b) -> a));

        Map<Long, Long> visibleCountMap = items.stream()
                .filter(item -> canAccessProblem(problemMap.get(item.getProblemId()), userId, userRole))
                .collect(Collectors.groupingBy(ProblemSetItem::getSetId, Collectors.counting()));
        sets.stream()
                .filter(ps -> setIds.contains(ps.getId()))
                .forEach(ps -> ps.setProblemCount(visibleCountMap.getOrDefault(ps.getId(), 0L).intValue()));
    }

    private void evictProblemSetListCache() {
        problemSetListCache.invalidateAll();
        redisCacheService.deleteByPattern("problem-set:list:*");
    }

    private void evictProblemSetDetailCache(Long setId) {
        redisCacheService.delete(buildProblemSetDetailCacheKey(setId));
    }

    private void evictProblemSetItemsCache(Long setId) {
        problemSetItemsCache.invalidate(setId);
        redisCacheService.delete(buildProblemSetItemsCacheKey(setId));
    }

    private String buildProblemSetListCacheKey(String type, Boolean mine, Boolean onlyPublic,
                                               int page, int size, Long userId, String userRole) {
        return String.join(":",
                "problem-set:list",
                type == null || type.isBlank() ? "all" : type.trim(),
                String.valueOf(Boolean.TRUE.equals(mine)),
                String.valueOf(Boolean.TRUE.equals(onlyPublic)),
                String.valueOf(page),
                String.valueOf(size),
                userId == null ? "anon" : String.valueOf(userId),
                userRole == null || userRole.isBlank() ? "guest" : userRole);
    }

    private String buildProblemSetDetailCacheKey(Long setId) {
        return "problem-set:detail:" + setId;
    }

    private String buildProblemSetItemsCacheKey(Long setId) {
        return "problem-set:items:v2:" + setId;
    }
}

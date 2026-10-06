/**
 * 文件说明：题目 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ls.domain.Problem;
import com.ls.domain.Tag;
import com.ls.event.ProblemCacheEvictEvent;
import com.ls.mapper.ProblemMapper;
import com.ls.mapper.TagMapper;
import com.ls.mapper.TestCaseMapper;
import com.ls.service.ProblemService;
import com.ls.service.RedisCacheService;
import com.ls.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 问题服务实现类
 * @author ls
 */
@Slf4j
@Service
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, Problem> implements ProblemService {

    private static final Duration PROBLEM_DETAIL_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration PROBLEM_VIEW_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration PROBLEM_LIST_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration PROBLEM_AUTHOR_CACHE_TTL = Duration.ofHours(6);
    private static final Duration PROBLEM_TAG_CACHE_TTL = Duration.ofHours(6);
    private static final Duration PUBLIC_PROBLEM_PAGE_REDIS_TTL = Duration.ofHours(6);
    private static final long PUBLIC_PROBLEM_PAGE_LOCAL_MAX_WEIGHT = 64L * 1024L * 1024L;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final Cache<String, Page<Problem>> publicProblemPageCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumWeight(PUBLIC_PROBLEM_PAGE_LOCAL_MAX_WEIGHT)
            .weigher((String key, Page<Problem> page) -> estimateProblemPageWeight(key, page))
            .build();

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private TagService tagService;

    @Override
    public Page<Problem> getProblems(int current, int size, Map<String, Object> params) {
        int safeCurrent = Math.max(1, current);
        int safeSize = Math.min(Math.max(1, size), 500);
        Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params;

        String title = normalizeFilterText(firstParamValue(safeParams, "title", "search"));
        String difficulty = normalizeFilterText(firstParamValue(safeParams, "difficulty"));
        Long problemId = normalizeProblemId(firstParamValue(safeParams, "problemId", "id"));
        List<String> tagNames = normalizeTagNames(firstParamValue(safeParams, "tagNames", "tagName", "tags", "category"));
        String cacheKey = buildPublicProblemPageCacheKey(safeCurrent, safeSize, title, difficulty, tagNames, problemId);

        Page<Problem> localCached = publicProblemPageCache.getIfPresent(cacheKey);
        if (localCached != null) {
            return localCached;
        }

        Page<Problem> redisCached = redisCacheService.get(cacheKey, Page.class);
        if (redisCached != null) {
            publicProblemPageCache.put(cacheKey, redisCached);
            return redisCached;
        }

        Page<Problem> page = new Page<>(safeCurrent, safeSize);
        IPage<Problem> result = tagNames.isEmpty()
                ? baseMapper.selectPublicProblemsWithoutTags(page, title, difficulty, problemId)
                : baseMapper.selectPublicProblemsByTags(page, tagNames, tagNames.size(), title, difficulty, problemId);
        fillProblemTags(result.getRecords());

        Page<Problem> resultPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        resultPage.setRecords(result.getRecords());
        publicProblemPageCache.put(cacheKey, resultPage);
        redisCacheService.set(cacheKey, resultPage, PUBLIC_PROBLEM_PAGE_REDIS_TTL);

        return resultPage;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProblems(Integer page, Integer size, String difficulty, String tagName, String search) {
        int safePage = Math.max(1, page == null ? 1 : page);
        int safeSize = Math.min(Math.max(1, size == null ? 10 : size), 100);
        String cacheKey = buildProblemListCacheKey(safePage, safeSize, difficulty, tagName, search);
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }

        Map<String, Object> params = new HashMap<>();
        if (difficulty != null) params.put("difficulty", difficulty);
        if (tagName != null) params.put("tagNames", tagName);
        if (search != null) params.put("title", search);
        Page<Problem> problemPage = getProblems(safePage, safeSize, params);

        Map<String, Object> result = new HashMap<>();
        result.put("total", problemPage.getTotal());
        result.put("pages", problemPage.getPages());
        result.put("current", problemPage.getCurrent());
        result.put("records", problemPage.getRecords());
        redisCacheService.set(cacheKey, result, PROBLEM_LIST_CACHE_TTL);
        return result;
    }

    @Override
    public Problem getProblemById(Long id) {
        String cacheKey = buildProblemDetailCacheKey(id);
        Problem cached = redisCacheService.get(cacheKey, Problem.class);
        if (cached != null) {
            return cached;
        }
        Problem problem = baseMapper.selectProblemWithTags(id);
        if (problem != null) {
            redisCacheService.set(cacheKey, problem, PROBLEM_DETAIL_CACHE_TTL);
        }
        return problem;
    }

    @Override
    public Problem getProblemForSolving(Long id) {
        String cacheKey = buildProblemViewEntityCacheKey(id);
        Problem cached = redisCacheService.get(cacheKey, Problem.class);
        if (cached != null) {
            return cached;
        }
        Problem problem = baseMapper.selectProblemSolvingViewWithTags(id);
        if (problem != null) {
            redisCacheService.set(cacheKey, problem, PROBLEM_VIEW_CACHE_TTL);
        }
        return problem;
    }

    @Override
    public Map<String, Object> getProblemSummaryForView(Long id) {
        String cacheKey = buildProblemViewCacheKey(id);
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }

        Problem problem = getProblemForSolving(id);
        if (problem == null) {
            return null;
        }

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
        response.put("timeLimit", problem.getTimeLimit());
        response.put("memoryLimit", problem.getMemoryLimit());
        response.put("authorId", problem.getAuthorId());
        response.put("authorName", problem.getAuthorName());
        response.put("tags", problem.getTags());
        response.put("submitCount", problem.getSubmitCount());
        response.put("acceptRate", problem.getAcceptRate());
        response.put("acceptCount", problem.getAcceptCount());

        redisCacheService.set(cacheKey, response, PROBLEM_VIEW_CACHE_TTL);
        return response;
    }

    @Override
    @Transactional
    public boolean createProblem(Problem problem, List<String> tagNames) {
        try {
            boolean saved = this.save(problem);
            if (!saved) {
                return false;
            }

            attachProblemTags(problem.getId(), tagNames);

            evictProblemCaches(problem.getId());
            return true;
        } catch (Exception e) {
            log.error("创建问题失败: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean updateProblem(Problem problem, List<String> tagNames) {
        try {
            boolean updated = this.updateById(problem);
            if (!updated) {
                return false;
            }

            tagMapper.deleteProblemTags(problem.getId());
            attachProblemTags(problem.getId(), tagNames);

            evictProblemCaches(problem.getId());
            return true;
        } catch (Exception e) {
            log.error("更新问题失败: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public Problem updateProblem(Long id, Problem problem, Long userId) {
        Problem existingProblem = this.getById(id);

        if (existingProblem == null) {
            return null;
        }

        if (!existingProblem.getAuthorId().equals(userId)) {
            return null;
        }

        problem.setId(id);
        this.updateById(problem);
        evictProblemCaches(id);

        return this.getById(id);
    }

    @Override
    public boolean updateProblemVisibility(Long id, String visibility) {
        LambdaUpdateWrapper<Problem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Problem::getId, id)
                .set(Problem::getVisibility, visibility);

        boolean updated = this.update(updateWrapper);
        if (updated) {
            evictProblemCaches(id);
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean deleteProblem(Long id) {
        try {
            LambdaUpdateWrapper<Problem> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Problem::getId, id)
                    .set(Problem::getDeleted, 1);

            boolean deleted = this.update(updateWrapper);
            if (deleted) {
                evictProblemCaches(id);
            }
            return deleted;
        } catch (Exception e) {
            log.error("删除问题失败: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean deleteProblem(Long id, Long userId) {
        Problem problem = this.getById(id);

        if (problem == null) {
            return false;
        }

        if (!problem.getAuthorId().equals(userId)) {
            return false;
        }

        return this.deleteProblem(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Problem> getProblemsByAuthor(Long authorId) {
        String cacheKey = "problem:author:" + authorId + ":all";
        List<Problem> cached = redisCacheService.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }
        List<Problem> problems = baseMapper.selectProblemsByAuthor(authorId);
        fillProblemTags(problems);
        redisCacheService.set(cacheKey, problems, PROBLEM_AUTHOR_CACHE_TTL);
        return problems;
    }

    @Override
    public List<Problem> getUserCreatedProblems(Long userId) {
        return this.getProblemsByAuthor(userId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Problem> getProblemsByTag(String tagName) {
        String normalizedTagName = tagName == null ? "" : tagName.trim();
        String cacheKey = "problem:tag:" + (normalizedTagName.isEmpty() ? "all" : normalizedTagName);
        List<Problem> cached = redisCacheService.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }
        List<Problem> problems = baseMapper.selectProblemsByTag(normalizedTagName);
        fillProblemTags(problems);
        redisCacheService.set(cacheKey, problems, PROBLEM_TAG_CACHE_TTL);
        return problems;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Problem> getPublicProblemsByAuthor(Long authorId) {
        String cacheKey = "problem:author:" + authorId + ":public";
        List<Problem> cached = redisCacheService.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }
        List<Problem> problems = baseMapper.selectPublicProblemsByAuthor(authorId);
        fillProblemTags(problems);
        redisCacheService.set(cacheKey, problems, PROBLEM_AUTHOR_CACHE_TTL);
        return problems;
    }

    private void evictProblemCaches(Long problemId) {
        evictProblemListCaches();
        redisCacheService.deleteKeys(List.of(
                buildProblemDetailCacheKey(problemId),
                buildProblemViewEntityCacheKey(problemId),
                buildProblemViewCacheKey(problemId)
        ));
    }

    private List<Map<String, Object>> parseProblemExamples(Problem problem) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String raw = problem.getExamples();
            if (raw != null && !raw.isBlank()) {
                List<Map<String, Object>> parsed = JSON_MAPPER.readValue(raw, new TypeReference<>() {});
                for (Map<String, Object> item : parsed) {
                    String input = stringValue(firstPresent(item, "input", "inputExample", "stdin"));
                    String output = stringValue(firstPresent(item, "output", "expectedOutput", "outputExample"));
                    String explanation = stringValue(firstPresent(item, "explanation", "explain", "description"));
                    if (!input.isBlank() || !output.isBlank()) {
                        result.add(exampleMap(input, output, explanation));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析题目公开样例失败: problemId={}, error={}", problem.getId(), e.getMessage());
        }
        if (result.isEmpty() && hasSampleContent(problem.getInputExample(), problem.getOutputExample())) {
            result.add(exampleMap(problem.getInputExample(), problem.getOutputExample(), ""));
        }
        if (hasSampleContent(problem.getDebugInputExample(), problem.getDebugOutputExample())) {
            String debugInput = stringValue(problem.getDebugInputExample());
            String debugOutput = stringValue(problem.getDebugOutputExample());
            boolean duplicated = result.stream().anyMatch(example ->
                    stringValue(example.get("input")).equals(debugInput)
                            && stringValue(example.get("output")).equals(debugOutput));
            if (!duplicated) {
                result.add(exampleMap(debugInput, debugOutput, ""));
            }
        }
        return result;
    }

    private Map<String, Object> exampleMap(String input, String output, String explanation) {
        Map<String, Object> item = new HashMap<>();
        item.put("input", stringValue(input));
        item.put("output", stringValue(output));
        item.put("explanation", stringValue(explanation));
        return item;
    }

    private Object firstPresent(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    private boolean hasSampleContent(String input, String output) {
        return !stringValue(input).isBlank() || !stringValue(output).isBlank();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @EventListener
    public void handleProblemCacheEvictEvent(ProblemCacheEvictEvent event) {
        evictProblemListCaches();
        if (event.includeProblemDetailCaches()) {
            redisCacheService.deleteByPattern("problem:detail:*");
            redisCacheService.deleteByPattern("problem:view-entity:*");
            redisCacheService.deleteByPattern("problem:view:*");
        }
    }

    private void evictProblemListCaches() {
        publicProblemPageCache.invalidateAll();
        redisCacheService.deleteByPattern("problem:list:*");
        redisCacheService.deleteByPattern("problem:public-page:*");
        redisCacheService.deleteByPattern("problem:author:*");
        redisCacheService.deleteByPattern("problem:tag:*");
    }

    private void fillProblemTags(List<Problem> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        List<Long> problemIds = records.stream()
                .map(Problem::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (problemIds.isEmpty()) {
            return;
        }

        Map<Long, List<Tag>> tagMap = tagService.getTagsMapByProblemIds(problemIds);
        records.forEach(problem -> problem.setTags(
                tagMap.getOrDefault(problem.getId(), Collections.emptyList())));
    }

    private void attachProblemTags(Long problemId, List<String> tagNames) {
        if (problemId == null || tagNames == null || tagNames.isEmpty()) {
            return;
        }

        List<String> normalizedNames = tagNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (normalizedNames.isEmpty()) {
            return;
        }

        List<Long> tagIds = tagService.getOrCreateTagsByNames(normalizedNames);
        for (Long tagId : tagIds) {
            tagMapper.insertProblemTag(problemId, tagId);
        }
    }

    private String buildProblemDetailCacheKey(Long problemId) {
        return "problem:detail:" + problemId;
    }

    private String buildProblemViewCacheKey(Long problemId) {
        return "problem:view:" + problemId;
    }

    private String buildProblemViewEntityCacheKey(Long problemId) {
        return "problem:view-entity:" + problemId;
    }

    private String buildProblemListCacheKey(Integer page, Integer size, String difficulty, String tagName, String search) {
        return "problem:list:"
                + (page == null ? 1 : page)
                + ":"
                + (size == null ? 10 : size)
                + ":"
                + (difficulty == null || difficulty.isEmpty() ? "all" : difficulty)
                + ":"
                + (tagName == null || tagName.isEmpty() ? "all" : tagName)
                + ":"
                + (search == null || search.isEmpty() ? "all" : search);
    }

    private String buildPublicProblemPageCacheKey(int current,
                                                  int size,
                                                  String title,
                                                  String difficulty,
                                                  List<String> tagNames,
                                                  Long problemId) {
        return "problem:public-page:"
                + current
                + ":"
                + size
                + ":"
                + (problemId == null ? "all" : problemId)
                + ":"
                + (title == null || title.isEmpty() ? "all" : title)
                + ":"
                + (difficulty == null || difficulty.isEmpty() ? "all" : difficulty)
                + ":"
                + (tagNames == null || tagNames.isEmpty() ? "all" : String.join(",", tagNames));
    }

    private String firstParamValue(Map<String, Object> params, String... keys) {
        for (String key : keys) {
            Object value = params.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private String normalizeFilterText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long normalizeProblemId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<String> normalizeTagNames(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private static int estimateProblemPageWeight(String key, Page<Problem> page) {
        long weight = key == null ? 0 : key.length() * 2L;
        weight += 512;
        if (page != null && page.getRecords() != null) {
            weight += 128L * page.getRecords().size();
            for (Problem problem : page.getRecords()) {
                weight += estimateProblemWeight(problem);
            }
        }
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, weight));
    }

    private static long estimateProblemWeight(Problem problem) {
        if (problem == null) {
            return 0;
        }
        long weight = 512;
        weight += estimateTextWeight(problem.getTitle());
        weight += estimateTextWeight(problem.getDifficulty());
        weight += estimateTextWeight(problem.getDescription());
        weight += estimateTextWeight(problem.getInputFormat());
        weight += estimateTextWeight(problem.getOutputFormat());
        weight += estimateTextWeight(problem.getInputExample());
        weight += estimateTextWeight(problem.getOutputExample());
        weight += estimateTextWeight(problem.getDebugInputExample());
        weight += estimateTextWeight(problem.getDebugOutputExample());
        weight += estimateTextWeight(problem.getHint());
        weight += estimateTextWeight(problem.getSolution());
        weight += estimateTextWeight(problem.getVisibility());
        weight += estimateTextWeight(problem.getAuthorName());
        if (problem.getTags() != null) {
            weight += 64L * problem.getTags().size();
            for (Tag tag : problem.getTags()) {
                weight += tag == null ? 0 : estimateTextWeight(tag.getName());
            }
        }
        return weight;
    }

    private static long estimateTextWeight(String value) {
        return value == null ? 0 : value.length() * 2L;
    }
}

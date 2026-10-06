/**
 * 文件说明：标签 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ls.domain.Tag;
import com.ls.event.ProblemCacheEvictEvent;
import com.ls.mapper.ProblemTagMapper;
import com.ls.mapper.TagMapper;
import com.ls.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 标签服务实现类
 * @author ls
 * @since 2024-07-01
 */
@Slf4j
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ProblemTagMapper problemTagMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // L1 本地缓存：标签列表极少变动，缓存10分钟避免重复查库
    private final Cache<String, List<Tag>> tagListCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1)
            .build();

    private static final String ALL_TAGS_KEY = "ALL";

    @Override
    public List<Tag> getAllTags() {
        List<Tag> cached = tagListCache.getIfPresent(ALL_TAGS_KEY);
        if (cached != null) {
            return cached;
        }
        List<Tag> tags = list();
        tagListCache.put(ALL_TAGS_KEY, tags);
        log.debug("标签列表缓存已刷新，共 {} 个标签", tags.size());
        return tags;
    }

    @Override
    public Tag getTagById(Long id) {
        return getById(id);
    }

    @Override
    public Tag getTagByName(String name) {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getName, name);
        return getOne(queryWrapper);
    }

    @Override
    public Tag createTag(Tag tag) {
        save(tag);
        evictTagRelatedCaches();
        return tag;
    }

    @Override
    public boolean updateTag(Tag tag) {
        boolean result = updateById(tag);
        if (result) evictTagRelatedCaches();
        return result;
    }

    @Override
    public boolean deleteTag(Long id) {
        boolean result = removeById(id);
        if (result) evictTagRelatedCaches();
        return result;
    }

    @Override
    public List<Tag> getTagsByProblemId(Long problemId) {
        return tagMapper.selectTagsByProblemId(problemId);
    }

    @Override
    public Map<Long, List<Tag>> getTagsMapByProblemIds(List<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return Map.of();
        }
        
        Map<Long, List<Tag>> result = new HashMap<>();
        
        // 初始化每个问题ID对应的空标签列表
        problemIds.forEach(id -> result.put(id, new ArrayList<>()));
        
        // 查询所有关联的标签
        List<Map<String, Object>> relations = problemTagMapper.selectProblemTagRelations(problemIds);
        
        if (relations.isEmpty()) {
            return result;
        }
        
        // 收集所有标签ID
        List<Long> tagIds = relations.stream()
                .map(m -> ((Number) m.get("tag_id")).longValue())
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询标签
        List<Tag> allTags = listByIds(tagIds);
        
        // 构建标签ID到标签对象的映射
        Map<Long, Tag> tagMap = allTags.stream()
                .collect(Collectors.toMap(Tag::getId, t -> t));
        
        // 组装结果
        relations.forEach(relation -> {
            Long problemId = ((Number) relation.get("problem_id")).longValue();
            Long tagId = ((Number) relation.get("tag_id")).longValue();
            
            Tag tag = tagMap.get(tagId);
            if (tag != null) {
                result.get(problemId).add(tag);
            }
        });
        
        return result;
    }

    @Override
    @Transactional
    public boolean setTagsForProblem(Long problemId, List<Long> tagIds) {
        // 先删除原有的关联
        problemTagMapper.deleteByProblemId(problemId);
        
        // 如果tagIds为空，则直接返回成功
        if (tagIds == null || tagIds.isEmpty()) {
            eventPublisher.publishEvent(ProblemCacheEvictEvent.all());
            return true;
        }
        
        // 建立新的关联
        boolean result = problemTagMapper.insertBatch(problemId, tagIds) > 0;
        if (result) {
            eventPublisher.publishEvent(ProblemCacheEvictEvent.all());
        }
        return result;
    }

    @Override
    @Transactional
    public List<Long> getOrCreateTagsByNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }

        List<Long> tagIds = new ArrayList<>();
        boolean created = false;

        for (String name : tagNames) {
            // 查找是否已存在
            Tag existingTag = getTagByName(name);

            if (existingTag != null) {
                tagIds.add(existingTag.getId());
            } else {
                // 创建新标签
                Tag newTag = new Tag();
                newTag.setName(name);
                save(newTag);
                tagIds.add(newTag.getId());
                created = true;
            }
        }

        if (created) evictTagRelatedCaches();
        return tagIds;
    }

    private void evictTagRelatedCaches() {
        tagListCache.invalidateAll();
        eventPublisher.publishEvent(ProblemCacheEvictEvent.all());
    }
} 

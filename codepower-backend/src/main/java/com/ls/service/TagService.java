/**
 * 文件说明：标签 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ls.domain.Tag;

import java.util.List;
import java.util.Map;

/**
 * 标签服务接口
 * @author ls
 * @since 2024-07-01
 */
public interface TagService extends IService<Tag> {
    
    /**
     * 获取所有标签
     * @return 标签列表
     */
    List<Tag> getAllTags();
    
    /**
     * 获取标签详情
     * @param id 标签ID
     * @return 标签对象
     */
    Tag getTagById(Long id);
    
    /**
     * 根据名称获取标签
     * @param name 标签名称
     * @return 标签对象
     */
    Tag getTagByName(String name);
    
    /**
     * 创建新标签
     * @param tag 标签对象
     * @return 创建的标签对象
     */
    Tag createTag(Tag tag);
    
    /**
     * 更新标签
     * @param tag 标签对象
     * @return 是否更新成功
     */
    boolean updateTag(Tag tag);
    
    /**
     * 删除标签
     * @param id 标签ID
     * @return 是否删除成功
     */
    boolean deleteTag(Long id);
    
    /**
     * 获取问题的所有标签
     * @param problemId 问题ID
     * @return 标签列表
     */
    List<Tag> getTagsByProblemId(Long problemId);
    
    /**
     * 批量获取多个问题的标签
     * @param problemIds 问题ID列表
     * @return 问题ID到标签列表的映射
     */
    Map<Long, List<Tag>> getTagsMapByProblemIds(List<Long> problemIds);
    
    /**
     * 为问题设置标签
     * @param problemId 问题ID
     * @param tagIds 标签ID列表
     * @return 是否设置成功
     */
    boolean setTagsForProblem(Long problemId, List<Long> tagIds);
    
    /**
     * 根据标签名获取或创建标签
     * @param tagNames 标签名列表
     * @return 标签ID列表
     */
    List<Long> getOrCreateTagsByNames(List<String> tagNames);
} 
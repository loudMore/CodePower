/**
 * 文件说明：题目集 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.domain.ProblemSet;
import com.ls.domain.ProblemSetItem;

import java.util.List;
import java.util.Map;

/** 题目集服务 */
public interface ProblemSetService {
    /** 分页查询题目集列表 */
    IPage<ProblemSet> listProblemSets(String type, Boolean mine, Boolean onlyPublic, int page, int size, Long userId, String userRole);
    /** 获取题目集详情 */
    ProblemSet getDetail(Long id);
    /** 获取题目集详情（带可见性校验） */
    ProblemSet getDetail(Long id, Long userId, String userRole);
    /** 获取题目集中的题目列表 */
    List<ProblemSetItem> getItems(Long setId);
    /** 获取题目集中的题目列表（带可见性校验） */
    List<ProblemSetItem> getItems(Long setId, Long userId, String userRole);
    /** 创建题目集 */
    ProblemSet create(ProblemSet problemSet, Long userId, String userRole, int userLevel);
    /** 更新题目集信息 */
    ProblemSet update(Long id, ProblemSet problemSet, Long userId, String userRole);
    /** 删除题目集 */
    void delete(Long id, Long userId, String userRole);
    /** 向题目集中批量添加题目 */
    void addItems(Long setId, List<ProblemSetItem> items, Long userId, String userRole);
    /** 从题目集中批量移除题目 */
    void removeItems(Long setId, List<Long> problemIds, Long userId, String userRole);
}

/**
 * 文件说明：题目 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ls.domain.Problem;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * 问题服务接口
 * @author ls
 * @since 2024-07-01
 */
public interface ProblemService extends IService<Problem> {

    /**
     * 分页获取问题列表
     * @param current 当前页
     * @param size 每页大小
     * @param params 查询参数
     * @return 分页问题列表
     */
    Page<Problem> getProblems(int current, int size, Map<String, Object> params);

    /**
     * 分页获取问题列表(带过滤)
     * @param page 当前页
     * @param size 每页大小
     * @param difficulty 难度过滤
     * @param tagName 标签过滤
     * @param search 标题搜索
     * @return 问题列表及分页信息
     */
    Map<String, Object> getProblems(Integer page, Integer size, String difficulty, String tagName, String search);

    /**
     * 根据ID获取问题详情
     * @param id 问题ID
     * @return 问题对象
     */
    Problem getProblemById(Long id);

    Problem getProblemForSolving(Long id);

    Map<String, Object> getProblemSummaryForView(Long id);

    /**
     * 创建新问题
     * @param problem 问题对象
     * @param tagNames 标签名称列表
     * @return 是否创建成功
     */
    boolean createProblem(Problem problem, List<String> tagNames);

    /**
     * 更新问题
     * @param problem 问题对象
     * @param tagNames 标签名称列表
     * @return 是否更新成功
     */
    boolean updateProblem(Problem problem, List<String> tagNames);
    
    /**
     * 更新问题（带权限检查）
     * @param id 问题ID
     * @param problem 问题对象
     * @param userId 当前用户ID
     * @return 更新后的问题对象
     */
    Problem updateProblem(Long id, Problem problem, Long userId);

    /**
     * 更新问题可见性
     * @param id 问题ID
     * @param visibility 可见性
     * @return 是否更新成功
     */
    boolean updateProblemVisibility(Long id, String visibility);

    /**
     * 删除问题
     * @param id 问题ID
     * @return 是否删除成功
     */
    boolean deleteProblem(Long id);
    
    /**
     * 删除问题（带权限检查）
     * @param id 问题ID
     * @param userId 当前用户ID
     * @return 是否删除成功
     */
    boolean deleteProblem(Long id, Long userId);

    /**
     * 获取用户创建的问题
     * @param authorId 作者ID
     * @return 问题列表
     */
    List<Problem> getProblemsByAuthor(Long authorId);
    
    /**
     * 获取用户创建的问题
     * @param userId 用户ID
     * @return 问题列表
     */
    List<Problem> getUserCreatedProblems(Long userId);

    /**
     * 根据标签名获取问题
     * @param tagName 标签名
     * @return 问题列表
     */
    List<Problem> getProblemsByTag(String tagName);
    
    /**
     * 获取用户创建的公开问题
     * @param authorId 作者ID
     * @return 公开问题列表
     */
    List<Problem> getPublicProblemsByAuthor(Long authorId);
} 

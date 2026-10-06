/**
 * 文件说明：题解 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ls.domain.ProblemSolution;

import java.util.List;
import java.util.Map;

/**
 * 问题题解服务接口
 * @author ls
 * @since 2024-07-04
 */
public interface ProblemSolutionService extends IService<ProblemSolution> {
    
    /**
     * 根据问题ID获取题解列表
     * @param problemId 问题ID
     * @return 题解列表
     */
    List<ProblemSolution> getSolutionsByProblemId(Long problemId);
    
    /**
     * 获取问题的官方题解
     * @param problemId 问题ID
     * @return 官方题解列表
     */
    List<ProblemSolution> getOfficialSolutionsByProblemId(Long problemId);
    
    /**
     * 获取问题的社区题解（非官方题解）
     * @param problemId 问题ID
     * @return 社区题解列表
     */
    List<ProblemSolution> getCommunitySolutionsByProblemId(Long problemId);
    
    /**
     * 获取问题官方题解代码
     * @param problemId 问题ID
     * @return 语言-代码映射
     */
    Map<String, String> getOfficialSolutionCodeByProblemId(Long problemId);
    
    /**
     * 根据问题ID和语言获取题解
     * @param problemId 问题ID
     * @param language 编程语言
     * @return 题解列表
     */
    List<ProblemSolution> getSolutionsByProblemIdAndLanguage(Long problemId, String language);
    
    /**
     * 根据用户ID获取题解列表
     * @param userId 用户ID
     * @return 题解列表
     */
    List<ProblemSolution> getSolutionsByUserId(Long userId);
    
    /**
     * 创建题解
     * @param solution 题解对象
     * @return 是否创建成功
     */
    boolean createSolution(ProblemSolution solution);
    
    /**
     * 更新题解
     * @param solution 题解对象
     * @return 是否更新成功
     */
    boolean updateSolution(ProblemSolution solution);
    
    /**
     * 删除题解
     * @param id 题解ID
     * @return 是否删除成功
     */
    boolean deleteSolution(Long id);
    
    /**
     * 批量创建官方题解
     * @param problemId 问题ID
     * @param userId 用户ID
     * @param solutionsMap 题解映射(语言->代码)
     * @return 是否成功
     */
    boolean createOfficialSolutions(Long problemId, Long userId, Map<String, String> solutionsMap);
} 
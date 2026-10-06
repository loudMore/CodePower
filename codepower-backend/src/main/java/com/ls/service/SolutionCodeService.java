/**
 * 文件说明：题解代码 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ls.domain.SolutionCode;

import java.util.List;
import java.util.Map;

/**
 * 题解代码服务接口
 * @author ls
 */
public interface SolutionCodeService extends IService<SolutionCode> {
    
    /**
     * 根据问题ID查询所有题解代码
     * @param problemId 问题ID
     * @return 题解代码列表
     */
    List<SolutionCode> getByProblemId(Long problemId);
    
    /**
     * 根据问题ID和语言查询题解代码
     * @param problemId 问题ID
     * @param language 编程语言
     * @return 题解代码
     */
    SolutionCode getByProblemIdAndLanguage(Long problemId, String language);
    
    /**
     * 创建官方题解代码
     * @param problemId 问题ID
     * @param userId 用户ID
     * @param solutionMap 题解映射(语言->代码)
     * @return 是否成功
     */
    boolean createOfficialSolutions(Long problemId, Long userId, Map<String, String> solutionMap);
    
    /**
     * 删除问题的所有题解代码
     * @param problemId 问题ID
     * @return 是否成功
     */
    boolean deleteByProblemId(Long problemId);
} 
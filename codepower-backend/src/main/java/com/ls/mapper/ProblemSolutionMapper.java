/**
 * 文件说明：题解 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.ProblemSolution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 问题题解Mapper接口
 * @author ls
 * @since 2024-07-04
 */
@Mapper
public interface ProblemSolutionMapper extends BaseMapper<ProblemSolution> {
    
    /**
     * 根据问题ID查询所有题解
     * @param problemId 问题ID
     * @return 题解列表
     */
    @Select("SELECT * FROM problem_solutions WHERE problem_id = #{problemId} AND status = 1 AND deleted = 0 ORDER BY is_official DESC, likes DESC, created_at DESC")
    List<ProblemSolution> selectByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 根据问题ID和语言查询题解
     * @param problemId 问题ID
     * @param language 编程语言
     * @return 题解列表
     */
    @Select("SELECT * FROM problem_solutions WHERE problem_id = #{problemId} AND language = #{language} AND status = 1 AND deleted = 0 ORDER BY is_official DESC, likes DESC, created_at DESC")
    List<ProblemSolution> selectByProblemIdAndLanguage(@Param("problemId") Long problemId, @Param("language") String language);
    
    /**
     * 根据问题ID查询官方题解
     * @param problemId 问题ID
     * @return 官方题解列表
     */
    @Select("SELECT * FROM problem_solutions WHERE problem_id = #{problemId} AND is_official = 1 AND status = 1 AND deleted = 0 ORDER BY language")
    List<ProblemSolution> selectOfficialByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 根据用户ID查询用户创建的题解
     * @param userId 用户ID
     * @return 题解列表
     */
    @Select("SELECT * FROM problem_solutions WHERE user_id = #{userId} AND status = 1 AND deleted = 0 ORDER BY created_at DESC")
    List<ProblemSolution> selectByUserId(@Param("userId") Long userId);
} 

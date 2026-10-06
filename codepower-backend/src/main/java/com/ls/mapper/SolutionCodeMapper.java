/**
 * 文件说明：题解代码 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.SolutionCode;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题解代码Mapper接口
 * @author ls
 */
@Mapper
public interface SolutionCodeMapper extends BaseMapper<SolutionCode> {
    
    /**
     * 根据问题ID查询所有题解代码
     * @param problemId 问题ID
     * @return 题解代码列表
     */
    @Select("SELECT * FROM solution_codes WHERE problem_id = #{problemId}")
    List<SolutionCode> selectByProblemId(Long problemId);
    
    /**
     * 根据问题ID和语言查询题解代码
     * @param problemId 问题ID
     * @param language 编程语言
     * @return 题解代码
     */
    @Select("SELECT * FROM solution_codes WHERE problem_id = #{problemId} AND language = #{language}")
    SolutionCode selectByProblemIdAndLanguage(@Param("problemId") Long problemId, @Param("language") String language);
    
    /**
     * 根据问题ID删除所有题解代码
     * @param problemId 问题ID
     * @return 影响行数
     */
    @Delete("DELETE FROM solution_codes WHERE problem_id = #{problemId}")
    int deleteByProblemId(Long problemId);

    /**
     * 查询问题的官方题解代码
     * @param problemId 问题ID
     * @return 官方题解代码列表
     */
    @Select("SELECT * FROM solution_codes WHERE problem_id = #{problemId} AND is_official = 1")
    List<SolutionCode> selectOfficialSolutionsByProblemId(Long problemId);
} 
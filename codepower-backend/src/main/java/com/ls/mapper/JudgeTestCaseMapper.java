/**
 * 文件说明：评测测试点 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.JudgeTestCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 评测用例 Mapper */
@Mapper
public interface JudgeTestCaseMapper extends BaseMapper<JudgeTestCase> {

    /**
     * 根据问题ID查询所有测试用例
     */
    @Select("SELECT * FROM judge_test_cases WHERE problem_id = #{problemId} ORDER BY order_num ASC")
    List<JudgeTestCase> findByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 根据问题ID查询可见测试用例
     */
    @Select("SELECT * FROM judge_test_cases WHERE problem_id = #{problemId} AND is_hidden = 0 ORDER BY order_num ASC")
    List<JudgeTestCase> findVisibleByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 查询问题的测试用例数量
     */
    @Select("SELECT COUNT(*) FROM judge_test_cases WHERE problem_id = #{problemId}")
    Integer countByProblemId(@Param("problemId") Long problemId);
} 
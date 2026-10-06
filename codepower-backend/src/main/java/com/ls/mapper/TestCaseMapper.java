/**
 * 文件说明：T es tC as e 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.JudgeTestCase;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 测试用例Mapper接口
 * @author ls
 */
@Mapper
public interface TestCaseMapper extends BaseMapper<JudgeTestCase> {
    
    /**
     * 根据问题ID查询测试用例
     * @param problemId 问题ID
     * @return 测试用例列表
     */
    @Select("SELECT * FROM judge_test_cases WHERE problem_id = #{problemId} ORDER BY order_num ASC")
    List<JudgeTestCase> selectByProblemId(Long problemId);
    
    /**
     * 根据问题ID删除所有测试用例
     * @param problemId 问题ID
     * @return 影响行数
     */
    @Delete("DELETE FROM judge_test_cases WHERE problem_id = #{problemId}")
    int deleteByProblemId(Long problemId);
} 
/**
 * 文件说明：测试点提交结果 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.SubmissionResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 提交测试结果 Mapper */
@Mapper
public interface SubmissionResultMapper extends BaseMapper<SubmissionResult> {

    /**
     * 根据提交ID查询测试结果
     */
    @Select("SELECT * FROM submission_results WHERE submission_id = #{submissionId}")
    List<SubmissionResult> findBySubmissionId(@Param("submissionId") Long submissionId);
    
    /**
     * 查询提交通过的测试用例数量
     */
    @Select("SELECT COUNT(*) FROM submission_results WHERE submission_id = #{submissionId} AND status = 'ACCEPTED'")
    Integer countPassedTestCases(@Param("submissionId") Long submissionId);
    
    /**
     * 查询提交的总测试用例数量
     */
    @Select("SELECT COUNT(*) FROM submission_results WHERE submission_id = #{submissionId}")
    Integer countTotalTestCases(@Param("submissionId") Long submissionId);
} 
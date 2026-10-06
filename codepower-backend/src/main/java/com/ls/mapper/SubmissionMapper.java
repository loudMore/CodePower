/**
 * 文件说明：提交记录 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.domain.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 提交记录 Mapper */
@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    /**
     * 根据用户ID查询提交记录
     */
    @Select("SELECT * FROM submissions WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Submission> findByUserId(IPage<Submission> page, @Param("userId") Long userId);
    
    /**
     * 根据问题ID查询提交记录
     */
    @Select("SELECT * FROM submissions WHERE problem_id = #{problemId} ORDER BY created_at DESC")
    List<Submission> findByProblemId(IPage<Submission> page, @Param("problemId") Long problemId);
    
    /**
     * 根据用户ID和问题ID查询提交记录
     */
    @Select("SELECT * FROM submissions WHERE user_id = #{userId} AND problem_id = #{problemId} ORDER BY created_at DESC")
    List<Submission> findByUserIdAndProblemId(IPage<Submission> page, @Param("userId") Long userId, @Param("problemId") Long problemId);
    
    /**
     * 查询用户解决的问题数量
     */
    @Select("SELECT COUNT(DISTINCT problem_id) FROM submissions WHERE user_id = #{userId} AND status = 'ACCEPTED' AND contest_id IS NULL")
    Integer countAcceptedProblemsByUserId(@Param("userId") Long userId);
    
    /**
     * 查询问题的通过人数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM submissions WHERE problem_id = #{problemId} AND status = 'ACCEPTED'")
    Integer countUsersByProblemIdAndAccepted(@Param("problemId") Long problemId);
} 
/**
 * 文件说明：用户题目首通记录 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.UserProblemSolveRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/** 用户做题记录 Mapper */
@Mapper
public interface UserProblemSolveRecordMapper extends BaseMapper<UserProblemSolveRecord> {

    @Insert("INSERT IGNORE INTO user_problem_solve_records (user_id, problem_id, first_accepted_submission_id, first_accepted_at, latest_accepted_submission_id, latest_accepted_at, accepted_count) " +
            "VALUES (#{userId}, #{problemId}, #{submissionId}, #{acceptedAt}, #{submissionId}, #{acceptedAt}, 1)")
    int insertIgnore(@Param("userId") Long userId,
                     @Param("problemId") Long problemId,
                     @Param("submissionId") Long submissionId,
                     @Param("acceptedAt") LocalDateTime acceptedAt);

    @Update("UPDATE user_problem_solve_records SET latest_accepted_submission_id = #{submissionId}, latest_accepted_at = #{acceptedAt}, accepted_count = accepted_count + 1 " +
            "WHERE user_id = #{userId} AND problem_id = #{problemId}")
    int touchAccepted(@Param("userId") Long userId,
                      @Param("problemId") Long problemId,
                      @Param("submissionId") Long submissionId,
                      @Param("acceptedAt") LocalDateTime acceptedAt);
}

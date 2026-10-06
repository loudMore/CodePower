/**
 * 文件说明：题目收藏 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.FavoriteProblem;
import com.ls.domain.Problem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

/**
 * 用户收藏问题 Mapper 接口
 * 
 * @author ls
 * @since 2025-07-01
 */
@Mapper
public interface FavoriteProblemMapper extends BaseMapper<FavoriteProblem> {

    /**
     * 查询用户收藏的所有问题ID
     */
    @Select("SELECT problem_id FROM favorite_problems WHERE user_id = #{userId}")
    List<Long> findProblemIdsByUserId(@Param("userId") Long userId);
    
    /**
     * 查询收藏了某问题的所有用户ID
     */
    @Select("SELECT user_id FROM favorite_problems WHERE problem_id = #{problemId}")
    List<Long> findUserIdsByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 检查用户是否已收藏某问题
     */
    @Select("SELECT COUNT(*) FROM favorite_problems WHERE user_id = #{userId} AND problem_id = #{problemId}")
    int checkFavorite(@Param("userId") Long userId, @Param("problemId") Long problemId);
    
    /**
     * 查询用户收藏的所有问题
     */
    @Select("SELECT p.* FROM problems p JOIN favorite_problems fp ON p.id = fp.problem_id WHERE fp.user_id = #{userId}")
    List<Problem> findProblemsByUserId(@Param("userId") Long userId);
    
    /**
     * 添加收藏
     */
    @Insert("INSERT INTO favorite_problems (user_id, problem_id) VALUES (#{userId}, #{problemId})")
    int addFavorite(@Param("userId") Long userId, @Param("problemId") Long problemId);
    
    /**
     * 取消收藏
     */
    @Delete("DELETE FROM favorite_problems WHERE user_id = #{userId} AND problem_id = #{problemId}")
    int removeFavorite(@Param("userId") Long userId, @Param("problemId") Long problemId);
} 
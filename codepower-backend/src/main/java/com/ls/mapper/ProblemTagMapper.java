/**
 * 文件说明：题目标签关系 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.ProblemTag;
import com.ls.domain.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 问题-标签关联Mapper接口
 * @author ls
 * @since 2025-07-01
 */
@Mapper
public interface ProblemTagMapper extends BaseMapper<ProblemTag> {
    
    /**
     * 为问题添加标签
     * @param problemTag 问题-标签关联对象
     * @return 影响行数
     */
    @Insert("INSERT INTO problem_tags(problem_id, tag_id) VALUES(#{problemId}, #{tagId})")
    int insert(ProblemTag problemTag);
    
    /**
     * 批量插入问题-标签关联
     * @param problemId 问题ID
     * @param tagIds 标签ID列表
     * @return 影响行数
     */
    @Insert("<script>" +
            "INSERT INTO problem_tags(problem_id, tag_id) VALUES " +
            "<foreach collection='tagIds' item='tagId' separator=','>" +
            "(#{problemId}, #{tagId})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("problemId") Long problemId, @Param("tagIds") List<Long> tagIds);
    
    /**
     * 删除问题的所有标签关联
     * @param problemId 问题ID
     * @return 影响行数
     */
    @Delete("DELETE FROM problem_tags WHERE problem_id = #{problemId}")
    int deleteByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 删除问题的特定标签关联
     * @param problemId 问题ID
     * @param tagId 标签ID
     * @return 影响行数
     */
    @Delete("DELETE FROM problem_tags WHERE problem_id = #{problemId} AND tag_id = #{tagId}")
    int delete(@Param("problemId") Long problemId, @Param("tagId") Long tagId);
    
    /**
     * 查询问题的所有标签ID
     * @param problemId 问题ID
     * @return 标签ID列表
     */
    @Select("SELECT tag_id FROM problem_tags WHERE problem_id = #{problemId}")
    List<Long> selectTagIdsByProblemId(Long problemId);
    
    /**
     * 根据标签ID查询关联的问题ID列表
     * @param tagId 标签ID
     * @return 问题ID列表
     */
    @Select("SELECT problem_id FROM problem_tags WHERE tag_id = #{tagId}")
    List<Long> selectProblemIdsByTagId(@Param("tagId") Long tagId);
    
    /**
     * 根据多个标签ID查询满足所有标签的问题ID列表
     * @param tagIds 标签ID列表
     * @return 问题ID列表
     */
    @Select("<script>" +
            "SELECT problem_id FROM problem_tags WHERE tag_id IN " +
            "<foreach collection='tagIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " GROUP BY problem_id HAVING COUNT(DISTINCT tag_id) = #{tagCount}" +
            "</script>")
    List<Long> selectProblemIdsByTagIds(@Param("tagIds") List<Long> tagIds, @Param("tagCount") int tagCount);

    /**
     * 查询多个问题的标签关系
     * @param problemIds 问题ID列表
     * @return 关系记录列表，每条记录包含problem_id和tag_id
     */
    @Select("<script>" +
            "SELECT problem_id, tag_id FROM problem_tags " +
            "WHERE problem_id IN " +
            "<foreach collection='problemIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> selectProblemTagRelations(@Param("problemIds") List<Long> problemIds);
} 
/**
 * 文件说明：标签 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.Tag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 标签Mapper接口
 * @author ls
 * @since 2024-07-01
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 查询所有标签
     * @return 标签列表
     */
    @Select("SELECT * FROM tags ORDER BY name ASC")
    List<Tag> selectAllTags();

    /**
     * 根据问题ID查询关联的标签
     * @param problemId 问题ID
     * @return 标签列表
     */
    @Select("SELECT t.* FROM tags t " +
            "JOIN problem_tags pt ON t.id = pt.tag_id " +
            "WHERE pt.problem_id = #{problemId} " +
            "ORDER BY t.name ASC")
    List<Tag> selectTagsByProblemId(Long problemId);

    /**
     * 根据名称查询标签
     * @param name 标签名称
     * @return 标签对象
     */
    @Select("SELECT * FROM tags WHERE name = #{name}")
    Tag selectTagByName(String name);

    /**
     * 创建问题和标签的关联关系
     * @param problemId 问题ID
     * @param tagId 标签ID
     * @return 影响行数
     */
    @Insert("INSERT INTO problem_tags (problem_id, tag_id) VALUES (#{problemId}, #{tagId})")
    int insertProblemTag(@Param("problemId") Long problemId, @Param("tagId") Long tagId);

    /**
     * 删除问题的所有标签关联关系
     * @param problemId 问题ID
     * @return 影响行数
     */
    @Delete("DELETE FROM problem_tags WHERE problem_id = #{problemId}")
    int deleteProblemTags(Long problemId);
} 
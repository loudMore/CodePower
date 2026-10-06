/**
 * 文件说明：题目 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.domain.Problem;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 问题Mapper接口
 * @author ls
 */
@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {

    /**
     * 查询问题详情，包括标签信息
     * @param id 问题ID
     * @return 问题详情
     */
    @Select("SELECT p.*, u.username as author_name FROM problems p LEFT JOIN users u ON p.author_id = u.id WHERE p.id = #{id} AND p.deleted = 0")
    @Results({
        @Result(id = true, property = "id", column = "id"),
        @Result(property = "authorName", column = "author_name"),
        @Result(property = "tags", column = "id", 
                many = @Many(select = "com.ls.mapper.TagMapper.selectTagsByProblemId"))
    })
    Problem selectProblemWithTags(Long id);

    @Select("SELECT p.id, p.title, p.description, p.difficulty, p.input_format, p.output_format, " +
            "p.input_example, p.output_example, p.examples, p.debug_input_example, p.debug_output_example, p.hint, " +
            "p.time_limit, p.memory_limit, p.visibility, p.author_id, p.status, " +
            "p.submit_count, p.accept_count, p.accept_rate, u.username as author_name " +
            "FROM problems p LEFT JOIN users u ON p.author_id = u.id " +
            "WHERE p.id = #{id} AND p.deleted = 0")
    @Results({
        @Result(id = true, property = "id", column = "id"),
        @Result(property = "authorName", column = "author_name"),
        @Result(property = "tags", column = "id",
                many = @Many(select = "com.ls.mapper.TagMapper.selectTagsByProblemId"))
    })
    Problem selectProblemSolvingViewWithTags(Long id);

    /**
     * 查询公开问题列表
     * @param page 分页对象
     * @param title 标题关键词
     * @param difficulty 难度
     * @return 分页问题列表
     */
    @Select({"<script>",
             "SELECT p.*, u.username as author_name FROM problems p",
             "LEFT JOIN users u ON p.author_id = u.id",
             "WHERE p.visibility = 'PUBLIC' AND p.status = 1 AND p.deleted = 0",
             "<if test='title != null and title != \"\"'>",
             "  AND p.title LIKE CONCAT('%', #{title}, '%')",
             "</if>",
             "<if test='difficulty != null and difficulty != \"\"'>",
             "  AND p.difficulty = #{difficulty}",
             "</if>",
             "ORDER BY p.id DESC",
             "</script>"})
    @Results({
        @Result(id = true, property = "id", column = "id"),
        @Result(property = "authorName", column = "author_name"),
        @Result(property = "tags", column = "id", 
                many = @Many(select = "com.ls.mapper.TagMapper.selectTagsByProblemId"))
    })
    IPage<Problem> selectPublicProblems(Page<Problem> page, @Param("title") String title, @Param("difficulty") String difficulty);

    @Select({"<script>",
             "SELECT p.id, p.title, p.difficulty, p.status, p.author_id, p.visibility,",
             "p.submit_count, p.accept_count, p.accept_rate, p.created_at, p.updated_at, p.deleted,",
             "u.username as author_name FROM problems p",
             "LEFT JOIN users u ON p.author_id = u.id",
             "WHERE p.visibility = 'PUBLIC' AND p.status = 1 AND p.deleted = 0",
             "<if test='problemId != null'>",
             "  AND p.id = #{problemId}",
             "</if>",
             "<if test='title != null and title != \"\"'>",
             "  AND p.title LIKE CONCAT('%', #{title}, '%')",
             "</if>",
             "<if test='difficulty != null and difficulty != \"\"'>",
             "  AND p.difficulty = #{difficulty}",
             "</if>",
             "ORDER BY p.id DESC",
             "</script>"})
    @Results({
        @Result(id = true, property = "id", column = "id"),
        @Result(property = "authorName", column = "author_name")
    })
    IPage<Problem> selectPublicProblemsWithoutTags(Page<Problem> page,
                                                    @Param("title") String title,
                                                    @Param("difficulty") String difficulty,
                                                    @Param("problemId") Long problemId);

    @Select({"<script>",
             "SELECT p.id, p.title, p.difficulty, p.status, p.author_id, p.visibility,",
             "p.submit_count, p.accept_count, p.accept_rate, p.created_at, p.updated_at, p.deleted,",
             "u.username as author_name FROM problems p",
             "JOIN (",
             "  SELECT pt.problem_id FROM problem_tags pt",
             "  JOIN tags t ON pt.tag_id = t.id",
             "  WHERE t.name IN",
             "  <foreach collection='tagNames' item='tag' open='(' separator=',' close=')'>",
             "    #{tag}",
             "  </foreach>",
             "  GROUP BY pt.problem_id",
             "  HAVING COUNT(DISTINCT t.name) = #{tagCount}",
             ") tagged ON tagged.problem_id = p.id",
             "LEFT JOIN users u ON p.author_id = u.id",
             "WHERE p.visibility = 'PUBLIC' AND p.status = 1 AND p.deleted = 0",
             "<if test='problemId != null'>",
             "  AND p.id = #{problemId}",
             "</if>",
             "<if test='title != null and title != \"\"'>",
             "  AND p.title LIKE CONCAT('%', #{title}, '%')",
             "</if>",
             "<if test='difficulty != null and difficulty != \"\"'>",
             "  AND p.difficulty = #{difficulty}",
             "</if>",
             "ORDER BY p.id DESC",
             "</script>"})
    @Results({
        @Result(id = true, property = "id", column = "id"),
        @Result(property = "authorName", column = "author_name")
    })
    IPage<Problem> selectPublicProblemsByTags(Page<Problem> page,
                                              @Param("tagNames") List<String> tagNames,
                                              @Param("tagCount") int tagCount,
                                              @Param("title") String title,
                                              @Param("difficulty") String difficulty,
                                              @Param("problemId") Long problemId);

    @Select({"<script>",
             "SELECT id, title, difficulty, visibility, author_id, status, submit_count, accept_count, accept_rate",
             "FROM problems",
             "WHERE deleted = 0 AND id IN",
             "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
             "#{id}",
             "</foreach>",
             "</script>"})
    @Results({
        @Result(id = true, property = "id", column = "id")
    })
    List<Problem> selectProblemSummariesByIds(@Param("ids") List<Long> ids);

    /**
     * 根据作者ID查询问题列表
     * @param authorId 作者ID
     * @return 问题列表
     */
    @Select("SELECT p.*, u.username as author_name FROM problems p LEFT JOIN users u ON p.author_id = u.id WHERE p.author_id = #{authorId} AND p.deleted = 0 ORDER BY p.id DESC")
    @Results({
        @Result(id = true, property = "id", column = "id"),
        @Result(property = "authorName", column = "author_name"),
        @Result(property = "tags", column = "id", 
                many = @Many(select = "com.ls.mapper.TagMapper.selectTagsByProblemId"))
    })
    List<Problem> selectProblemsByAuthor(Long authorId);

    /**
     * 根据标签查询问题列表
     * @param tagName 标签名
     * @return 问题列表
     */
    @Select("SELECT p.* FROM problems p " +
            "JOIN problem_tags pt ON p.id = pt.problem_id " +
            "JOIN tags t ON pt.tag_id = t.id " +
            "WHERE t.name = #{tagName} " +
            "AND p.deleted = 0 " +
            "AND p.status = 1 " +
            "AND p.visibility = 'PUBLIC' " +
            "ORDER BY p.id DESC")
    @Results({
        @Result(id = true, property = "id", column = "id"),
        @Result(property = "tags", column = "id", 
                many = @Many(select = "com.ls.mapper.TagMapper.selectTagsByProblemId"))
    })
    List<Problem> selectProblemsByTag(String tagName);

    @Select("SELECT * FROM problems WHERE id = #{id}")
    Problem findById(Long id);
    
    @Update("UPDATE problems SET visibility = #{visibility} WHERE id = #{id}")
    int updateVisibility(Long id, String visibility);
    
    @Update("UPDATE problems SET status = #{status} WHERE id = #{id}")
    int updateStatus(Long id, Integer status);

    /**
     * 查询具有指定标签的问题ID列表
     * @param tagId 标签ID
     * @return 问题ID列表
     */
    @Select("SELECT problem_id FROM problem_tags WHERE tag_id = #{tagId}")
    List<Long> selectProblemIdsByTagId(Long tagId);
    
    /**
     * 根据作者ID查询公开问题列表
     * @param authorId 作者ID
     * @return 公开问题列表
     */
    @Select("SELECT p.*, u.username as author_name FROM problems p LEFT JOIN users u ON p.author_id = u.id WHERE p.author_id = #{authorId} AND p.visibility = 'PUBLIC' AND p.deleted = 0 ORDER BY p.id DESC")
    @Results({
        @Result(id = true, property = "id", column = "id"),
        @Result(property = "authorName", column = "author_name"),
        @Result(property = "tags", column = "id", 
                many = @Many(select = "com.ls.mapper.TagMapper.selectTagsByProblemId"))
    })
    List<Problem> selectPublicProblemsByAuthor(Long authorId);
} 

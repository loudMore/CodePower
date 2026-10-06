/**
 * 文件说明：标签 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.ls.domain.Tag;
import com.ls.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 标签控制器
 * @author ls
 * @since 2024-07-01
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    /**
     * 获取所有标签
     */
    @GetMapping("")
    public ResponseEntity<?> getAllTags() {
        try {
            List<Tag> tags = tagService.getAllTags();
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取标签失败: " + e.getMessage()));
        }
    }

    /**
     * 获取标签详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTagById(@PathVariable Long id) {
        try {
            Tag tag = tagService.getTagById(id);
            if (tag == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "标签不存在"));
            }
            return ResponseEntity.ok(tag);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取标签详情失败: " + e.getMessage()));
        }
    }

    /**
     * 创建新标签
     */
    @PostMapping("")
    public ResponseEntity<?> createTag(@RequestBody Tag tag) {
        try {
            Tag createdTag = tagService.createTag(tag);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "创建标签失败: " + e.getMessage()));
        }
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTag(@PathVariable Long id, @RequestBody Tag tag) {
        try {
            tag.setId(id);
            boolean updated = tagService.updateTag(tag);
            if (updated) {
                return ResponseEntity.ok(tag);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "标签不存在或无法更新"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "更新标签失败: " + e.getMessage()));
        }
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable Long id) {
        try {
            boolean deleted = tagService.deleteTag(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "标签删除成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "标签不存在或无法删除"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "删除标签失败: " + e.getMessage()));
        }
    }

    /**
     * 获取问题的标签
     */
    @GetMapping("/problem/{problemId}")
    public ResponseEntity<?> getTagsByProblemId(@PathVariable Long problemId) {
        try {
            List<Tag> tags = tagService.getTagsByProblemId(problemId);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取问题标签失败: " + e.getMessage()));
        }
    }
} 
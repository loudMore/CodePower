/**
 * 文件说明：学习路线 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.LearningPath;
import com.ls.domain.LearningPathStage;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 学习路线控制器 — 路线列表、详情、阶段完成、进度追踪 */
@RestController
@RequestMapping("/api/learning-paths")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;
    private final UserMapper userMapper;

    /** 获取学习路线列表（支持按难度、语言筛选） */
    @GetMapping
    public Result<?> listPaths(@RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(learningPathService.listPaths(difficulty, language, page, size));
    }

    /** 获取学习路线详情（含阶段列表） */
    @GetMapping("/{id}")
    public Result<LearningPath> getPathDetail(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? getUserId(userDetails) : null;
        return Result.success(learningPathService.getPathDetail(id, userId));
    }

    /** 完成学习路线的指定阶段 */
    @PostMapping("/{id}/stages/{stageId}/complete")
    public Result<Void> completeStage(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long id,
                                       @PathVariable Long stageId) {
        Long userId = getUserId(userDetails);
        learningPathService.completeStage(userId, id, stageId);
        return Result.success();
    }

    /** 获取当前用户的所有学习路线进度 */
    @GetMapping("/my/progress")
    public Result<List<Map<String, Object>>> getMyProgress(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return Result.success(learningPathService.getMyProgress(userId));
    }

    /** 创建新的学习路线 */
    @PostMapping
    public Result<LearningPath> createPath(@RequestBody LearningPath path) {
        return Result.success(learningPathService.createPath(path));
    }

    /** 更新学习路线信息 */
    @PutMapping("/{id}")
    public Result<LearningPath> updatePath(@PathVariable Long id, @RequestBody LearningPath path) {
        return Result.success(learningPathService.updatePath(id, path));
    }

    /** 添加学习路线阶段 */
    @PostMapping("/{id}/stages")
    public Result<LearningPathStage> addStage(@PathVariable Long id, @RequestBody LearningPathStage stage) {
        return Result.success(learningPathService.addStage(id, stage));
    }

    /** 删除学习路线阶段 */
    @DeleteMapping("/{id}/stages/{stageId}")
    public Result<Void> deleteStage(@PathVariable Long id, @PathVariable Long stageId) {
        learningPathService.deleteStage(id, stageId);
        return Result.success();
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return user.getId();
    }
}

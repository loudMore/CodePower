/**
 * 文件说明：题目集 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.ProblemSet;
import com.ls.domain.ProblemSetItem;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.ProblemSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 题目集控制器 — 专题训练集的增删改查 */
@RestController
@RequestMapping("/api/problem-sets")
@RequiredArgsConstructor
public class ProblemSetController {

    private final ProblemSetService problemSetService;
    private final UserMapper userMapper;

    /** 获取题目集列表（分页、筛选） */
    @GetMapping
    public Result<IPage<ProblemSet>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) Boolean onlyPublic,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetails != null ? getUser(userDetails) : null;
        Long userId = user != null ? user.getId() : null;
        String userRole = user != null ? user.getRole() : null;
        return Result.success(problemSetService.listProblemSets(type, mine, onlyPublic, page, size, userId, userRole));
    }

    /** 获取题目集详情 */
    @GetMapping("/{id}")
    public Result<ProblemSet> detail(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return Result.success(problemSetService.getDetail(id, user.getId(), user.getRole()));
    }

    /** 获取题目集中的题目列表 */
    @GetMapping("/{id}/items")
    public Result<List<ProblemSetItem>> items(@PathVariable Long id,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return Result.success(problemSetService.getItems(id, user.getId(), user.getRole()));
    }

    /** 创建题目集 */
    @PostMapping
    public Result<ProblemSet> create(@AuthenticationPrincipal UserDetails userDetails,
                                      @RequestBody ProblemSet problemSet) {
        User user = getUser(userDetails);
        return Result.success(problemSetService.create(problemSet, user.getId(), user.getRole(), user.getLevel()));
    }

    /** 更新题目集信息 */
    @PutMapping("/{id}")
    public Result<ProblemSet> update(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      @RequestBody ProblemSet problemSet) {
        User user = getUser(userDetails);
        return Result.success(problemSetService.update(id, problemSet, user.getId(), user.getRole()));
    }

    /** 删除题目集 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        problemSetService.delete(id, user.getId(), user.getRole());
        return Result.success();
    }

    /** 向题目集中添加题目 */
    @PostMapping("/{id}/items")
    public Result<Void> addItems(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  @RequestBody List<ProblemSetItem> items) {
        User user = getUser(userDetails);
        problemSetService.addItems(id, items, user.getId(), user.getRole());
        return Result.success();
    }

    /** 从题目集中移除题目 */
    @DeleteMapping("/{id}/items")
    public Result<Void> removeItems(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     @RequestBody Map<String, List<Long>> body) {
        User user = getUser(userDetails);
        problemSetService.removeItems(id, body.get("problemIds"), user.getId(), user.getRole());
        return Result.success();
    }

    private User getUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return user;
    }
}

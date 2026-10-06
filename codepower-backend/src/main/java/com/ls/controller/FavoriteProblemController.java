/**
 * 文件说明：题目收藏 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.Problem;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.FavoriteProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 收藏控制器 — 收藏/取消收藏题目、收藏列表 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteProblemController {

    private final FavoriteProblemService favoriteProblemService;
    private final UserMapper userMapper;

    /** 收藏题目 */
    @PostMapping("/{problemId}")
    public ResponseEntity<?> addFavorite(@PathVariable Long problemId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return favoriteProblemService.addFavorite(userId, problemId);
    }

    /** 取消收藏题目 */
    @DeleteMapping("/{problemId}")
    public ResponseEntity<?> removeFavorite(@PathVariable Long problemId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return favoriteProblemService.removeFavorite(userId, problemId);
    }

    /** 检查当前用户是否已收藏指定题目 */
    @GetMapping("/check/{problemId}")
    public Result<Map<String, Boolean>> checkFavorite(@PathVariable Long problemId,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        boolean isFavorite = favoriteProblemService.isFavorite(userId, problemId);
        return Result.success(Map.of("isFavorite", isFavorite));
    }

    /** 获取当前用户的收藏题目列表 */
    @GetMapping
    public ResponseEntity<List<Problem>> getUserFavorites(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return favoriteProblemService.getUserFavoriteProblems(userId);
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

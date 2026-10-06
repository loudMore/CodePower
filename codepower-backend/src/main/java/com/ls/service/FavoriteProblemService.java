/**
 * 文件说明：题目收藏 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.ls.domain.FavoriteProblem;
import com.ls.domain.Problem;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 收藏问题服务接口
 */
public interface FavoriteProblemService {
    
    /**
     * 添加收藏
     */
    ResponseEntity<?> addFavorite(Long userId, Long problemId);
    
    /**
     * 取消收藏
     */
    ResponseEntity<?> removeFavorite(Long userId, Long problemId);
    
    /**
     * 检查是否已收藏
     */
    boolean isFavorite(Long userId, Long problemId);
    
    /**
     * 获取用户收藏的所有问题
     */
    ResponseEntity<List<Problem>> getUserFavoriteProblems(Long userId);
} 
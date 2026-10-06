/**
 * 文件说明：题目收藏 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.ls.domain.FavoriteProblem;
import com.ls.domain.Problem;
import com.ls.mapper.FavoriteProblemMapper;
import com.ls.mapper.ProblemMapper;
import com.ls.service.FavoriteProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 收藏问题服务实现类
 *
 * @author ls
 * @since 2025-07-01
 */
@Service
public class FavoriteProblemServiceImpl implements FavoriteProblemService {

    @Autowired
    private FavoriteProblemMapper favoriteProblemMapper;
    
    @Autowired
    private ProblemMapper problemMapper;

    @Override
    public ResponseEntity<?> addFavorite(Long userId, Long problemId) {
        // 检查问题是否存在
        Problem problem = problemMapper.findById(problemId);
        if (problem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "问题不存在"));
        }
        
        // 检查是否已经收藏
        if (favoriteProblemMapper.checkFavorite(userId, problemId) > 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "已经收藏过该问题"));
        }
        
        // 使用自定义SQL添加收藏
        int result = favoriteProblemMapper.addFavorite(userId, problemId);
        if (result > 0) {
            return ResponseEntity.ok(Map.of("message", "收藏成功"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "收藏失败，请稍后再试"));
        }
    }

    @Override
    public ResponseEntity<?> removeFavorite(Long userId, Long problemId) {
        // 检查是否已经收藏
        if (favoriteProblemMapper.checkFavorite(userId, problemId) == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "未收藏该问题"));
        }
        
        // 使用自定义SQL取消收藏
        int result = favoriteProblemMapper.removeFavorite(userId, problemId);
        if (result > 0) {
            return ResponseEntity.ok(Map.of("message", "取消收藏成功"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "取消收藏失败，请稍后再试"));
        }
    }

    @Override
    public boolean isFavorite(Long userId, Long problemId) {
        return favoriteProblemMapper.checkFavorite(userId, problemId) > 0;
    }

    @Override
    public ResponseEntity<List<Problem>> getUserFavoriteProblems(Long userId) {
        List<Problem> problems = favoriteProblemMapper.findProblemsByUserId(userId);
        return ResponseEntity.ok(problems);
    }
} 
/**
 * 文件说明：题解 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ls.domain.ProblemSolution;
import com.ls.domain.User;
import com.ls.domain.UserProfile;
import com.ls.mapper.ProblemSolutionMapper;
import com.ls.mapper.UserMapper;
import com.ls.mapper.UserProfileMapper;
import com.ls.service.ProblemSolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 问题题解服务实现类
 * @author ls
 * @since 2024-07-04
 */
@Service
public class ProblemSolutionServiceImpl extends ServiceImpl<ProblemSolutionMapper, ProblemSolution> implements ProblemSolutionService {

    @Autowired
    private ProblemSolutionMapper problemSolutionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;
    
    @Override
    public List<ProblemSolution> getSolutionsByProblemId(Long problemId) {
        return fillAuthorInfo(problemSolutionMapper.selectByProblemId(problemId));
    }
    
    @Override
    public List<ProblemSolution> getSolutionsByProblemIdAndLanguage(Long problemId, String language) {
        return fillAuthorInfo(problemSolutionMapper.selectByProblemIdAndLanguage(problemId, language));
    }
    
    @Override
    public List<ProblemSolution> getOfficialSolutionsByProblemId(Long problemId) {
        return fillAuthorInfo(problemSolutionMapper.selectOfficialByProblemId(problemId));
    }
    
    @Override
    public List<ProblemSolution> getSolutionsByUserId(Long userId) {
        return fillAuthorInfo(problemSolutionMapper.selectByUserId(userId));
    }
    
    @Override
    public boolean createSolution(ProblemSolution solution) {
        boolean saved = save(solution);
        if (saved) {
            fillAuthorInfo(List.of(solution));
        }
        return saved;
    }
    
    @Override
    public boolean updateSolution(ProblemSolution solution) {
        return updateById(solution);
    }
    
    @Override
    public boolean deleteSolution(Long id) {
        return removeById(id);
    }
    
    @Override
    @Transactional
    public boolean createOfficialSolutions(Long problemId, Long userId, Map<String, String> solutionsMap) {
        if (solutionsMap == null || solutionsMap.isEmpty()) {
            return false;
        }
        
        List<ProblemSolution> solutions = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : solutionsMap.entrySet()) {
            String language = entry.getKey();
            String code = entry.getValue();
            
            // 跳过空代码
            if (code == null || code.trim().isEmpty()) {
                continue;
            }
            
            ProblemSolution solution = new ProblemSolution();
            solution.setProblemId(problemId);
            solution.setUserId(userId);
            solution.setLanguage(language);
            solution.setCode(code);
            solution.setIsOfficial(1); // 设为官方题解
            solution.setTitle("官方" + language + "题解");
            solution.setStatus(1);
            
            solutions.add(solution);
        }
        
        if (solutions.isEmpty()) {
            return false;
        }
        
        return saveBatch(solutions);
    }

    @Override
    public List<ProblemSolution> getCommunitySolutionsByProblemId(Long problemId) {
        LambdaQueryWrapper<ProblemSolution> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProblemSolution::getProblemId, problemId)
                    .eq(ProblemSolution::getIsOfficial, 0) // 非官方题解
                    .eq(ProblemSolution::getStatus, 1) // 启用状态
                    .orderByDesc(ProblemSolution::getCreatedAt);
        return fillAuthorInfo(this.list(queryWrapper));
    }

    @Override
    public Map<String, String> getOfficialSolutionCodeByProblemId(Long problemId) {
        // 查询问题的所有官方题解代码
        List<ProblemSolution> solutionCodes = problemSolutionMapper.selectOfficialByProblemId(problemId);
        
        // 将结果转换为语言-代码映射
        Map<String, String> codeMap = new HashMap<>();
        for (ProblemSolution code : solutionCodes) {
            codeMap.put(code.getLanguage(), code.getCode());
        }
        
        return codeMap;
    }

    private List<ProblemSolution> fillAuthorInfo(List<ProblemSolution> solutions) {
        if (solutions == null || solutions.isEmpty()) {
            return solutions;
        }

        List<Long> userIds = solutions.stream()
                .map(ProblemSolution::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return solutions;
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
        Map<Long, UserProfile> profileMap = userProfileMapper.selectList(
                        new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity(), (left, right) -> left));

        for (ProblemSolution solution : solutions) {
            User user = userMap.get(solution.getUserId());
            if (user != null) {
                solution.setUsername(user.getUsername());
            }
            UserProfile profile = profileMap.get(solution.getUserId());
            if (profile != null && profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
                solution.setAvatarUrl(profile.getAvatarUrl());
            } else {
                solution.setAvatarUrl("/avatars/avatar-1.svg");
            }
        }

        return solutions;
    }
} 

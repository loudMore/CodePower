/**
 * 文件说明：学习路线 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.domain.LearningPath;
import com.ls.domain.LearningPathStage;
import com.ls.domain.Problem;
import com.ls.domain.Submission;
import com.ls.domain.UserLearningProgress;
import com.ls.mapper.LearningPathMapper;
import com.ls.mapper.LearningPathStageMapper;
import com.ls.mapper.ProblemMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.mapper.UserLearningProgressMapper;
import com.ls.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 学习路线服务实现 */
@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl extends ServiceImpl<LearningPathMapper, LearningPath> implements LearningPathService {

    private final LearningPathStageMapper stageMapper;
    private final UserLearningProgressMapper progressMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;

    @Override
    public IPage<LearningPath> listPaths(String difficulty, String language, int page, int size) {
        LambdaQueryWrapper<LearningPath> wrapper = new LambdaQueryWrapper<LearningPath>()
                .eq(LearningPath::getStatus, 1)
                .eq(difficulty != null, LearningPath::getDifficulty, difficulty)
                .eq(language != null, LearningPath::getLanguage, language)
                .orderByAsc(LearningPath::getSortOrder);
        IPage<LearningPath> result = this.page(new Page<>(page, size), wrapper);

        for (LearningPath path : result.getRecords()) {
            long total = stageMapper.selectCount(new LambdaQueryWrapper<LearningPathStage>()
                    .eq(LearningPathStage::getPathId, path.getId()));
            path.setTotalStages((int) total);
        }
        return result;
    }

    @Override
    public LearningPath getPathDetail(Long pathId, Long userId) {
        LearningPath path = this.getById(pathId);
        if (path == null) throw new BusinessException(ResultCode.NOT_FOUND, "学习路线不存在");

        List<LearningPathStage> stages = stageMapper.selectList(
                new LambdaQueryWrapper<LearningPathStage>()
                        .eq(LearningPathStage::getPathId, pathId)
                        .orderByAsc(LearningPathStage::getStageOrder));
        path.setStages(stages);
        path.setTotalStages(stages.size());

        // 填充关联题目信息
        List<Long> problemIds = stages.stream()
                .map(LearningPathStage::getProblemId)
                .filter(pid -> pid != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Problem> problemMap = problemIds.isEmpty() ? Collections.emptyMap() :
                problemMapper.selectBatchIds(problemIds).stream()
                        .collect(Collectors.toMap(Problem::getId, p -> p));
        stages.forEach(s -> {
            if (s.getProblemId() != null) {
                Problem p = problemMap.get(s.getProblemId());
                if (p != null) {
                    s.setProblemTitle(p.getTitle());
                    s.setProblemDifficulty(p.getDifficulty());
                }
            }
        });

        if (userId != null) {
            // 检查用户对关联题目的提交记录（ACCEPTED 即完成）
            Set<Long> acceptedProblemIds = Collections.emptySet();
            if (!problemIds.isEmpty()) {
                List<Submission> subs = submissionMapper.selectList(
                        new LambdaQueryWrapper<Submission>()
                                .eq(Submission::getUserId, userId)
                                .in(Submission::getProblemId, problemIds)
                                .eq(Submission::getStatus, "ACCEPTED")
                                .isNull(Submission::getContestId));
                acceptedProblemIds = subs.stream()
                        .map(Submission::getProblemId)
                        .collect(Collectors.toSet());
            }

            // 同时读取手动标记的完成记录（无题目的阶段仍用手动完成）
            List<UserLearningProgress> progressList = progressMapper.selectList(
                    new LambdaQueryWrapper<UserLearningProgress>()
                            .eq(UserLearningProgress::getUserId, userId)
                            .eq(UserLearningProgress::getPathId, pathId));
            Set<Long> manuallyCompleted = progressList.stream()
                    .filter(p -> "COMPLETED".equals(p.getStatus()))
                    .map(UserLearningProgress::getStageId)
                    .collect(Collectors.toSet());

            int completedCount = 0;
            for (LearningPathStage s : stages) {
                boolean completed;
                if (s.getProblemId() != null) {
                    // 有关联题目：通过提交判断
                    completed = acceptedProblemIds.contains(s.getProblemId());
                    // 同步写入进度表
                    if (completed && !manuallyCompleted.contains(s.getId())) {
                        syncProgressRecord(userId, pathId, s.getId());
                    }
                } else {
                    // 无关联题目：手动标记
                    completed = manuallyCompleted.contains(s.getId());
                }
                s.setProgressStatus(completed ? "COMPLETED" : "PENDING");
                if (completed) completedCount++;
            }
            path.setCompletedStages(completedCount);
        }
        return path;
    }

    // 同步进度记录到数据库（幂等）
    private void syncProgressRecord(Long userId, Long pathId, Long stageId) {
        UserLearningProgress existing = progressMapper.selectOne(
                new LambdaQueryWrapper<UserLearningProgress>()
                        .eq(UserLearningProgress::getUserId, userId)
                        .eq(UserLearningProgress::getStageId, stageId));
        if (existing == null) {
            UserLearningProgress p = new UserLearningProgress();
            p.setUserId(userId);
            p.setPathId(pathId);
            p.setStageId(stageId);
            p.setStatus("COMPLETED");
            p.setCompletedAt(LocalDateTime.now());
            p.setCreatedAt(LocalDateTime.now());
            progressMapper.insert(p);
        } else if (!"COMPLETED".equals(existing.getStatus())) {
            existing.setStatus("COMPLETED");
            existing.setCompletedAt(LocalDateTime.now());
            progressMapper.updateById(existing);
        }
    }

    @Override
    @Transactional
    public void completeStage(Long userId, Long pathId, Long stageId) {
        LearningPathStage stage = stageMapper.selectById(stageId);
        if (stage == null || !stage.getPathId().equals(pathId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阶段不存在");
        }
        UserLearningProgress existing = progressMapper.selectOne(
                new LambdaQueryWrapper<UserLearningProgress>()
                        .eq(UserLearningProgress::getUserId, userId)
                        .eq(UserLearningProgress::getStageId, stageId));
        if (existing != null) {
            existing.setStatus("COMPLETED");
            existing.setCompletedAt(LocalDateTime.now());
            progressMapper.updateById(existing);
        } else {
            UserLearningProgress progress = new UserLearningProgress();
            progress.setUserId(userId);
            progress.setPathId(pathId);
            progress.setStageId(stageId);
            progress.setStatus("COMPLETED");
            progress.setCompletedAt(LocalDateTime.now());
            progress.setCreatedAt(LocalDateTime.now());
            progressMapper.insert(progress);
        }
    }

    @Override
    public List<Map<String, Object>> getMyProgress(Long userId) {
        List<UserLearningProgress> allProgress = progressMapper.selectList(
                new LambdaQueryWrapper<UserLearningProgress>()
                        .eq(UserLearningProgress::getUserId, userId));
        Map<Long, List<UserLearningProgress>> byPath = allProgress.stream()
                .collect(Collectors.groupingBy(UserLearningProgress::getPathId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<UserLearningProgress>> entry : byPath.entrySet()) {
            LearningPath path = this.getById(entry.getKey());
            if (path == null) continue;
            long totalStages = stageMapper.selectCount(
                    new LambdaQueryWrapper<LearningPathStage>().eq(LearningPathStage::getPathId, path.getId()));
            long completed = entry.getValue().stream().filter(p -> "COMPLETED".equals(p.getStatus())).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("pathId", path.getId());
            item.put("title", path.getTitle());
            item.put("difficulty", path.getDifficulty());
            item.put("totalStages", totalStages);
            item.put("completedStages", completed);
            item.put("progress", totalStages > 0 ? Math.round(completed * 100.0 / totalStages) : 0);
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public LearningPath createPath(LearningPath path) {
        path.setStatus(1);
        path.setCreatedAt(LocalDateTime.now());
        path.setUpdatedAt(LocalDateTime.now());
        this.save(path);
        return path;
    }

    @Override
    @Transactional
    public LearningPath updatePath(Long id, LearningPath updated) {
        LearningPath path = this.getById(id);
        if (path == null) throw new BusinessException(ResultCode.NOT_FOUND, "学习路线不存在");
        if (updated.getTitle() != null) path.setTitle(updated.getTitle());
        if (updated.getDescription() != null) path.setDescription(updated.getDescription());
        if (updated.getDifficulty() != null) path.setDifficulty(updated.getDifficulty());
        if (updated.getLanguage() != null) path.setLanguage(updated.getLanguage());
        if (updated.getCoverUrl() != null) path.setCoverUrl(updated.getCoverUrl());
        if (updated.getEstimatedHours() != null) path.setEstimatedHours(updated.getEstimatedHours());
        if (updated.getSortOrder() != null) path.setSortOrder(updated.getSortOrder());
        path.setUpdatedAt(LocalDateTime.now());
        this.updateById(path);
        return path;
    }

    @Override
    @Transactional
    public LearningPathStage addStage(Long pathId, LearningPathStage stage) {
        LearningPath path = this.getById(pathId);
        if (path == null) throw new BusinessException(ResultCode.NOT_FOUND, "学习路线不存在");
        stage.setPathId(pathId);
        if (stage.getStageOrder() == null) {
            long count = stageMapper.selectCount(
                    new LambdaQueryWrapper<LearningPathStage>().eq(LearningPathStage::getPathId, pathId));
            stage.setStageOrder((int) count + 1);
        }
        stageMapper.insert(stage);
        return stage;
    }

    @Override
    @Transactional
    public void deleteStage(Long pathId, Long stageId) {
        LearningPathStage stage = stageMapper.selectById(stageId);
        if (stage == null || !stage.getPathId().equals(pathId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阶段不存在");
        }
        stageMapper.deleteById(stageId);
    }
}

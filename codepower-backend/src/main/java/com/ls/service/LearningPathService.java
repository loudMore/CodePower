/**
 * 文件说明：学习路线 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.domain.LearningPath;
import com.ls.domain.LearningPathStage;

import java.util.List;
import java.util.Map;

/** 学习路线服务 */
public interface LearningPathService {
    /** 分页查询学习路线列表 */
    IPage<LearningPath> listPaths(String difficulty, String language, int page, int size);
    /** 获取学习路线详情（含用户进度） */
    LearningPath getPathDetail(Long pathId, Long userId);
    /** 标记用户完成某阶段 */
    void completeStage(Long userId, Long pathId, Long stageId);
    /** 获取当前用户的所有学习路线进度 */
    List<Map<String, Object>> getMyProgress(Long userId);
    /** 创建学习路线 */
    LearningPath createPath(LearningPath path);
    /** 更新学习路线 */
    LearningPath updatePath(Long id, LearningPath path);
    /** 向学习路线中添加阶段 */
    LearningPathStage addStage(Long pathId, LearningPathStage stage);
    /** 删除学习路线中的阶段 */
    void deleteStage(Long pathId, Long stageId);
}

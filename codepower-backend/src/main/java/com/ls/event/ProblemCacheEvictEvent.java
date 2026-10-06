/**
 * 文件说明：题目缓存清理事件，用于在业务变更后通知缓存刷新。
 */
package com.ls.event;

/** 题目缓存清理事件：标签、题目内容变更后通知列表缓存和详情缓存失效。 */
public record ProblemCacheEvictEvent(boolean includeProblemDetailCaches) {
    public static ProblemCacheEvictEvent all() {
        return new ProblemCacheEvictEvent(true);
    }

    public static ProblemCacheEvictEvent listsOnly() {
        return new ProblemCacheEvictEvent(false);
    }
}

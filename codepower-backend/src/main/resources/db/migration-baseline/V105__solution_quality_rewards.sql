ALTER TABLE problem_solutions
    ADD COLUMN source_submission_id BIGINT NULL COMMENT '题解来源 AC 提交',
    ADD COLUMN runtime_ms INT NULL COMMENT '来源提交运行时间 ms',
    ADD COLUMN memory_kb INT NULL COMMENT '来源提交内存 KB',
    ADD COLUMN reward_granted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已发放题解质量奖励',
    ADD COLUMN reward_reason VARCHAR(32) NULL COMMENT '题解奖励原因';

CREATE INDEX idx_problem_solutions_quality
    ON problem_solutions(problem_id, is_official, deleted, reward_granted, runtime_ms, memory_kb);

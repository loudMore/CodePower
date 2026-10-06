-- V2 merge official solution data from solution_codes to problem_solutions

INSERT INTO problem_solutions (
  problem_id,
  user_id,
  language,
  title,
  description,
  code,
  is_official,
  likes,
  status,
  created_at,
  updated_at,
  deleted
)
SELECT
  sc.problem_id,
  COALESCE(p.author_id, 1) AS user_id,
  sc.language,
  CONCAT('官方', sc.language, '题解') AS title,
  NULL AS description,
  sc.code,
  1 AS is_official,
  0 AS likes,
  1 AS status,
  COALESCE(sc.created_at, CURRENT_TIMESTAMP) AS created_at,
  CURRENT_TIMESTAMP AS updated_at,
  0 AS deleted
FROM solution_codes sc
LEFT JOIN problems p ON p.id = sc.problem_id
WHERE sc.is_official = 1
  AND NOT EXISTS (
    SELECT 1
    FROM problem_solutions ps
    WHERE ps.problem_id = sc.problem_id
      AND ps.language = sc.language
      AND ps.is_official = 1
      AND ps.deleted = 0
  );

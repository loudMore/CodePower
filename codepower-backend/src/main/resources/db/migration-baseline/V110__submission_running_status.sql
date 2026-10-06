-- Ensure async judge intermediate state is accepted by the submission status dictionary.
INSERT INTO dict_submission_status (code, name)
VALUES ('RUNNING', '评测中')
ON DUPLICATE KEY UPDATE name = VALUES(name);

ALTER TABLE problems
    ADD COLUMN examples LONGTEXT NULL AFTER output_example;

UPDATE problems
SET examples = JSON_ARRAY(JSON_OBJECT(
        'input', COALESCE(input_example, ''),
        'output', COALESCE(output_example, ''),
        'explanation', ''
    ))
WHERE examples IS NULL
  AND (COALESCE(input_example, '') <> '' OR COALESCE(output_example, '') <> '');


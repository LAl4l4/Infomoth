-- Repair duplicate sentiment samples written by the old 15-minute scheduler.
-- Run this against MySQL 8 after the Backend schema migration has run.
-- The script keeps the first row in each consecutive run with the same raw
-- sentiment score, then rebuilds cumulative sample statistics from the rows
-- that remain. It is safe to run more than once.

START TRANSACTION;

CREATE TEMPORARY TABLE sentiment_duplicate_ids (
    table_name VARCHAR(32) NOT NULL,
    ID INT NOT NULL,
    PRIMARY KEY (table_name, ID)
);

INSERT INTO sentiment_duplicate_ids (table_name, ID)
SELECT 'politics_average', ID
FROM (
    SELECT ID,
           sentimentScore,
           LAG(sentimentScore) OVER (ORDER BY ID) AS previousScore
    FROM politics_average
) AS ordered_rows
WHERE previousScore IS NOT NULL
  AND sentimentScore = previousScore;

INSERT INTO sentiment_duplicate_ids (table_name, ID)
SELECT 'tech_average', ID
FROM (
    SELECT ID,
           sentimentScore,
           LAG(sentimentScore) OVER (ORDER BY ID) AS previousScore
    FROM tech_average
) AS ordered_rows
WHERE previousScore IS NOT NULL
  AND sentimentScore = previousScore;

CREATE TEMPORARY TABLE sentiment_recomputed (
    table_name VARCHAR(32) NOT NULL,
    ID INT NOT NULL,
    rollingAverage DOUBLE NOT NULL,
    rollingStandardDeviation DOUBLE NOT NULL,
    sampleCount INT NOT NULL,
    PRIMARY KEY (table_name, ID)
);

INSERT INTO sentiment_recomputed (
    table_name, ID, rollingAverage, rollingStandardDeviation, sampleCount
)
SELECT 'politics_average', ID,
       runningSum / sampleCount,
       CASE
           WHEN sampleCount <= 1 THEN 0.0
           ELSE SQRT(GREATEST(
               0.0,
               (runningSumSquares - (runningSum * runningSum / sampleCount))
                   / (sampleCount - 1)
           ))
       END,
       sampleCount
FROM (
    SELECT p.ID,
           COUNT(*) OVER (
               ORDER BY p.ID ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
           ) AS sampleCount,
           SUM(p.sentimentScore) OVER (
               ORDER BY p.ID ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
           ) AS runningSum,
           SUM(p.sentimentScore * p.sentimentScore) OVER (
               ORDER BY p.ID ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
           ) AS runningSumSquares
    FROM politics_average p
    LEFT JOIN sentiment_duplicate_ids d
      ON d.table_name = 'politics_average' AND d.ID = p.ID
    WHERE d.ID IS NULL
) AS cumulative;

INSERT INTO sentiment_recomputed (
    table_name, ID, rollingAverage, rollingStandardDeviation, sampleCount
)
SELECT 'tech_average', ID,
       runningSum / sampleCount,
       CASE
           WHEN sampleCount <= 1 THEN 0.0
           ELSE SQRT(GREATEST(
               0.0,
               (runningSumSquares - (runningSum * runningSum / sampleCount))
                   / (sampleCount - 1)
           ))
       END,
       sampleCount
FROM (
    SELECT t.ID,
           COUNT(*) OVER (
               ORDER BY t.ID ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
           ) AS sampleCount,
           SUM(t.sentimentScore) OVER (
               ORDER BY t.ID ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
           ) AS runningSum,
           SUM(t.sentimentScore * t.sentimentScore) OVER (
               ORDER BY t.ID ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
           ) AS runningSumSquares
    FROM tech_average t
    LEFT JOIN sentiment_duplicate_ids d
      ON d.table_name = 'tech_average' AND d.ID = t.ID
    WHERE d.ID IS NULL
) AS cumulative;

UPDATE politics_average p
JOIN sentiment_recomputed r
  ON r.table_name = 'politics_average' AND r.ID = p.ID
SET p.rollingAverage = r.rollingAverage,
    p.rollingStandardDeviation = r.rollingStandardDeviation,
    p.sampleCount = r.sampleCount,
    p.sampleVarianceCorrected = TRUE;

UPDATE tech_average t
JOIN sentiment_recomputed r
  ON r.table_name = 'tech_average' AND r.ID = t.ID
SET t.rollingAverage = r.rollingAverage,
    t.rollingStandardDeviation = r.rollingStandardDeviation,
    t.sampleCount = r.sampleCount,
    t.sampleVarianceCorrected = TRUE;

DELETE p
FROM politics_average p
JOIN sentiment_duplicate_ids d
  ON d.table_name = 'politics_average' AND d.ID = p.ID;

DELETE t
FROM tech_average t
JOIN sentiment_duplicate_ids d
  ON d.table_name = 'tech_average' AND d.ID = t.ID;

COMMIT;

SELECT table_name, COUNT(*) AS removed_rows
FROM sentiment_duplicate_ids
GROUP BY table_name;

SELECT 'politics_average' AS table_name, COUNT(*) AS rows_after
FROM politics_average
UNION ALL
SELECT 'tech_average', COUNT(*)
FROM tech_average;

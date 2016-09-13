
DROP VIEW IF EXISTS snapshot_totals_daily;

-- filter out Sat and Sun
CREATE VIEW IF NOT EXISTS snapshot_totals_daily
AS
SELECT n.*
FROM snapshot_totals n
INNER JOIN (
  SELECT _id, MAX(snapshot_time) AS snapshot_time
  FROM snapshot_totals
  WHERE strftime("%w",snapshot_time/1000, 'unixepoch') NOT IN ('0', '6')
  GROUP BY date(snapshot_time/1000, 'unixepoch'), account_id
) AS max USING (_id, snapshot_time);

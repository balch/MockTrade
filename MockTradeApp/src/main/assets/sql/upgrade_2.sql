
DROP INDEX IF EXISTS snapshot_totals_account_idx;

CREATE INDEX IF NOT EXISTS snapshot_totals_query_idx ON [snapshot_totals](account_id, snapshot_time);

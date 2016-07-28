
DROP INDEX IF EXISTS snapshot_totals_account_idx;

CREATE INDEX IF NOT EXISTS snapshot_totals_query_idx ON [snapshot_totals](account_id, snapshot_time);

CREATE INDEX IF NOT EXISTS investment_last_trade_time_idx ON investment(last_trade_time DESC);
CREATE INDEX IF NOT EXISTS investment_query_idx ON investment(account_id, symbol);

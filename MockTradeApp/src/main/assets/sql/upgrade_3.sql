
CREATE INDEX IF NOT EXISTS investment_last_trade_time_idx ON investment(last_trade_time DESC);
CREATE INDEX IF NOT EXISTS investment_query_idx ON investment(account_id, symbol);

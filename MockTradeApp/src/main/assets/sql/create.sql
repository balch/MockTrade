CREATE TABLE account (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    initial_balance INTEGER NOT NULL,
    available_funds INTEGER NOT NULL,
    strategy TEXT NOT NULL,
    exclude_from_totals INTEGER NOT NULL DEFAULT 0,
    create_time INTEGER NOT NULL,
    update_time INTEGER NOT NULL
);

CREATE TABLE investment (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER  NOT NULL REFERENCES account(_id) ON DELETE CASCADE,
    symbol TEXT NOT NULL,
    status TEXT NOT NULL,
    description TEXT NOT NULL,
    exchange TEXT NOT NULL,
    cost_basis INTEGER NOT NULL,
    price INTEGER NOT NULL,
    last_trade_time INTEGER NOT NULL,
    prev_day_close INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    create_time INTEGER NOT NULL,
    update_time INTEGER NOT NULL
);

CREATE INDEX investment_account_idx ON investment(account_id);
CREATE INDEX investment_last_trade_time_idx ON investment(last_trade_time DESC);
CREATE INDEX investment_query_idx ON investment(account_id, symbol);


CREATE TABLE [order] (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER  NOT NULL REFERENCES account(_id) ON DELETE CASCADE,
    symbol TEXT NOT NULL,
    action TEXT NOT NULL,
    status TEXT NOT NULL,
    strategy TEXT NOT NULL,
    duration TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    limit_price INTEGER NOT NULL,
    stop_price INTEGER NOT NULL,
    stop_percent REAL NOT NULL,
    highest_price INTEGER NOT NULL,
    create_time INTEGER NOT NULL,
    update_time INTEGER NOT NULL
);

CREATE INDEX order_account_idx ON [order](account_id);

CREATE TABLE [transaction] (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER  NOT NULL REFERENCES account(_id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    type TEXT NOT NULL,
    notes TEXT NOT NULL,
    create_time INTEGER NOT NULL,
    update_time INTEGER NOT NULL
);

CREATE INDEX transaction_account_idx ON [transaction](account_id);

CREATE TABLE snapshot_totals (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER  NOT NULL REFERENCES account(_id) ON DELETE CASCADE,
    snapshot_time INTEGER NOT NULL,
    cost_basis INTEGER NOT NULL,
    total_value INTEGER NOT NULL,
    today_change INTEGER NOT NULL,
    create_time INTEGER NOT NULL,
    update_time INTEGER NOT NULL
);


CREATE INDEX snapshot_totals_account_idx ON [snapshot_totals](account_id);
CREATE INDEX snapshot_totals_query_idx ON [snapshot_totals](account_id, snapshot_time);

CREATE VIEW IF NOT EXISTS snapshot_totals_daily
AS
SELECT n.*
FROM snapshot_totals n
INNER JOIN (
  SELECT _id, MAX(snapshot_time) AS snapshot_time
  FROM snapshot_totals GROUP BY date(snapshot_time/1000, 'unixepoch'), account_id
) AS max USING (_id, snapshot_time);


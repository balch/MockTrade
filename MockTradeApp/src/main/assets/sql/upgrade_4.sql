CREATE TABLE summary_current (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER  NOT NULL REFERENCES account(_id) ON DELETE CASCADE,
    symbol TEXT NOT NULL,
    trade_time TEXT NOT NULL,
    price INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    cost_basis INTEGER NOT NULL,
    create_time TEXT NOT NULL,
    update_time TEXT NOT NULL
);

CREATE INDEX summary_current_trade_time_idx ON [summary_current](trade_time);
CREATE INDEX summary_current_daily_delete_idx ON [summary_current](account_id, symbol, trade_time);
CREATE INDEX summary_current_account_idx ON [summary_current](account_id);


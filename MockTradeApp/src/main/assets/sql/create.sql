CREATE TABLE account (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    initial_balance INTEGER NOT NULL,
    available_funds INTEGER NOT NULL,
    strategy TEXT NOT NULL,
    exclude_from_totals INTEGER NOT NULL DEFAULT 0,
    create_time TEXT NOT NULL,
    update_time TEXT NOT NULL
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
    last_trade_time TEXT NOT NULL,
    prev_day_close INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    create_time TEXT NOT NULL,
    update_time TEXT NOT NULL
);

CREATE INDEX investment_account_idx ON investment(account_id);

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
    create_time TEXT NOT NULL,
    update_time TEXT NOT NULL
);

CREATE INDEX order_account_idx ON [order](account_id);

CREATE TABLE [transaction] (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER  NOT NULL REFERENCES account(_id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    type TEXT NOT NULL,
    notes TEXT NOT NULL,
    create_time TEXT NOT NULL,
    update_time TEXT NOT NULL
);

CREATE INDEX transaction_account_idx ON [transaction](account_id);

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

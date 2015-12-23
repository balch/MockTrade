BEGIN TRANSACTION;

-- add last_trade_time with NON NULL qualifier
ALTER TABLE investment RENAME TO tmp_investment;

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

INSERT INTO investment( _id, account_id, symbol, status,
                description, exchange, cost_basis, price,
                prev_day_close, quantity, create_time,
                update_time, last_trade_time)
    SELECT _id, account_id, symbol, status,
           description, exchange, cost_basis, price,
           prev_day_close, quantity, create_time,
           update_time,  strftime("%Y-%m-%dT%H:%M:%SZ", date('now','-1 day'))
    FROM tmp_investment;

DROP TABLE tmp_investment;

COMMIT;

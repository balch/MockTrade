ALTER TABLE summary_current ADD COLUMN prev_day_close INTEGER NOT NULL DEFAULT 0;

UPDATE summary_current SET
   prev_day_close = (SELECT prev_day_close from investment where symbol = summary_current.symbol);

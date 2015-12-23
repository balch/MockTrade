/*
 * Author: Balch
 * Created: 9/4/14 12:26 AM
 *
 * This file is part of MockTrade.
 *
 * MockTrade is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MockTrade is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MockTrade.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2014
 */

package com.balch.mocktrade.investment;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.balch.android.app.framework.sql.SqlMapper;

import java.io.Serializable;
import java.util.Map;

public class SummaryItemMapper implements SqlMapper<Investment>,  Serializable {
    public static final String TAG = SummaryItemMapper.class.getSimpleName();

    public static final String TABLE_NAME = "summary_current";

    public static final String COLUMN_ACCOUNT_ID = "account_id";
    public static final String COLUMN_SYMBOL = "symbol";
    public static final String COLUMN_TRADE_TIME = "trade_time";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_QUANTITY = "quantity";
    public static final String COLUMN_COST_BASIS = "cost_basis";

    @Override
    public String getTableName() {
        return SummaryItemMapper.TABLE_NAME;
    }

    @Override
    public ContentValues getContentValues(Investment investment) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_ACCOUNT_ID, investment.getAccount().getId());
        values.put(COLUMN_SYMBOL, investment.getSymbol());
        values.put(COLUMN_COST_BASIS, investment.getCostBasis().getMicroCents());
        values.put(COLUMN_PRICE, investment.getPrice().getMicroCents());
        values.put(COLUMN_TRADE_TIME, investment.getLastTradeTime().toString());
        values.put(COLUMN_QUANTITY, investment.getQuantity());

        return values;
    }

    @Override
    public void populate(Investment investment, Cursor cursor, Map<String, Integer> columnMap) {
        throw new UnsupportedOperationException();
    }

    public void deleteSummaryItemByTradeTime(Investment investment, SQLiteDatabase db) {
        db.delete(TABLE_NAME,
                COLUMN_ACCOUNT_ID+"=? AND "+ COLUMN_SYMBOL+"=? AND "+COLUMN_TRADE_TIME+"=?",
                new String[]{investment.getAccount().getId().toString(),
                        investment.getSymbol(), investment.getLastTradeTime().toString()});
    }
}

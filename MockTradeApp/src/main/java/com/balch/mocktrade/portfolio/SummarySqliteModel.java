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

package com.balch.mocktrade.portfolio;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.balch.android.app.framework.sql.SqlMapper;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.SqliteModel;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class SummarySqliteModel extends SqliteModel
        implements SqlMapper<SummaryItem>, Serializable {
    public static final String TAG = SummarySqliteModel.class.getSimpleName();

    public static final String TABLE_NAME = "summary_current";

    public static final String COLUMN_ACCOUNT_ID = "account_id";
    public static final String COLUMN_SYMBOL = "symbol";
    public static final String COLUMN_TRADE_TIME = "trade_time";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_QUANTITY = "quantity";
    public static final String COLUMN_COST_BASIS = "cost_basis";
    public static final String COLUMN_PREV_DAY_CLOSE = "prev_day_close";

    public SummarySqliteModel(ModelProvider modelProvider) {
        super(modelProvider);
    }

    @Override
    public String getTableName() {
        return SummarySqliteModel.TABLE_NAME;
    }

    @Override
    public ContentValues getContentValues(SummaryItem summaryItem) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_ACCOUNT_ID, summaryItem.getAccount().getId());
        values.put(COLUMN_SYMBOL, summaryItem.getSymbol());
        values.put(COLUMN_COST_BASIS, summaryItem.getCostBasis().getMicroCents());
        values.put(COLUMN_PRICE, summaryItem.getPrice().getMicroCents());
        values.put(COLUMN_TRADE_TIME, summaryItem.getTradeTime().getTime());
        values.put(COLUMN_QUANTITY, summaryItem.getQuantity());
        values.put(COLUMN_PREV_DAY_CLOSE, summaryItem.getPrevDayClose().getMicroCents());

        return values;
    }

    @Override
    public void populate(SummaryItem summaryItem, Cursor cursor, Map<String, Integer> columnMap) {
        summaryItem.setId(cursor.getLong(columnMap.get(COLUMN_ID)));
        summaryItem.account = new Account();
        summaryItem.account.setId(cursor.getLong(columnMap.get(COLUMN_ACCOUNT_ID)));
        summaryItem.symbol = cursor.getString(columnMap.get(COLUMN_SYMBOL));
        summaryItem.costBasis = new Money(cursor.getLong(columnMap.get(COLUMN_COST_BASIS)));
        summaryItem.price = new Money(cursor.getLong(columnMap.get(COLUMN_PRICE)));
        summaryItem.tradeTime = new Date(cursor.getLong(columnMap.get(COLUMN_TRADE_TIME)));
        summaryItem.quantity = cursor.getLong(columnMap.get(COLUMN_QUANTITY));
        summaryItem.prevDayClose = new Money(cursor.getLong(columnMap.get(COLUMN_PREV_DAY_CLOSE)));
    }

    public void deleteSummaryItemByTradeTime(Investment investment, SQLiteDatabase db) {
        db.delete(TABLE_NAME,
                COLUMN_ACCOUNT_ID+"=? AND "+ COLUMN_SYMBOL+"=? AND "+COLUMN_TRADE_TIME+"=?",
                new String[]{investment.getAccount().getId().toString(),
                        investment.getSymbol(),
                        String.valueOf(investment.getLastTradeTime().getTime())});
    }

    public Date getLastSyncTime() {
        Date date = null;
        Cursor cursor = null;
        try {
            cursor = getSqlConnection().getReadableDatabase().
                    rawQuery("select max("+COLUMN_CREATE_TIME+") from "+TABLE_NAME, new String[0]);
            if (cursor.moveToNext()) {
                date = new Date(cursor.getLong(0));
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return date;
    }

    public Date getLastTradeTime() {
        Date date = null;
        Cursor cursor = null;
        try {
            cursor = getSqlConnection().getReadableDatabase().
                    rawQuery("select max("+COLUMN_TRADE_TIME+") from "+TABLE_NAME, new String[0]);
            if (cursor.moveToNext()) {
                date = new Date(cursor.getLong(0));
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return date;
    }

}

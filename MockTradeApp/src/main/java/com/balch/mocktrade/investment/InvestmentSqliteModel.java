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

import com.balch.android.app.framework.sql.SqlConnection;
import com.balch.android.app.framework.sql.SqlMapper;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.account.Account;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class InvestmentSqliteModel implements SqlMapper<Investment> {

    private static final String TAG = Investment.class.getSimpleName();

    private static final String TABLE_NAME = "investment";

    private static final String COLUMN_ACCOUNT_ID = "account_id";
    private static final String COLUMN_SYMBOL = "symbol";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_EXCHANGE = "exchange";
    private static final String COLUMN_COST_BASIS = "cost_basis";
    private static final String COLUMN_PRICE = "price";
    private static final String COLUMN_LAST_TRADE_TIME = "last_trade_time";
    private static final String COLUMN_PREV_DAY_CLOSE = "prev_day_close";
    private static final String COLUMN_QUANTITY = "quantity";

    private static final String SQL_LAST_TRADE_TIME =
            "SELECT MAX(" + COLUMN_LAST_TRADE_TIME + ") FROM "+TABLE_NAME;

    private static final String SQL_WHERE_BY_ACCOUNT_AND_SYMBOL =
             COLUMN_SYMBOL + " = ? AND " + COLUMN_ACCOUNT_ID + " = ?";

    private final SqlConnection sqlConnection;

    public InvestmentSqliteModel(SqlConnection sqlConnection) {
        this.sqlConnection = sqlConnection;
    }

    public List<Investment> getInvestments(Long accountId) {
        try {
            String where = null;
            String [] whereArgs = null;
            if (accountId != null) {
                where = COLUMN_ACCOUNT_ID + " = ?";
                whereArgs = new String[]{accountId.toString()};
            }
            return sqlConnection.query(this, Investment.class, where, whereArgs, COLUMN_SYMBOL + " COLLATE NOCASE");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Investment> getAllInvestments() {
        return this.getInvestments(null);
    }

    public Investment getInvestmentBySymbol(String symbol, Long accountId) {
        try {
            String [] whereArgs = new String[]{symbol, accountId.toString()};
            List<Investment> investments = sqlConnection.query(this, Investment.class,
                    SQL_WHERE_BY_ACCOUNT_AND_SYMBOL, whereArgs, null);

            return (investments.size() == 1) ? investments.get(0) : null;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean updateInvestment(Investment investment) {
        try {
            return sqlConnection.update(this, investment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public ContentValues getContentValues(Investment investment) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_ACCOUNT_ID, investment.getAccount().getId());
        values.put(COLUMN_SYMBOL, investment.getSymbol());
        values.put(COLUMN_STATUS, investment.getStatus().name());
        values.put(COLUMN_DESCRIPTION, investment.getDescription());
        values.put(COLUMN_EXCHANGE, investment.getExchange());
        values.put(COLUMN_COST_BASIS, investment.getCostBasis().getMicroCents());
        values.put(COLUMN_PRICE, investment.getPrice().getMicroCents());
        values.put(COLUMN_LAST_TRADE_TIME, investment.getLastTradeTime().getTime());
        values.put(COLUMN_PREV_DAY_CLOSE, investment.getPrevDayClose().getMicroCents());
        values.put(COLUMN_QUANTITY, investment.getQuantity());

        return values;
    }

    @Override
    public void populate(Investment investment, Cursor cursor, Map<String, Integer> columnMap) {
        investment.setId(cursor.getLong(columnMap.get(COLUMN_ID)));

        Account account = new Account();
        account.setId(cursor.getLong(columnMap.get(COLUMN_ACCOUNT_ID)));
        investment.setAccount(account);
        investment.setSymbol(cursor.getString(columnMap.get(COLUMN_SYMBOL)));
        investment.setStatus(Investment.InvestmentStatus.valueOf(cursor.getString(columnMap.get(COLUMN_STATUS))));
        investment.setDescription(cursor.getString(columnMap.get(COLUMN_DESCRIPTION)));
        investment.setExchange(cursor.getString(columnMap.get(COLUMN_EXCHANGE)));
        investment.setCostBasis(new Money(cursor.getLong(columnMap.get(COLUMN_COST_BASIS))));
        investment.setPrice(new Money(cursor.getLong(columnMap.get(COLUMN_PRICE))), new Date(cursor.getLong(columnMap.get(COLUMN_LAST_TRADE_TIME))));
        investment.setPrevDayClose(new Money(cursor.getLong(columnMap.get(COLUMN_PREV_DAY_CLOSE))));
        investment.setQuantity(cursor.getLong(columnMap.get(COLUMN_QUANTITY)));
    }

    public Date getLastTradeTime() {
        Date date = null;
        Cursor cursor = null;
        try {
            cursor = sqlConnection.rawQuery(SQL_LAST_TRADE_TIME, null);
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

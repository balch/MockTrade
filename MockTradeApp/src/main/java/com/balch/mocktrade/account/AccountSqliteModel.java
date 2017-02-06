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

package com.balch.mocktrade.account;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.balch.android.app.framework.sql.SqlConnection;
import com.balch.android.app.framework.sql.SqlMapper;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.NetworkRequestProvider;
import com.balch.mocktrade.account.strategies.BaseStrategy;
import com.balch.mocktrade.settings.Settings;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AccountSqliteModel implements SqlMapper<Account> {
    private static final String TAG = AccountSqliteModel.class.getSimpleName();

    public static final String TABLE_NAME = "account";

    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_INITIAL_BALANCE = "initial_balance";
    public static final String COLUMN_STRATEGY = "strategy";
    public static final String COLUMN_AVAILABLE_FUNDS = "available_funds";
    public static final String COLUMN_EXCLUDE_FROM_TOTALS = "exclude_from_totals";

    private final SqlConnection sqlConnection;
    private final Context context;
    private final NetworkRequestProvider networkRequestProvider;
    private final Settings settings;

    public AccountSqliteModel(Context context, NetworkRequestProvider networkRequestProvider,
                              SqlConnection sqlConnection, Settings settings) {
        this.context = context.getApplicationContext();
        this.sqlConnection = sqlConnection;
        this.networkRequestProvider = networkRequestProvider;
        this.settings = settings;
    }

    public List<Account> getAccounts(boolean allAccounts) {
        try {
            String where = null;
            String [] args = null;
            if (!allAccounts) {
                where = COLUMN_EXCLUDE_FROM_TOTALS + "=?";
                args = new String[]{"0"};
            }
            return sqlConnection.query(this, Account.class, where, args, COLUMN_NAME + " COLLATE NOCASE");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Account getAccount(long accountId) {
        try {
            List<Account> accounts = sqlConnection.query(this, Account.class, SqlMapper.COLUMN_ID+"=?", new String[]{String.valueOf(accountId)}, null);
            return (accounts.size() == 1) ? accounts.get(0) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createAccount(Account account) {
        SQLiteDatabase db = sqlConnection.getWritableDatabase();
        db.beginTransaction();
        try {
            sqlConnection.insert(this, account, db);

            Transaction transaction = new Transaction(account, account.getInitialBalance(), Transaction.TransactionType.DEPOSIT, "Initial Deposit");
            sqlConnection.insert(transaction, transaction, db);

            db.setTransactionSuccessful();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            db.endTransaction();
        }

        // see if we should run a strategy
        Class<? extends BaseStrategy> strategyClazz = account.getStrategy().getStrategyClazz();
        if (strategyClazz != null) {
            try {
                BaseStrategy strategy = BaseStrategy.createStrategy(strategyClazz,
                        context, networkRequestProvider, sqlConnection, settings);
                strategy.initialize(account);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing the strategy", e);
            }
        }
    }

    public void deleteAccount(Account account) {
        try {
            sqlConnection.delete(this, account);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }


    @Override
    public ContentValues getContentValues(Account account) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME, account.getName());
        values.put(COLUMN_DESCRIPTION, account.getDescription());
        values.put(COLUMN_INITIAL_BALANCE, account.getInitialBalance().getMicroCents());
        values.put(COLUMN_STRATEGY, account.getStrategy().name());
        values.put(COLUMN_AVAILABLE_FUNDS, account.getAvailableFunds().getMicroCents());
        values.put(COLUMN_EXCLUDE_FROM_TOTALS, account.getExcludeFromTotals() ? 1 : 0);

        return values;
    }

    @Override
    public void populate(Account account, Cursor cursor, Map<String, Integer> columnMap) {
        account.setId(cursor.getLong(columnMap.get(COLUMN_ID)));
        account.setName(cursor.getString(columnMap.get(COLUMN_NAME)));
        account.setDescription(cursor.getString(columnMap.get(COLUMN_DESCRIPTION)));
        account.setInitialBalance(new Money(cursor.getLong(columnMap.get(COLUMN_INITIAL_BALANCE))));
        account.setStrategy(Account.Strategy.valueOf(cursor.getString(columnMap.get(COLUMN_STRATEGY))));
        account.setAvailableFunds(new Money(cursor.getLong(columnMap.get(COLUMN_AVAILABLE_FUNDS))));
        account.setExcludeFromTotals(cursor.getInt(columnMap.get(COLUMN_EXCLUDE_FROM_TOTALS))==1);
    }


}

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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.balch.android.app.framework.sql.SqlMapper;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.account.strategies.BaseStrategy;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.SqliteModel;

import java.util.List;
import java.util.Map;

public class AccountSqliteModel extends SqliteModel implements SqlMapper<Account> {
    private static final String TAG = AccountSqliteModel.class.getSimpleName();

    public static final String TABLE_NAME = "account";

    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_INITIAL_BALANCE = "initial_balance";
    public static final String COLUMN_STRATEGY = "strategy";
    public static final String COLUMN_AVAILABLE_FUNDS = "available_funds";
    public static final String COLUMN_EXCLUDE_FROM_TOTALS = "exclude_from_totals";

    public AccountSqliteModel(ModelProvider modelProvider) {
        super(modelProvider);
    }

    public List<Account> getAllAccounts() {
        try {
            return getSqlConnection().query(this, Account.class, null, null, COLUMN_NAME + " COLLATE NOCASE");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Account getAccount(long accountId) {
        try {
            List<Account> accounts = getSqlConnection().query(this, Account.class, SqlMapper.COLUMN_ID+"=?", new String[]{String.valueOf(accountId)}, null);
            return (accounts.size() == 1) ? accounts.get(0) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createAccount(Account account) {
        SQLiteDatabase db = getSqlConnection().getWritableDatabase();
        db.beginTransaction();
        try {
            this.getSqlConnection().insert(this, account, db);

            Transaction transaction = new Transaction(account, account.initialBalance, Transaction.TransactionType.DEPOSIT, "Initial Deposit");
            getSqlConnection().insert(transaction, transaction, db);

            db.setTransactionSuccessful();

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            db.endTransaction();
        }

        // see if we should run a strategy
        Class<? extends BaseStrategy> strategyClazz = account.getStrategy().getStrategyClazz();
        if (strategyClazz != null) {
            try {
                BaseStrategy strategy = BaseStrategy.createStrategy(strategyClazz, this.getContext(), this.getModelFactory());
                strategy.initialize(account);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing the strategy", e);
            }
        }
    }

    public void deleteAccount(Account account) {
        try {
            this.getSqlConnection().delete(this, account);
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

        values.put(COLUMN_NAME, account.name);
        values.put(COLUMN_DESCRIPTION, account.description);
        values.put(COLUMN_INITIAL_BALANCE, account.initialBalance.getMicroCents());
        values.put(COLUMN_STRATEGY, account.strategy.name());
        values.put(COLUMN_AVAILABLE_FUNDS, account.availableFunds.getMicroCents());
        values.put(COLUMN_EXCLUDE_FROM_TOTALS, account.excludeFromTotals ? 1 : 0);

        return values;
    }

    @Override
    public void populate(Account account, Cursor cursor, Map<String, Integer> columnMap) {
        account.setId(cursor.getLong(columnMap.get(COLUMN_ID)));
        account.name = cursor.getString(columnMap.get(COLUMN_NAME));
        account.description = cursor.getString(columnMap.get(COLUMN_DESCRIPTION));
        account.initialBalance = new Money(cursor.getLong(columnMap.get(COLUMN_INITIAL_BALANCE)));
        account.strategy = Account.Strategy.valueOf(cursor.getString(columnMap.get(COLUMN_STRATEGY)));
        account.availableFunds = new Money(cursor.getLong(columnMap.get(COLUMN_AVAILABLE_FUNDS)));
        account.excludeFromTotals = cursor.getInt(columnMap.get(COLUMN_EXCLUDE_FROM_TOTALS))==1;
    }


}

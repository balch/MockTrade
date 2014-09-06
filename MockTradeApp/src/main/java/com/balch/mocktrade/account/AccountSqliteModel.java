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

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.balch.mocktrade.account.strategies.BaseStrategy;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.SqliteModel;

import java.util.List;

public class AccountSqliteModel extends SqliteModel  {
    private static final String TAG = AccountSqliteModel.class.getName();

    public AccountSqliteModel(ModelProvider modelProvider) {
        super(modelProvider);
    }

    public List<Account> getAllAccounts() {
        try {
            return getSqlConnection().query(Account.class, null, null, Account.SQL_NAME + " COLLATE NOCASE");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Account getAccount(long accountId) {
        try {
            List<Account> accounts = getSqlConnection().query(Account.class, Account._ID+"=?", new String[]{String.valueOf(accountId)}, null);
            return (accounts.size() == 1) ? accounts.get(0) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createAccount(Account account) {
        SQLiteDatabase db = getSqlConnection().getWritableDatabase();
        db.beginTransaction();
        try {
            this.getSqlConnection().insert(account, db);

            Transaction transaction = new Transaction(account, account.initialBalance, Transaction.TransactionType.DEPOSIT, "Initial Deposit");
            getSqlConnection().insert(transaction, db);

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
            this.getSqlConnection().delete(account);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

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

import com.balch.android.app.framework.bean.BaseBean;
import com.balch.android.app.framework.types.Money;

import java.io.Serializable;
import java.util.Map;

public class Transaction extends BaseBean implements Serializable {
    static public final String TABLE_NAME = "[transaction]";

    public static final String COLUMN_ACCOUNT_ID = "account_id";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_NOTES = "notes";

    protected Account account;
    protected Money amount;
    protected TransactionType type;
    protected String notes;

    public Transaction() {
    }

    public Transaction(Account account, Money amount, TransactionType type, String notes) {
        this.account = account;
        this.amount = amount;
        this.type = type;
        this.notes = notes;
    }

    @Override
    public String getTableName() {
        return Transaction.TABLE_NAME;
    }

    public enum TransactionType  {
        DEPOSIT,
        WITHDRAWAL;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Money getAmount() {
        return amount;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(COLUMN_ACCOUNT_ID, this.account.getId());
        values.put(COLUMN_AMOUNT, this.amount.getMicroCents());
        values.put(COLUMN_TYPE, this.type.name());
        values.put(COLUMN_NOTES, this.notes);

        return values;
    }

    @Override
    public void populate(Cursor cursor, Map<String, Integer> columnMap) {
        this.id = cursor.getLong(columnMap.get(COLUMN_ID));
        this.account = new Account();
        this.account.setId(cursor.getLong(columnMap.get(COLUMN_ACCOUNT_ID)));
        this.amount = new Money(cursor.getLong(columnMap.get(COLUMN_AMOUNT)));
        this.type = TransactionType.valueOf(cursor.getString(columnMap.get(COLUMN_TYPE)));
        this.notes = cursor.getString(columnMap.get(COLUMN_NOTES));
    }

}

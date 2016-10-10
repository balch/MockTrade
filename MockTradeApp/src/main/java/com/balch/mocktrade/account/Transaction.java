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
import android.os.Parcel;
import android.os.Parcelable;

import com.balch.android.app.framework.domain.DomainObject;
import com.balch.android.app.framework.sql.SqlMapper;
import com.balch.android.app.framework.types.Money;

import java.util.Map;

public class Transaction extends DomainObject implements SqlMapper<Transaction>, Parcelable {
    public static final String TABLE_NAME = "[transaction]";

    public static final String COLUMN_ACCOUNT_ID = "account_id";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_NOTES = "notes";

    private Account mAccount;
    private Money mAmount;
    private TransactionType mTransactionType;
    private String mNotes;

    public Transaction() {
    }

    public Transaction(Account account, Money amount, TransactionType type, String notes) {
        this.mAccount = account;
        this.mAmount = amount;
        this.mTransactionType = type;
        this.mNotes = notes;
    }

    protected Transaction(Parcel in) {
        super(in);
        mAccount = in.readParcelable(Account.class.getClassLoader());
        mAmount  = in.readParcelable(Money.class.getClassLoader());
        mTransactionType =  TransactionType.valueOf(in.readString());
        mNotes = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(mAccount, flags);
        dest.writeParcelable(mAmount, flags);
        dest.writeString(mTransactionType.name());
        dest.writeString(mNotes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Transaction> CREATOR = new Creator<Transaction>() {
        @Override
        public Transaction createFromParcel(Parcel in) {
            return new Transaction(in);
        }

        @Override
        public Transaction[] newArray(int size) {
            return new Transaction[size];
        }
    };

    @Override
    public String getTableName() {
        return Transaction.TABLE_NAME;
    }

    public enum TransactionType  {
        DEPOSIT,
        WITHDRAWAL
    }

    public Account getAccount() {
        return mAccount;
    }

    public void setAccount(Account account) {
        this.mAccount = account;
    }

    public Money getAmount() {
        return mAmount;
    }

    public void setAmount(Money amount) {
        this.mAmount = amount;
    }

    public TransactionType getType() {
        return mTransactionType;
    }

    public void setType(TransactionType type) {
        this.mTransactionType = type;
    }

    public String getNotes() {
        return mNotes;
    }

    public void setNotes(String notes) {
        this.mNotes = notes;
    }

    @Override
    public ContentValues getContentValues(Transaction transaction) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_ACCOUNT_ID, transaction.mAccount.getId());
        values.put(COLUMN_AMOUNT, transaction.mAmount.getMicroCents());
        values.put(COLUMN_TYPE, transaction.mTransactionType.name());
        values.put(COLUMN_NOTES, transaction.mNotes);

        return values;
    }

    @Override
    public void populate(Transaction transaction, Cursor cursor, Map<String, Integer> columnMap) {
        transaction.id = cursor.getLong(columnMap.get(COLUMN_ID));
        transaction.mAccount = new Account();
        transaction.mAccount.setId(cursor.getLong(columnMap.get(COLUMN_ACCOUNT_ID)));
        transaction.mAmount = new Money(cursor.getLong(columnMap.get(COLUMN_AMOUNT)));
        transaction.mTransactionType = TransactionType.valueOf(cursor.getString(columnMap.get(COLUMN_TYPE)));
        transaction.mNotes = cursor.getString(columnMap.get(COLUMN_NOTES));
    }

}

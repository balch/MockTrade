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


import com.balch.android.app.framework.bean.BaseBean;
import com.balch.android.app.framework.sql.annotations.SqlColumn;
import com.balch.android.app.framework.types.Money;

import java.io.Serializable;

public class Transaction extends BaseBean implements Serializable {
    static public final String TABLE_NAME = "[transaction]";

    @Override
    public String getTableName() {
        return Transaction.TABLE_NAME;
    }

    public enum TransactionType  {
        DEPOSIT,
        WITHDRAWAL;
   }

    public Transaction() {
    }

    public Transaction(Account account, Money amount, TransactionType type, String notes) {
        this.account = account;
        this.amount = amount;
        this.type = type;
        this.notes = notes;
    }

    @SqlColumn(name="account_id")
    protected Account account;

    @SqlColumn
    protected Money amount;

    @SqlColumn
    protected TransactionType type;

    @SqlColumn
    protected String notes;

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
}

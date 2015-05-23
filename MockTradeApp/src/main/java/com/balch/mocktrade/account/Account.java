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

import com.balch.android.app.framework.MetadataUtils;
import com.balch.android.app.framework.sql.SqlBean;
import com.balch.android.app.framework.bean.BeanEditState;
import com.balch.android.app.framework.bean.annotations.BeanColumnEdit;
import com.balch.android.app.framework.bean.annotations.BeanColumnNew;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.strategies.BaseStrategy;
import com.balch.mocktrade.account.strategies.DogsOfTheDow;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.portfolio.PerformanceItem;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Account extends SqlBean implements Serializable {
    public static final String TABLE_NAME = "account";

    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_INITIAL_BALANCE = "initial_balance";
    public static final String COLUMN_STRATEGY = "strategy";
    public static final String COLUMN_AVAILABLE_FUNDS = "available_funds";
    public static final String COLUMN_EXCLUDE_FROM_TOTALS = "exclude_from_totals";

    public static final String FLD_STRATEGY = "strategy";
    public static final String FLD_NAME = "name";

    @BeanColumnEdit(order = 1, labelResId = R.string.account_name_label, hints = {"MAX_CHARS=32","NOT_EMPTY=true"})
    @BeanColumnNew(order = 1, labelResId = R.string.account_name_label, hints = {"MAX_CHARS=32","NOT_EMPTY=true"})
    protected String name;

    @BeanColumnEdit(order = 2, labelResId = R.string.account_description_label, hints = {"MAX_CHARS=256","DISPLAY_LINES=2"})
    @BeanColumnNew(order = 2, labelResId = R.string.account_description_label, hints = {"MAX_CHARS=256","DISPLAY_LINES=2"})
    protected String description;

    @BeanColumnEdit(order = 3, labelResId = R.string.account_init_balance_label, state = BeanEditState.READONLY, hints = {"NON_NEGATIVE=true","HIDE_CENTS=true"})
    @BeanColumnNew(order = 3, labelResId = R.string.account_init_balance_label, hints = {"NON_NEGATIVE=true","HIDE_CENTS=true"})
    protected Money initialBalance;

    @BeanColumnEdit(order = 4, labelResId = R.string.account_strategy_label, state = BeanEditState.READONLY)
    @BeanColumnNew(order = 4,labelResId = R.string.account_strategy_label)
    protected Strategy strategy;

    protected Money availableFunds;

    @BeanColumnEdit(order = 5, labelResId = R.string.account_exclude_from_totals_label, state = BeanEditState.READONLY)
    @BeanColumnNew(order = 5,labelResId = R.string.account_exclude_from_totals_label)
    protected Boolean excludeFromTotals;

    @Override
    public String getTableName() {
        return Account.TABLE_NAME;
    }

    public Account() {
        this("", "", new Money(0), Strategy.NONE, false);
    }

    public Account(String name, String description, Money initialBalance, Strategy strategy, boolean excludeFromTotals) {
        this(name, description, initialBalance, initialBalance.clone(), strategy, excludeFromTotals);
    }

    public Account(String name, String description, Money initialBalance, Money availableFunds,
                   Strategy strategy, boolean excludeFromTotals) {
        this.name = name;
        this.description = description;
        this.initialBalance = initialBalance;
        this.availableFunds = availableFunds;
        this.strategy = strategy;
        this.excludeFromTotals = excludeFromTotals;
    }


    public void aggregate(Account account) {
        this.initialBalance.add(account.initialBalance);
        this.availableFunds.add(account.availableFunds);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Money getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(Money initialBalance) {
        this.initialBalance = initialBalance;
    }

    public Money getAvailableFunds() {
        return availableFunds;
    }

    public void setAvailableFunds(Money availableFunds) {
        this.availableFunds = availableFunds;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public Boolean getExcludeFromTotals() {
        return excludeFromTotals;
    }

    public void setExcludeFromTotals(Boolean excludeFromTotals) {
        this.excludeFromTotals = excludeFromTotals;
    }

    public PerformanceItem getPerformanceItem(List<Investment> investments) {
        Money currentBalance = new Money(this.getAvailableFunds().getMicroCents());
        Money todayChange = new Money(0);

        if (investments != null) {
            for (Investment i : investments) {
                currentBalance.add(i.getValue());

                if (i.isPriceCurrent()) {
                    todayChange.add(Money.subtract(i.getValue(),i.getPrevDayValue()));
                }
            }
        }

        return new PerformanceItem(this.initialBalance, currentBalance, todayChange);
    }

    @Override
    public String toString() {
        return "Account{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", initialBalance=" + initialBalance +
                ", availableFunds=" + availableFunds +
                ", strategy=" + strategy +
                ", excludeFromTotals=" + excludeFromTotals +
                '}';
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME, this.name);
        values.put(COLUMN_DESCRIPTION, this.description);
        values.put(COLUMN_INITIAL_BALANCE, this.initialBalance.getMicroCents());
        values.put(COLUMN_STRATEGY, this.strategy.name());
        values.put(COLUMN_AVAILABLE_FUNDS, this.availableFunds.getMicroCents());
        values.put(COLUMN_EXCLUDE_FROM_TOTALS, this.excludeFromTotals ? 1 : 0);

        return values;
    }

    @Override
    public void populate(Cursor cursor, Map<String, Integer> columnMap) {
        this.id = cursor.getLong(columnMap.get(COLUMN_ID));
        this.name = cursor.getString(columnMap.get(COLUMN_NAME));
        this.description = cursor.getString(columnMap.get(COLUMN_DESCRIPTION));
        this.initialBalance = new Money(cursor.getLong(columnMap.get(COLUMN_INITIAL_BALANCE)));
        this.strategy = Strategy.valueOf(cursor.getString(columnMap.get(COLUMN_STRATEGY)));
        this.availableFunds = new Money(cursor.getLong(columnMap.get(COLUMN_AVAILABLE_FUNDS)));
        this.excludeFromTotals = cursor.getInt(columnMap.get(COLUMN_EXCLUDE_FROM_TOTALS))==1;
    }

    public enum Strategy implements MetadataUtils.EnumResource {
        NONE(null),
        DOGS_OF_THE_DOW(DogsOfTheDow.class);

        protected final Class<? extends BaseStrategy> strategyClazz;

        Strategy(Class<? extends BaseStrategy> strategyClazz) {
            this.strategyClazz = strategyClazz;
        }

        public Class<? extends BaseStrategy> getStrategyClazz() {
            return strategyClazz;
        }

        @Override
        public int getListResId() {
            return R.array.account_strategy_display_values;
        }
    }


}

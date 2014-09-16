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


import com.balch.android.app.framework.MetadataUtils;
import com.balch.android.app.framework.bean.BaseBean;
import com.balch.android.app.framework.bean.BeanEditState;
import com.balch.android.app.framework.bean.annotations.BeanColumnEdit;
import com.balch.android.app.framework.bean.annotations.BeanColumnNew;
import com.balch.android.app.framework.sql.annotations.SqlColumn;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.strategies.BaseStrategy;
import com.balch.mocktrade.account.strategies.DogsOfTheDow;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.portfolio.PerformanceItem;

import java.io.Serializable;
import java.util.List;

public class Account extends BaseBean implements Serializable {
    static public final String TABLE_NAME = "account";

    static public final String SQL_NAME = "name";

    static public final String FLD_STRATEGY = "strategy";
    static public final String FLD_NAME = "name";

    public enum Strategy  implements MetadataUtils.EnumResource {
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

    @SqlColumn
    @BeanColumnEdit(order = 1, labelResId = R.string.account_name_label, hints = {"MAX_CHARS=32","NOT_EMPTY=true"})
    @BeanColumnNew(order = 1, labelResId = R.string.account_name_label, hints = {"MAX_CHARS=32","NOT_EMPTY=true"})
    protected String name;

    @SqlColumn
    @BeanColumnEdit(order = 2, labelResId = R.string.account_description_label, hints = {"MAX_CHARS=256","DISPLAY_LINES=2"})
    @BeanColumnNew(order = 2, labelResId = R.string.account_description_label, hints = {"MAX_CHARS=256","DISPLAY_LINES=2"})
    protected String description;

    @SqlColumn(name = "initial_balance")
    @BeanColumnEdit(order = 3, labelResId = R.string.account_init_balance_label, state = BeanEditState.READONLY, hints = {"NON_NEGATIVE=true","HIDE_CENTS=true"})
    @BeanColumnNew(order = 3, labelResId = R.string.account_init_balance_label, hints = {"NON_NEGATIVE=true","HIDE_CENTS=true"})
    protected Money initialBalance;

    @SqlColumn
    @BeanColumnEdit(order = 4, labelResId = R.string.account_strategy_label, state = BeanEditState.READONLY)
    @BeanColumnNew(order = 4,labelResId = R.string.account_strategy_label)
    protected Strategy strategy;

    @SqlColumn(name = "available_funds")
    protected Money availableFunds;

    @SqlColumn(name = "exclude_from_totals")
    @BeanColumnEdit(order = 5, labelResId = R.string.account_exclude_from_totals_label, state = BeanEditState.READONLY)
    @BeanColumnNew(order = 5,labelResId = R.string.account_exclude_from_totals_label)
    protected Boolean excludeFromTotals;

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

}

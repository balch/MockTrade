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
import android.text.format.DateUtils;
import android.util.Log;

import com.balch.android.app.framework.MetadataUtils;
import com.balch.android.app.framework.bean.BaseBean;
import com.balch.android.app.framework.types.ISO8601DateTime;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.order.Order;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

public class Investment extends BaseBean {
    public static final String TAG = Investment.class.getSimpleName();

    public static final String TABLE_NAME = "investment";

    public static final String COLUMN_ACCOUNT_ID = "account_id";
    public static final String COLUMN_SYMBOL = "symbol";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_EXCHANGE = "exchange";
    public static final String COLUMN_COST_BASIS = "cost_basis";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_LAST_TRADE_TIME = "last_trade_time";
    public static final String COLUMN_PREV_DAY_CLOSE = "prev_day_close";
    public static final String COLUMN_QUANTITY = "quantity";

    protected Account account;
    protected String symbol;
    protected InvestmentStatus status;
    protected String description;
    protected String exchange;
    protected Money costBasis;
    protected Money price;
    protected ISO8601DateTime lastTradeTime;
    protected Money prevDayClose;
    protected Long quantity;

    public Investment() {
    }

    public Investment(Account account, String symbol,
                      InvestmentStatus status, String description, String exchange,
                      Money costBasis, Money price, Date lastTradeTime, Long quantity) {
        this.account = account;
        this.symbol = symbol;
        this.status = status;
        this.description = description;
        this.exchange = exchange;
        this.costBasis = costBasis;
        this.price = price;
        this.prevDayClose = price.clone();
        this.quantity = quantity;
        this.lastTradeTime = new ISO8601DateTime(lastTradeTime);
    }

    @Override
    public String getTableName() {
        return Investment.TABLE_NAME;
    }


    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public InvestmentStatus getStatus() {
        return status;
    }

    public void setStatus(InvestmentStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public Money getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(Money costBasis) {
        this.costBasis = costBasis;
    }

    public Money getPrice() {
        return price;
    }

    public void setPrice(Money price, Date lastTradeTime) {
        this.price = price;
        this.lastTradeTime = new ISO8601DateTime(lastTradeTime);
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Money getValue() {
        return Money.multiply(this.price, this.quantity);
    }

    public Money getPrevDayValue() {
        return Money.multiply(this.prevDayClose, this.quantity);
    }

    public Money getPrevDayClose() {
        return prevDayClose;
    }

    public void setPrevDayClose(Money prevDayClose) {
        this.prevDayClose = prevDayClose;
    }

    public ISO8601DateTime getLastTradeTime() {
        return lastTradeTime;
    }

    public void aggregateOrder(Order order, Money price) {
        // note: this work with sell orders!!!
        this.costBasis.add(order.getCost(price));
        this.quantity += order.getQuantityDelta();
    }

    public boolean isPriceCurrent() {
        return DateUtils.isToday(this.lastTradeTime.getDate().getTime());
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(COLUMN_ACCOUNT_ID, this.account.getId());
        values.put(COLUMN_SYMBOL, this.symbol);
        values.put(COLUMN_STATUS, this.status.name());
        values.put(COLUMN_DESCRIPTION, this.description);
        values.put(COLUMN_EXCHANGE, this.exchange);
        values.put(COLUMN_COST_BASIS, this.costBasis.getMicroCents());
        values.put(COLUMN_PRICE, this.price.getMicroCents());
        values.put(COLUMN_LAST_TRADE_TIME, this.lastTradeTime.toString());
        values.put(COLUMN_PREV_DAY_CLOSE, this.prevDayClose.getMicroCents());
        values.put(COLUMN_QUANTITY, this.quantity);

        return values;
    }

    @Override
    public void populate(Cursor cursor, Map<String, Integer> columnMap) {
        this.id = cursor.getLong(columnMap.get(COLUMN_ID));
        this.account = new Account();
        this.account.setId(cursor.getLong(columnMap.get(COLUMN_ACCOUNT_ID)));
        this.symbol = cursor.getString(columnMap.get(COLUMN_SYMBOL));
        this.status = InvestmentStatus.valueOf(cursor.getString(columnMap.get(COLUMN_STATUS)));
        this.description = cursor.getString(columnMap.get(COLUMN_DESCRIPTION));
        this.exchange = cursor.getString(columnMap.get(COLUMN_EXCHANGE));
        this.costBasis = new Money(cursor.getLong(columnMap.get(COLUMN_COST_BASIS)));
        this.price = new Money(cursor.getLong(columnMap.get(COLUMN_PRICE)));

        try {
            this.lastTradeTime = new ISO8601DateTime(cursor.getString(columnMap.get(COLUMN_LAST_TRADE_TIME)));
        } catch (ParseException ex) {
            Log.e(TAG, "Error reading LastTrade time", ex);
        }
        this.prevDayClose = new Money(cursor.getLong(columnMap.get(COLUMN_PREV_DAY_CLOSE)));
        this.quantity = cursor.getLong(columnMap.get(COLUMN_QUANTITY));
    }


    public enum InvestmentStatus implements MetadataUtils.EnumResource {
        OPEN,
        CLOSED;

        @Override
        public int getListResId() {
            return R.array.order_strategy_display_values;
        }
    }



}
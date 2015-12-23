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

import android.text.format.DateUtils;

import com.balch.android.app.framework.MetadataUtils;
import com.balch.android.app.framework.domain.DomainObject;
import com.balch.android.app.framework.types.ISO8601DateTime;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.order.Order;

import java.util.Date;

public class Investment extends DomainObject {
    public static final String TAG = Investment.class.getSimpleName();

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

    public enum InvestmentStatus implements MetadataUtils.EnumResource {
        OPEN,
        CLOSED;

        @Override
        public int getListResId() {
            return R.array.order_strategy_display_values;
        }
    }
}
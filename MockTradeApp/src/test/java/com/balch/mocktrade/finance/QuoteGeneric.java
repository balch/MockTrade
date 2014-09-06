/*
 * Author: Balch
 * Created: 9/6/14 9:42 AM
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

package com.balch.mocktrade.finance;

import com.balch.android.app.framework.types.Money;

import java.util.Date;

public class QuoteGeneric implements Quote {
    protected String symbol;
    protected String name;
    protected String exchange;
    protected Money price;
    protected Date lastTradeTime;
    protected Money previousClose;
    protected Money dividendPerShare;


    public QuoteGeneric() {
        this("", "", "", new Money(0), new Date(), new Money(0), new Money(0));
    }

    public QuoteGeneric(String symbol, String name,
                        String exchange, Money price, Date lastTradeTime,
                        Money previousClose, Money dividendPerShare) {
        this.symbol = symbol;
        this.name = name;
        this.exchange = exchange;
        this.price = price;
        this.lastTradeTime = lastTradeTime;
        this.previousClose = previousClose;
        this.dividendPerShare = dividendPerShare;
    }

    @Override
    public Money getPrice() {
        return this.price;
    }

    @Override
    public void setPrice(Money price) {
        this.price = price;
    }

    @Override
    public String getSymbol() {
        return this.symbol;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getExchange() {
        return this.exchange;
    }

    @Override
    public Date getLastTradeTime() {
        return this.lastTradeTime;
    }

    @Override
    public void setLastTradeTime(Date time) {
        this.lastTradeTime = time;
    }

    @Override
    public Money getPreviousClose() {
        return this.previousClose;
    }

    @Override
    public boolean isDelayed() {
        return false;
    }

    @Override
    public int getDelaySeconds() {
        return 0;
    }

    @Override
    public Money getDividendPerShare() {
        return this.dividendPerShare;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public void setPreviousClose(Money previousClose) {
        this.previousClose = previousClose;
    }

    public void setDividendPerShare(Money dividendPerShare) {
        this.dividendPerShare = dividendPerShare;
    }
}
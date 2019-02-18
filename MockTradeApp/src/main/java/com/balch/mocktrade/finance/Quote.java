/*
 * Author: Balch
 * Created: 9/11/17 10:11 PM
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
 * Copyright (C) 2017
 *
 */

package com.balch.mocktrade.finance;

import androidx.annotation.VisibleForTesting;

import com.balch.android.app.framework.types.Money;

import java.util.Date;

public class Quote {
    private String symbol;
    private String name;
    private String exchange;
    private Money price;
    private Date lastTradeTime;
    private Money previousClose;
    private Money dividendPerShare;

    public Quote(String symbol, String name,
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

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Quote(Money price) {
        this.price = price;
    }

    public Money getPrice() {
        return this.price;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public String getName() {
        return this.name;
    }

    public String getExchange() {
        return this.exchange;
    }

    public Date getLastTradeTime() {
        return this.lastTradeTime;
    }

    public Money getPreviousClose() {
        return this.previousClose;
    }

    public Money getDividendPerShare() {
        return this.dividendPerShare;
    }

}
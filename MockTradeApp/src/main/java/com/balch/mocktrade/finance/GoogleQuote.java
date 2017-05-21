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

package com.balch.mocktrade.finance;

import com.balch.android.app.framework.types.Money;

import java.util.Date;

public class GoogleQuote implements Quote {
    static private final String TAG = GoogleQuote.class.getSimpleName();

    private Money price;
    private Date lastTradeTime;
    private String symbol;
    private final Money previousClose;
    private final Money dividendPerShare;
    private final String name;
    private final String exchange;

    public GoogleQuote(Money price, Money previousClose,
                       Money dividendPerShare, String symbol,
                       String name, String exchange, Date lastTradeTime) {
        this.price = price;
        this.previousClose = previousClose;
        this.dividendPerShare = dividendPerShare;
        this.symbol = symbol;
        this.name = name;
        this.exchange = exchange;
        this.lastTradeTime = lastTradeTime;
    }

    @Override
    public Money getPrice() {
        return price;
    }

    @Override
    public void setPrice(Money price) {
        this.price = price;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getExchange() {
        return exchange;
    }

    @Override
    public Date getLastTradeTime() {
        return lastTradeTime;
    }

    @Override
    public void setLastTradeTime(Date time) {
        lastTradeTime = time;
    }

    @Override
    public Money getPreviousClose() {
        return previousClose;
    }

    @Override
    public boolean isDelayed() {
        return  (getDelaySeconds() > 0);
    }

    @Override
    public int getDelaySeconds() {
        return 0;
    }

    @Override
    public Money getDividendPerShare() {
        return dividendPerShare;
    }

}

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

import android.text.TextUtils;
import android.util.Log;

import com.balch.android.app.framework.types.Money;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class QuoteYahooFinance implements Quote {
    static private final String TAG = QuoteYahooFinance.class.getSimpleName();

    private Map<String, String> quoteData = new HashMap<>();

    private final static String DIVIDEND_SHARE="DividendShare";
    private final static String LAST_TRADE_DATE="LastTradeDate";
    private final static String LAST_TRADE_PRICE_ONLY="LastTradePriceOnly";
    private final static String NAME="Name";
    private final static String PREVIOUS_CLOSE="PreviousClose";
    private final static String SYMBOL="Symbol";
    private final static String LAST_TRADE_TIME="LastTradeTime";
    private final static String STOCK_EXCHANGE="StockExchange";
    private final static String ERROR_SYMBOL_INVALID="ErrorIndicationreturnedforsymbolchangedinvalid";

    @Override
    public Money getPrice() {
        return new Money(quoteData.get(LAST_TRADE_PRICE_ONLY));
    }

    @Override
    public void setPrice(Money price) {
        quoteData.put(LAST_TRADE_PRICE_ONLY, String.valueOf(price.getDollars()));
    }

    @Override
    public String getSymbol() {
        return quoteData.get(SYMBOL);
    }

    @Override
    public String getName() {
        return quoteData.get(NAME);
    }

    @Override
    public String getExchange() {
        return quoteData.get(STOCK_EXCHANGE);
    }

    @Override
    public Date getLastTradeTime() {
        DateFormat df = new SimpleDateFormat("M/d/yy h:mma", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        String dateStr = quoteData.get(LAST_TRADE_DATE) + " " + quoteData.get(LAST_TRADE_TIME).toUpperCase();
        try {
            return df.parse(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date:" + dateStr, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setLastTradeTime(Date time) {
        DateFormat df = new SimpleDateFormat("M/d/yy h:mma", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        String dateStr = df.format(time);
        String[] parts = dateStr.split(" ");
        quoteData.put(LAST_TRADE_DATE, parts[0]);
        quoteData.put(LAST_TRADE_TIME, parts[1]);

    }

    @Override
    public Money getPreviousClose() {
        return new Money(quoteData.get(PREVIOUS_CLOSE));
    }

    public static QuoteYahooFinance fromJSONObject(JSONObject jsonObject) throws JSONException {
        QuoteYahooFinance quote = new QuoteYahooFinance();
        Iterator iter = jsonObject.keys();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            if (!jsonObject.isNull(key)) {
                quote.quoteData.put(key, jsonObject.getString(key));
            }
        }

        String error = quote.quoteData.get(QuoteYahooFinance.ERROR_SYMBOL_INVALID);
        if (!TextUtils.isEmpty(error)) {
            throw new JSONException(error);
        }

        return quote;

    }

    @Override
    public boolean isDelayed() {
        return  (getDelaySeconds() > 0);
    }

    @Override
    public int getDelaySeconds() {
        return 15 * 60;
    }

    @Override
    public Money getDividendPerShare() {
        return new Money(quoteData.get(DIVIDEND_SHARE));
    }

}

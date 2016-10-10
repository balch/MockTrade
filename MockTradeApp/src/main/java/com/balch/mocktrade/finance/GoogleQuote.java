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

import android.util.Log;

import com.balch.android.app.framework.types.ISO8601DateTime;
import com.balch.android.app.framework.types.Money;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GoogleQuote implements Quote {
    static private final String TAG = GoogleQuote.class.getSimpleName();

    private final static String LAST_CLOSE_PRICE ="pcls_fix";
    private final static String LAST_TRADE_PRICE_ONLY ="l_fix";
    private final static String SYMBOL ="t";
    private final static String LAST_TRADE_TIME ="lt_dts"; // 2014-09-04T10:32:44Z  in EST
    private final static String NAME ="name";
    private final static String EXCHANGE ="e";
    private final static String DIVIDEND_PER_SHARE ="div";

    private Money mPrice;
    private Date mLastTradeTime;
    private final Money mPreviousClose;
    private final Money mDividendPerShare;
    private final String mSymbol;
    private final String mName;
    private final String mExchange;

    public GoogleQuote(Money price, Money previousClose,
                       Money dividendPerShare, String symbol,
                       String name, String exchange, Date lastTradeTime) {
        this.mPrice = price;
        this.mPreviousClose = previousClose;
        this.mDividendPerShare = dividendPerShare;
        this.mSymbol = symbol;
        this.mName = name;
        this.mExchange = exchange;
        this.mLastTradeTime = lastTradeTime;
    }

    @Override
    public Money getPrice() {
        return mPrice;
    }

    @Override
    public void setPrice(Money price) {
        mPrice = price;
    }

    @Override
    public String getSymbol() {
        return mSymbol;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public String getExchange() {
        return mExchange;
    }

    @Override
    public Date getLastTradeTime() {
        return mLastTradeTime;
    }

    @Override
    public void setLastTradeTime(Date time) {
        mLastTradeTime = time;
    }

    @Override
    public Money getPreviousClose() {
        return mPreviousClose;
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
        return mDividendPerShare;
    }

    public static GoogleQuote fromJSONObject(JSONObject jsonObject) throws JSONException {

        Money price = new Money(jsonObject.optString(LAST_TRADE_PRICE_ONLY));
        Money previousClose = new Money(jsonObject.optString(LAST_CLOSE_PRICE));
        Money dividendPerShare = new Money(jsonObject.optString(DIVIDEND_PER_SHARE));
        String name = jsonObject.optString(NAME);
        String symbol = jsonObject.optString(SYMBOL);
        String exchange = jsonObject.optString(EXCHANGE);
        Date lastTradeTime =  getDateFromISO8601(jsonObject.optString(LAST_TRADE_TIME));

        return new GoogleQuote(price, previousClose, dividendPerShare, symbol,
                name, exchange, lastTradeTime);
    }

    private static Date getDateFromISO8601(String dateStr) {
        TimeZone ny_tz = TimeZone.getTimeZone("America/New_York");
        Calendar ny_cal = Calendar.getInstance(ny_tz);
        int offset_mins = (ny_cal.get(Calendar.ZONE_OFFSET) + ny_cal.get(Calendar.DST_OFFSET)) / 60000;

        dateStr = dateStr.replace("Z", String.format(Locale.getDefault(), "%s%02d:%02d", (offset_mins >= 0) ? "+" : "-", Math.abs(offset_mins / 60), Math.abs(offset_mins % 60)));
        try {
            return ISO8601DateTime.toDate(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date:" + dateStr, e);
            throw new RuntimeException(e);
        }
    }
}

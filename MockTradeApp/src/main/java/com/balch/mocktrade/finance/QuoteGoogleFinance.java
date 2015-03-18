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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

public class QuoteGoogleFinance implements Quote {
    static private final String TAG = QuoteGoogleFinance.class.getName();

    protected Map<String, String> data = new HashMap<String, String>();

    protected final static String LastTradePriceOnly="l_cur";
    protected final static String Symbol="t";
    protected final static String LastTradeTime="lt_dts"; // 2014-09-04T10:32:44Z  in EST

    @Override
    public Money getPrice() {
        return new Money(this.data.get(this.LastTradePriceOnly));
    }

    @Override
    public void setPrice(Money price) {
        this.data.put(this.LastTradePriceOnly, String.valueOf(price.getDollars()));
    }

    @Override
    public String getSymbol() {
        return this.data.get(this.Symbol);
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getExchange() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getLastTradeTime() {
        TimeZone ny_tz = TimeZone.getTimeZone("America/New_York");
        Calendar ny_cal = Calendar.getInstance(ny_tz);
        int offset_mins = (ny_cal.get(Calendar.ZONE_OFFSET) + ny_cal.get(Calendar.DST_OFFSET))/60000;

        String dateStr = this.data.get(LastTradeTime);
        dateStr = dateStr.replace("Z", String.format("%s%02d:%02d",(offset_mins>=0)?"+":"-", Math.abs(offset_mins/60), Math.abs(offset_mins%60)));
        try {
            return ISO8601DateTime.toDate(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date:" + dateStr, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setLastTradeTime(Date time) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Money getPreviousClose() {
        throw new UnsupportedOperationException();
    }

    public static QuoteGoogleFinance fromJSONObject(JSONObject jsonObject) throws JSONException {
        QuoteGoogleFinance quote = new QuoteGoogleFinance();
        Iterator iter = jsonObject.keys();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            if (!jsonObject.isNull(key)) {
                quote.data.put(key, jsonObject.getString(key));
            }
        }

        return quote;

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
        throw new UnsupportedOperationException();
    }

}

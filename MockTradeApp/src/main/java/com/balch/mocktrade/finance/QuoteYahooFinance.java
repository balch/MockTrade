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
    static private final String TAG = QuoteYahooFinance.class.getName();

    protected Map<String, String> data = new HashMap<String, String>();

    protected final static String Ask="Ask";
    protected final static String AverageDailyVolume="AverageDailyVolume";
    protected final static String Bid="Bid";
    protected final static String AskRealtime="AskRealtime";
    protected final static String BidRealtime="BidRealtime";
    protected final static String BookValue="BookValue";
    protected final static String Change_PercentChange="Change_PercentChange";
    protected final static String Change="Change";
    protected final static String Currency="Currency";
    protected final static String AfterHoursChangeRealtime="AfterHoursChangeRealtime";
    protected final static String DividendShare="DividendShare";
    protected final static String LastTradeDate="LastTradeDate";
    protected final static String EarningsShare="EarningsShare";
    protected final static String EPSEstimateCurrentYear="EPSEstimateCurrentYear";
    protected final static String EPSEstimateNextYear="EPSEstimateNextYear";
    protected final static String EPSEstimateNextQuarter="EPSEstimateNextQuarter";
    protected final static String DaysLow="DaysLow";
    protected final static String DaysHigh="DaysHigh";
    protected final static String YearLow="YearLow";
    protected final static String YearHigh="YearHigh";
    protected final static String MarketCapitalization="MarketCapitalization";
    protected final static String EBITDA="EBITDA";
    protected final static String ChangeFromYearLow="ChangeFromYearLow";
    protected final static String PercentChangeFromYearLow="PercentChangeFromYearLow";
    protected final static String ChangePercentRealtime="ChangePercentRealtime";
    protected final static String ChangeFromYearHigh="ChangeFromYearHigh";
    protected final static String PercebtChangeFromYearHigh="PercebtChangeFromYearHigh";
    protected final static String LastTradeWithTime="LastTradeWithTime";
    protected final static String LastTradePriceOnly="LastTradePriceOnly";
    protected final static String DaysRange="DaysRange";
    protected final static String DaysRangeRealtime="DaysRangeRealtime";
    protected final static String FiftydayMovingAverage="FiftydayMovingAverage";
    protected final static String TwoHundreddayMovingAverage="TwoHundreddayMovingAverage";
    protected final static String ChangeFromTwoHundreddayMovingAverage="ChangeFromTwoHundreddayMovingAverage";
    protected final static String PercentChangeFromTwoHundreddayMovingAverage="PercentChangeFromTwoHundreddayMovingAverage";
    protected final static String ChangeFromFiftydayMovingAverage="ChangeFromFiftydayMovingAverage";
    protected final static String PercentChangeFromFiftydayMovingAverage="PercentChangeFromFiftydayMovingAverage";
    protected final static String Name="Name";
    protected final static String Open="Open";
    protected final static String PreviousClose="PreviousClose";
    protected final static String ChangeinPercent="ChangeinPercent";
    protected final static String PriceSales="PriceSales";
    protected final static String PriceBook="PriceBook";
    protected final static String ExDividendDate="ExDividendDate";
    protected final static String PERatio="PERatio";
    protected final static String DividendPayDate="DividendPayDate";
    protected final static String PEGRatio="PEGRatio";
    protected final static String PriceEPSEstimateCurrentYear="PriceEPSEstimateCurrentYear";
    protected final static String PriceEPSEstimateNextYear="PriceEPSEstimateNextYear";
    protected final static String Symbol="Symbol";
    protected final static String ShortRatio="ShortRatio";
    protected final static String LastTradeTime="LastTradeTime";
    protected final static String TickerTrend="TickerTrend";
    protected final static String OneyrTargetPrice="OneyrTargetPrice";
    protected final static String Volume="Volume";
    protected final static String YearRange="YearRange";
    protected final static String DaysValueChange="DaysValueChange";
    protected final static String DaysValueChangeRealtime="DaysValueChangeRealtime";
    protected final static String StockExchange="StockExchange";
    protected final static String DividendYield="DividendYield";
    protected final static String PercentChange="PercentChange";
    protected final static String ErrorIndicationreturnedforsymbolchangedinvalid="ErrorIndicationreturnedforsymbolchangedinvalid";

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
        return this.data.get(this.Name);
    }

    @Override
    public String getExchange() {
        return this.data.get(this.StockExchange);
    }

    @Override
    public Date getLastTradeTime() {
        DateFormat df = new SimpleDateFormat("M/d/yy h:mma", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        String dateStr = this.data.get(LastTradeDate) + " " + this.data.get(LastTradeTime).toUpperCase();
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
        this.data.put(LastTradeDate, parts[0]);
        this.data.put(LastTradeTime, parts[1]);

    }

    @Override
    public Money getPreviousClose() {
        return new Money(this.data.get(this.PreviousClose));
    }

    public static QuoteYahooFinance fromJSONObject(JSONObject jsonObject) throws JSONException {
        QuoteYahooFinance quote = new QuoteYahooFinance();
        Iterator iter = jsonObject.keys();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            if (!jsonObject.isNull(key)) {
                quote.data.put(key, jsonObject.getString(key));
            }
        }

        String error = quote.data.get(QuoteYahooFinance.ErrorIndicationreturnedforsymbolchangedinvalid);
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
        return new Money(this.data.get(this.DividendShare));
    }

}

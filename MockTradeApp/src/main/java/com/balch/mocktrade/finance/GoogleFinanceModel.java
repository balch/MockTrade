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

import android.content.Context;
import android.util.Log;

import com.balch.android.app.framework.types.ISO8601DateTime;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.settings.Settings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

public class GoogleFinanceModel implements FinanceModel {
    private static final String TAG = GoogleFinanceModel.class.getSimpleName();

    private final static String LAST_CLOSE_PRICE ="pcls_fix";
    private final static String LAST_TRADE_PRICE_ONLY ="l_fix";
    private final static String SYMBOL ="t";
    private final static String LAST_TRADE_TIME ="lt_dts"; // 2014-09-04T10:32:44Z  in EST
    private final static String NAME ="name";
    private final static String EXCHANGE ="e";
    private final static String DIVIDEND_PER_SHARE ="div";

    private final GoogleFinanceApi googleFinanceApi;
    private final FinanceManager mFinanceManager;

    public GoogleFinanceModel(Context context, GoogleFinanceApi googleFinanceApi,
                              Settings settings) {
        this.googleFinanceApi = googleFinanceApi;
        this.mFinanceManager = new FinanceManager(context.getApplicationContext(), settings);
    }

    @Override
    public Observable<Quote> getQuote(final String symbol) {
        return getQuotes(Collections.singletonList(symbol))
                .map(new Function<Map<String, Quote>, Quote>() {
                    @Override
                    public Quote apply(@NonNull Map<String, Quote> quoteMap) throws Exception {
                        return quoteMap.get(symbol);
                    }
                });
    }

    @Override
    public Observable<Map<String, Quote>> getQuotes(final List<String> symbols) {

        final Set<String> uniqueSymbols = getUniqueSymbols(symbols);
        String symbolString = getDelimitedSymbols(uniqueSymbols);

        return googleFinanceApi.getQuotes(symbolString)
                .map(new Function<String, List<Quote>>() {
                    @Override
                    public List<Quote> apply(@NonNull String s) throws Exception {
                        return parseQuotes(s);
                    }
                })
                .map(new Function<List<Quote>, Map<String, Quote>>() {
                    @Override
                    public Map<String, Quote> apply(@NonNull List<Quote> quotes) throws Exception {
                        Map quoteMap = new HashMap<>(uniqueSymbols.size());
                        if (quotes.size() == uniqueSymbols.size()) {
                            Iterator<String> symbolIterator = uniqueSymbols.iterator();
                            for (Quote quote : quotes) {
                                // fix issue when returned symbol does not match, check LMT.WD
                                quote.setSymbol(symbolIterator.next());
                                quoteMap.put(quote.getSymbol(), quote);
                            }
                        }
                       return quoteMap;
                    }
                });
    }

    @Override
    public boolean isMarketOpen() {
        return mFinanceManager.isMarketOpen();
    }

    @Override
    public Date nextMarketOpen() {
        return mFinanceManager.nextMarketOpen();
    }

    @Override
    public boolean isInPollTime() {
        return mFinanceManager.isInPollTime();
    }

    @Override
    public void setQuoteServiceAlarm() {
        mFinanceManager.setQuoteServiceAlarm();
    }

    private Set<String> getUniqueSymbols(List<String> symbols) {
        Set<String> uniqueSymbols = new HashSet<>(symbols.size());
        uniqueSymbols.addAll(symbols);
        return uniqueSymbols;
    }

    private String getDelimitedSymbols(Set<String> symbols) {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (String s : symbols) {
            if (!isFirst) {
                builder.append(",");
            }
            builder.append(s.toUpperCase());

            isFirst = false;
        }
        return builder.toString();
    }


    private List<Quote> parseQuotes(String jsonString) {

        String json = jsonString.trim();
        if (json.startsWith("//")) {
            json = json.substring(2);
        }

        JsonElement root = new JsonParser().parse(json);
        List<Quote> quotes = new ArrayList<>();

        if (root.isJsonArray()) {
            JsonArray jsonQuotes = root.getAsJsonArray();
            for (JsonElement element : jsonQuotes) {
                quotes.add(parseQuote(element.getAsJsonObject()));
            }
        }
        return quotes;
    }

    private GoogleQuote parseQuote(JsonObject jsonObject) {

        Money price = new Money(getJsonString(jsonObject, LAST_TRADE_PRICE_ONLY));
        Money previousClose = new Money(getJsonString(jsonObject, LAST_CLOSE_PRICE));
        Money dividendPerShare = new Money(getJsonString(jsonObject, DIVIDEND_PER_SHARE));
        String name = getJsonString(jsonObject, NAME);
        String symbol = getJsonString(jsonObject, SYMBOL);
        String exchange = getJsonString(jsonObject, EXCHANGE);
        Date lastTradeTime =  getDateFromISO8601(getJsonString(jsonObject, LAST_TRADE_TIME));

        return new GoogleQuote(price, previousClose, dividendPerShare, symbol, name, exchange, lastTradeTime);
    }

    private String getJsonString(JsonObject jsonObject, String key) {
        return jsonObject.has(key) ? jsonObject.get(key).getAsString() :  null;
    }

    private Date getDateFromISO8601(String dateStr) {
        TimeZone ny_tz = TimeZone.getTimeZone("America/New_York");
        Calendar ny_cal = Calendar.getInstance(ny_tz);
        int offset_mins = (ny_cal.get(Calendar.ZONE_OFFSET) + ny_cal.get(Calendar.DST_OFFSET)) / 60000;

        dateStr = dateStr.replace("Z", String.format(Locale.getDefault(), "%s%02d:%02d",
                (offset_mins >= 0) ? "+" : "-", Math.abs(offset_mins / 60), Math.abs(offset_mins % 60)));
        try {
            return ISO8601DateTime.toDate(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date:" + dateStr, e);
            throw new RuntimeException(e);
        }
    }

}

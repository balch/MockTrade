/*
 * Author: Balch
 * Created: 9/11/17 9:09 PM
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

import android.content.Context;
import android.util.Log;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.settings.Settings;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;

public class FinanceModelImpl implements FinanceModel {
    private static final String TAG = FinanceModelImpl.class.getSimpleName();

    private final YahooFinanceApi financeApi;
    private final FinanceManager mFinanceManager;

    private static final DateTimeFormatter YAHOO_DATE_FORMAT = DateTimeFormat
            .forPattern("MM/dd/yyyy hh:mma")
            .withZone(DateTimeZone.forID("America/New_York"));

    public FinanceModelImpl(Context context, YahooFinanceApi financeApi,
                            Settings settings) {
        this.financeApi = financeApi;
        this.mFinanceManager = new FinanceManager(context.getApplicationContext(), settings);
    }

    @Override
    public Observable<Quote> getQuote(final String symbol) {
        return getQuotes(Collections.singletonList(symbol))
                .map(quoteMap -> quoteMap.get(symbol));
    }

    @Override
    public Observable<Map<String, Quote>> getQuotes(final List<String> symbols) {

        final Set<String> uniqueSymbols = getUniqueSymbols(symbols);
        String symbolString = getDelimitedSymbols(uniqueSymbols);

        return financeApi.getQuotes(symbolString)
                .map(this::parseYahooQuotes)
                .map(quotes -> mapSymbolsToQuotes(quotes, uniqueSymbols));
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

    private List<Quote> parseYahooQuotes(String csv) {

        List<Quote> quotes = new ArrayList<>();

        String [] rows = csv.split("\n");
        for (String row : rows) {
            Quote quote = parseRow(row);
            if (quote != null) {
                quotes.add(quote);
            }
        }

        return quotes;
    }

    private Quote parseRow(String csvRow) {

        Quote quote = null;
        String strippedRow = csvRow.replace("\"", "");
        String [] cols = strippedRow.split(",");

        int pos = 0;
        String symbol = cols[pos++];
        String exchange = cols[pos++];

        if (!"N/A".equals(exchange)) {
            Money price = new Money(cols[pos++]);
            Money previousClose = new Money(cols[pos++]);
            Date lastTradeTime = getDateFromYahoo(cols[pos++], cols[pos++]);

            String dividendPerShare = cols[pos++];
            if (!isDouble(dividendPerShare)) {
                dividendPerShare = "0";
            }
            Money div = new Money(dividendPerShare);

            // name is placed at the end to handle
            // company names that contain commas
            StringBuilder name = new StringBuilder(cols[pos++]);
            while (pos < cols.length) {
                name.append(cols[pos++]);
            }

            quote = new Quote(symbol, name.toString(), exchange, price, lastTradeTime, previousClose, div);
        }

        return quote;
    }

    private boolean isDouble(String val) {
        boolean isNumber = false;

        try {
            Double.parseDouble(val);
            isNumber = true;
        } catch (Exception ex) {
            // no -op
        }

        return isNumber;

    }

    private Date getDateFromYahoo(String dateStr, String timeStr) {

        try {
            return DateTime.parse(dateStr +" " + timeStr, YAHOO_DATE_FORMAT).toDate();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date:" + dateStr, e);
            return new Date();
        }
    }

    private Map<String, Quote>  mapSymbolsToQuotes(List<Quote> quotes, Set<String> uniqueSymbols) {
        Map<String, Quote> quoteMap = new HashMap<>(uniqueSymbols.size());
        for (Quote quote : quotes) {
            quoteMap.put(quote.getSymbol(), quote);
        }
        return quoteMap;
    }
}

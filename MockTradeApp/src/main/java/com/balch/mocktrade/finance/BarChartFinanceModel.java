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

import com.balch.mocktrade.R;
import com.balch.mocktrade.settings.Settings;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Single;

public class BarChartFinanceModel implements FinanceModel {
    private static final String TAG = BarChartFinanceModel.class.getSimpleName();

    private final BarChartFinanceApi financeApi;
    private final FinanceManager mFinanceManager;
    private final String apiKey;

    public BarChartFinanceModel(Context context, BarChartFinanceApi financeApi,
                                Settings settings) {
        this.financeApi = financeApi;
        this.mFinanceManager = new FinanceManager(context.getApplicationContext(), settings);
        this.apiKey = context.getResources().getString(R.string.barchart_app_id);
    }

    @Override
    public Single<Quote> getQuote(final String symbol) {
        return getQuotes(Collections.singletonList(symbol))
                .map(quoteMap -> quoteMap.get(symbol));
    }

    @Override
    public Single<Map<String, Quote>> getQuotes(final List<String> symbols) {

        final Set<String> uniqueSymbols = getUniqueSymbols(symbols);
        String symbolString = getDelimitedSymbols(uniqueSymbols);

        return financeApi.getQuotes(symbolString, apiKey)
                .map(quoteResult -> {
                    if (quoteResult.isSuccess()) {
                        return quoteResult.getQuotes();
                    } else {
                        throw new Exception(quoteResult.getErrorMessage());
                    }
                })
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

    private Map<String, Quote>  mapSymbolsToQuotes(List<Quote> quotes, Set<String> uniqueSymbols) {
        Map<String, Quote> quoteMap = new HashMap<>(uniqueSymbols.size());
        for (Quote quote : quotes) {
            quoteMap.put(quote.getSymbol(), quote);
        }
        return quoteMap;
    }
}

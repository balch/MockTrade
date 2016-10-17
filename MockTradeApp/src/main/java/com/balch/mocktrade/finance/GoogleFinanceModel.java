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

import com.android.volley.Request;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.balch.mocktrade.ModelProvider;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class GoogleFinanceModel implements FinanceModel {
    private static final String TAG = GoogleFinanceModel.class.getSimpleName();

    private final static String GOOGLE_BASE_URL = "http://www.google.com/finance/info?infotype=infoquoteall&q=";

    private final ModelProvider mModelProvider;
    private final FinanceManager mFinanceManager;

    public GoogleFinanceModel(ModelProvider mModelProvider) {
        this.mModelProvider = mModelProvider;
        this.mFinanceManager = new FinanceManager(mModelProvider.getContext(), mModelProvider.getSettings());
    }

    private String getGoogleQueryUrl(String symbols) throws UnsupportedEncodingException  {
        return GOOGLE_BASE_URL +  URLEncoder.encode(symbols, "UTF-8");
    }

    @Override
    public Quote getQuote(final String symbol) {
        Map<String, Quote> quoteMap = getQuotes(Collections.singletonList(symbol));
        return (quoteMap != null) ? quoteMap.get(symbol) : null;
    }

    @Override
    public Map<String, Quote> getQuotes(final List<String> symbols) {

        Set<String> uniqueSymbols = getUniqueSymbols(symbols);
        String symbolString = getDelimitedSymbols(uniqueSymbols);

        Map<String, Quote> quoteMap = null;
        try {
            final String url = getGoogleQueryUrl(symbolString);

            RequestFuture<String> future = RequestFuture.newFuture();
            mModelProvider.addRequest(new StringRequest(Request.Method.GET, url, future, future));

            String response = future.get();

            response = response.trim();
            if (response.startsWith("//")) {
                response = response.substring(2);
            }

            JSONArray jsonQuotes = new JSONArray(response);
            if (jsonQuotes.length() == uniqueSymbols.size()) {
                quoteMap = new HashMap<>(uniqueSymbols.size());
                Iterator<String> symbolIterator = uniqueSymbols.iterator();
                for (int x = 0; x < jsonQuotes.length(); x++) {
                    try {
                        Quote quote = GoogleQuote.fromJSONObject(jsonQuotes.getJSONObject(x));

                        // fix issue when returned symbol does not match, check LMT.WD
                        quote.setSymbol(symbolIterator.next());
                        quoteMap.put(quote.getSymbol(), quote);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            } else {
                Log.e(TAG, "Wrong number of quotes returned");
            }
        } catch (JSONException | InterruptedException |
                ExecutionException | UnsupportedEncodingException ex) {
            Log.e(TAG, "GoogleFinanceModel.getQuotes exception", ex);
            quoteMap = null;
        }

        return quoteMap;
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

}

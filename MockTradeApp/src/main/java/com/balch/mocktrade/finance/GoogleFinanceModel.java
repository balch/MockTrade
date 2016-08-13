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
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.balch.android.app.framework.model.RequestListener;
import com.balch.mocktrade.ModelProvider;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GoogleFinanceModel implements FinanceModel {
    private static final String TAG = GoogleFinanceModel.class.getSimpleName();

    private final static String GOOGLE_BASE_URL = "http://www.google.com/finance/info?infotype=infoquoteall&q=";

    private final ModelProvider mModelProvider;
    private final FinanceManager mFinanceManager;

    public GoogleFinanceModel(ModelProvider mModelProvider) {
        this.mModelProvider = mModelProvider;
        this.mFinanceManager = new FinanceManager(mModelProvider.getContext(), mModelProvider.getSettings());
    }

    protected String getGoogleQueryUrl(String symbols) throws UnsupportedEncodingException  {
        return GOOGLE_BASE_URL +  URLEncoder.encode(symbols, "UTF-8");
    }

    @Override
    public void getQuote(final String symbol, final RequestListener<Quote> listener) {
        this.getQuotes(Collections.singletonList(symbol), new RequestListener<Map<String, Quote>>() {
            @Override
            public void onResponse(Map<String, Quote> response) {
                listener.onResponse(response.get(symbol));
            }

            @Override
            public void onErrorResponse(String error) {
                listener.onErrorResponse(error);
            }
        });
    }

    @Override
    public void getQuotes(final List<String> symbols, final RequestListener<Map<String, Quote>> listener)  {

        final Map<String, Quote> googleQuotes = new HashMap<>();

        this.getGoogleRealTimeQuotes(symbols, new RequestListener<Map<String, Quote>>() {
            @Override
            public void onResponse(Map<String, Quote> response) {
                listener.onResponse(response);
            }

            @Override
            public void onErrorResponse(String error) {
                listener.onErrorResponse(error);
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

    protected String getDelimitedSymbols(List<String> symbols) {
        StringBuilder builder = new StringBuilder();
        Set<String> symbolSet = new HashSet<>();
        boolean isFirst = true;
        for (String s : symbols) {
            if (symbolSet.contains(s)) {
                continue;
            }

            symbolSet.add(s);
            if (!isFirst) {
                builder.append(",");
            }
            builder.append(s.toUpperCase());

            isFirst = false;
        }
        return builder.toString();
    }

    protected void getGoogleRealTimeQuotes(final List<String> symbols, final RequestListener<Map<String, Quote>> listener) {
        String symbolString = this.getDelimitedSymbols(symbols);

        try {
            final String url = this.getGoogleQueryUrl(symbolString);

            StringRequest request = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                response = response.trim();
                                if (response.startsWith("//")) {
                                    response = response.substring(2);
                                }

                                Map<String, Quote> quoteMap = new HashMap<>(symbols.size());
                                JSONArray jsonQuotes = new JSONArray(response);
                                for (int x = 0; x < jsonQuotes.length(); x++) {
                                    try {
                                        Quote quote = GoogleQuote.fromJSONObject(jsonQuotes.getJSONObject(x));
                                        quoteMap.put(quote.getSymbol(), quote);
                                    } catch (Exception e) {
                                        Log.e(TAG, e.getMessage(), e);
                                    }
                                }
                                listener.onResponse(quoteMap);
                            } catch (Exception ex) {
                                Log.e(TAG, ex.getMessage(), ex);
                                listener.onErrorResponse(ex.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, error.getMessage(), error);
                            listener.onErrorResponse(error.getMessage());
                        }
                    }
            );

            mModelProvider.addRequest(request);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}

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

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.balch.android.app.framework.model.RequestListener;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.YQLModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

// https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(%22YHOO%22%2C%22AAPL%22%2C%22GOOG%22%2C%22MSFT%22)&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys

public class FinanceYQLModel extends YQLModel implements FinanceModel {
    static private final String TAG = FinanceYQLModel.class.getSimpleName();

    static private final String JSON_QUERY = "query";
    static private final String JSON_COUNT = "count";
    static private final String JSON_RESULTS = "results";
    static private final String JSON_QUOTE = "quote";

    protected FinanceManager financeManager;

    public FinanceYQLModel() {
    }

    @Override
    public void initialize(ModelProvider modelProvider) {
        super.initialize(modelProvider);
        this.financeManager = new FinanceManager(this.getContext(), this.geSettings());
    }

    @Override
    public void getQuote(final String symbol, final RequestListener<Quote> listener) {
        this.getQuotes(Arrays.asList(symbol), new RequestListener<Map<String, Quote>>() {
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

        final CountDownLatch latch = new CountDownLatch(2);
        final Map<String, Quote> yqlQuotes = new HashMap<String, Quote>();
        final Map<String, Quote> googleQuotes = new HashMap<String, Quote>();
        final StringBuilder errorMessages = new StringBuilder();

        this.getYQLQuotes(symbols, new RequestListener<Map<String, Quote>>() {
            @Override
            public void onResponse(Map<String, Quote> response) {
                try {
                    yqlQuotes.putAll(response);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onErrorResponse(String error) {
                errorMessages.append(error).append("\n");
                latch.countDown();
            }
        });

        this.getGoogleRealTimeQuotes(symbols, new RequestListener<Map<String, Quote>>() {
            @Override
            public void onResponse(Map<String, Quote> response) {
                try {
                    googleQuotes.putAll(response);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onErrorResponse(String error) {
                errorMessages.append(error).append("\n");
                latch.countDown();
            }
        });

        try {
            // wait for both requests to finish
            latch.await();
            if (errorMessages.length() == 0) {

                // add google's realtime quotes on top of yql quotes
                for (Quote q : googleQuotes.values()) {
                    Quote yqlQuote = yqlQuotes.get(q.getSymbol());
                    if (yqlQuote != null) {
                        yqlQuote.setPrice(q.getPrice());
                        yqlQuote.setLastTradeTime(q.getLastTradeTime());
                    } else {
                        Log.wtf(TAG, "GoogleQuote contains a symbol that is not in the yqlQuote map.  GoogleQuote Symbol:"+q.getSymbol()+" Submitted Symbols:"+ TextUtils.join(",", symbols));
                    }
                }
                listener.onResponse(yqlQuotes);
            } else {
                listener.onErrorResponse(errorMessages.toString());
            }
        } catch (InterruptedException e) {
        }
    }


    @Override
    public boolean isMarketOpen() {
        return this.financeManager.isMarketOpen();
    }

    @Override
    public Date nextMarketOpen() {
        return this.financeManager.nextMarketOpen();
    }

    @Override
    public boolean isInPollTime() {
        return this.financeManager.isInPollTime();
    }

    @Override
    public void setQuoteServiceAlarm() {
        this.financeManager.setQuoteServiceAlarm();
    }

    protected String getDelimitedSymbols(List<String> symbols) {
        StringBuilder builder = new StringBuilder();
        Set<String> symbolSet = new HashSet<String>();
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

                                Map<String, Quote> quoteMap = new HashMap<String, Quote>(symbols.size());
                                JSONArray jsonQuotes = new JSONArray(response);
                                for (int x = 0; x < jsonQuotes.length(); x++) {
                                    try {
                                        Quote quote = QuoteGoogleFinance.fromJSONObject(jsonQuotes.getJSONObject(x));
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

            this.getRequestQueue().add(request);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void getYQLQuotes(final List<String> symbols, final RequestListener<Map<String, Quote>> listener) {

        String symbolString = this.getDelimitedSymbols(symbols);
        StringBuilder sql = new StringBuilder("select * from yahoo.finance.quotes where symbol in (\"");
        sql.append(symbolString.replace(",","\",\"")).append("\")");

        try {
            final String request = this.getYQLQueryUrl(sql.toString());
            JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, request, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Map<String, Quote> quoteMap = new HashMap<String, Quote>(symbols.size());

                                JSONObject root = response.getJSONObject(JSON_QUERY);
                                int count = root.getInt(JSON_COUNT);
                                if (count == 1) {
                                    JSONObject jsonQuote = root.getJSONObject(JSON_RESULTS).getJSONObject(JSON_QUOTE);
                                    Quote quote = QuoteYahooFinance.fromJSONObject(jsonQuote);
                                    quoteMap.put(quote.getSymbol(), quote);
                                } else if (count > 1) {
                                    JSONArray jsonQuotes = root.getJSONObject(JSON_RESULTS).getJSONArray(JSON_QUOTE);
                                    for (int x = 0; x < jsonQuotes.length(); x++) {
                                        try {
                                            Quote quote = QuoteYahooFinance.fromJSONObject(jsonQuotes.getJSONObject(x));
                                            quoteMap.put(quote.getSymbol(), quote);
                                        } catch (Exception e) {
                                            Log.e(TAG, e.getMessage(), e);
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "No results return: "+ request);
                                    listener.onErrorResponse("Not Found");
                                    return;
                                }

                                listener.onResponse(quoteMap);
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage(), e);
                                listener.onErrorResponse(e.getMessage());
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, error.getMessage(), error);
                    listener.onErrorResponse(error.getMessage());
                }
            });

            this.getRequestQueue().add(jsObjRequest);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


}

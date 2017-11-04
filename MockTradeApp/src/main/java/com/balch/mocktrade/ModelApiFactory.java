/*
 * Author: Balch
 * Created: 5/21/17 6:39 AM
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

package com.balch.mocktrade;

import com.balch.mocktrade.finance.IEXFinanceApi;
import com.balch.mocktrade.finance.IEXQuoteTypeAdapter;
import com.balch.mocktrade.finance.QuoteResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ModelApiFactory {

    private final static String IEX_FINANCE_BASE_URL = "https://api.iextrading.com/1.0/";

    private IEXFinanceApi IEXFinanceApi = null;

    private final static Gson gson = new GsonBuilder()
            .registerTypeAdapter(QuoteResult.class, new IEXQuoteTypeAdapter())
            .create();

    @SuppressWarnings("unchecked")
    public <T> T getModelApi(Class<T> api) {
        if (api == IEXFinanceApi.class) {
            if (IEXFinanceApi == null) {
                IEXFinanceApi = getRetrofitService(IEX_FINANCE_BASE_URL)
                        .create(IEXFinanceApi.class);
            }
            return (T) IEXFinanceApi;
        }

        return null;
    }

    private static Retrofit getRetrofitService(String baseUrl) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build();
    }


}

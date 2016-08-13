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

package com.balch.mocktrade.model;


import android.content.Context;

import com.android.volley.Request;
import com.balch.android.app.framework.model.ModelInitializer;
import com.balch.mocktrade.settings.Settings;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class GoogleFinanceModel implements ModelInitializer<ModelProvider> {
    protected final static String GOOGLE_BASE_URL = "http://www.google.com/finance/info?infotype=infoquoteall&q=";

    protected ModelProvider mModelProvider;

    public GoogleFinanceModel() {
    }

    protected GoogleFinanceModel(ModelProvider modelProvider) {
        initialize(modelProvider);
    }

    @Override
    public void initialize(ModelProvider modelProvider) {
        this.mModelProvider = modelProvider;
    }

    protected String getGoogleQueryUrl(String symbols) throws UnsupportedEncodingException  {
        return GOOGLE_BASE_URL +  URLEncoder.encode(symbols, "UTF-8");
    }

    public <T> Request<T> addRequest(Request<T> request) {
        return mModelProvider.addRequest(request);
    }

    public <T> Request<T> addRequest(Request<T> request, boolean customRetryPolicy) {
        return mModelProvider.addRequest(request, customRetryPolicy);
    }

    public Context getContext() {
        return mModelProvider.getContext();
    }

    public Settings geSettings() {
        return mModelProvider.getSettings();
    }

}

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

package com.balch.mocktrade;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.android.volley.RequestQueue;
import com.balch.android.app.framework.model.ModelFactory;
import com.balch.android.app.framework.sql.SqlConnection;
import com.balch.android.app.framework.view.VolleyBackground;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.FinanceYQLModel;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.SqliteSourceProvider;
import com.balch.mocktrade.model.YQLSourceProvider;
import com.balch.mocktrade.order.OrderModel;
import com.balch.mocktrade.order.OrderSqliteModel;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioSqliteModel;
import com.balch.mocktrade.settings.Settings;

public class TradeApplication extends Application implements ModelProvider {
    private static final String TAG = TradeApplication.class.getSimpleName();

    private static final String DATABASE_NAME = "mocktrade.db";
    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_CREATES_SCRIPT = "sql/create.sql";
    private static final String DATABASE_UPDATE_SCRIPT_FORMAT = "sql/upgrade_%d.sql";

    private SqlConnection mSqlConnection;
    private RequestQueue mRequestQueue;
    private Settings mSettings;

    private SqliteSourceProvider mSqliteScheme;
    private YQLSourceProvider mYqlScheme;
    private ModelFactory mModelFactory;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        mSettings = new Settings(this);

        mSqlConnection = new SqlConnection(this, DATABASE_NAME, DATABASE_VERSION,
                DATABASE_CREATES_SCRIPT, DATABASE_UPDATE_SCRIPT_FORMAT);

        mRequestQueue = VolleyBackground.newRequestQueue(this, 10);

        configureModelFactory();

        FinanceModel financeModel = getModelFactory().getModel(FinanceModel.class);
        financeModel.setQuoteServiceAlarm();

    }

    @Override
    public ModelFactory getModelFactory() {
        return mModelFactory;
    }

    @Override
    public Context getContext() {
        return this;
    }

    public SqliteSourceProvider getSqliteScheme() {
        return mSqliteScheme;
    }

    public YQLSourceProvider getYqlScheme() {
        return mYqlScheme;
    }

    private void configureModelFactory() {
        mModelFactory = new ModelFactory();

        mSqliteScheme = new SqliteSourceProvider(this);
        mYqlScheme = new YQLSourceProvider(this);

        mModelFactory.registerModel(PortfolioModel.class, PortfolioSqliteModel.class, mSqliteScheme, true);
        mModelFactory.registerModel(OrderModel.class, OrderSqliteModel.class, mSqliteScheme, true);
        mModelFactory.registerModel(FinanceModel.class, FinanceYQLModel.class, mYqlScheme, true);
    }

    @Override
    public SqlConnection getSqlConnection() {
        return mSqlConnection;
    }

    @Override
    public Settings getSettings() {
        return mSettings;
    }

    @Override
    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }
}

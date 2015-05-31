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
import android.app.FragmentManager;
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
    private static final String TAG = TradeApplication.class.getName();

    private static final String DATABASE_NAME = "mocktrade.db";
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_CREATES_SCRIPT = "sql/create.sql";
    private static final String DATABASE_UPDATE_SCRIPT_FORMAT = "sql/upgrade_%d.sql";

    private SqlConnection sqlConnection;
    private RequestQueue requestQueue;
    private Settings settings;

    private SqliteSourceProvider sqliteScheme;
    private YQLSourceProvider yqlScheme;
    private ModelFactory modelFactory;


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

        this.settings = new Settings(this);

        this.sqlConnection = new SqlConnection(this, DATABASE_NAME, DATABASE_VERSION,
                DATABASE_CREATES_SCRIPT, DATABASE_UPDATE_SCRIPT_FORMAT);

        this.requestQueue = VolleyBackground.newRequestQueue(this, 10);

        this.configureModelFactory();

        FinanceModel financeModel = getModelFactory().getModel(FinanceModel.class);
        financeModel.setQuoteServiceAlarm();

    }

    @Override
    public ModelFactory getModelFactory() {
        return modelFactory;
    }

    @Override
    public Context getContext() {
        return this;
    }

    public SqliteSourceProvider getSqliteScheme() {
        return sqliteScheme;
    }

    public YQLSourceProvider getYqlScheme() {
        return yqlScheme;
    }

    private void configureModelFactory() {
        this.modelFactory = new ModelFactory();

        this.sqliteScheme = new SqliteSourceProvider(this);
        this.yqlScheme = new YQLSourceProvider(this);

        this.modelFactory.registerModel(PortfolioModel.class, PortfolioSqliteModel.class, this.sqliteScheme, true);
        this.modelFactory.registerModel(OrderModel.class, OrderSqliteModel.class, this.sqliteScheme, true);
        this.modelFactory.registerModel(FinanceModel.class, FinanceYQLModel.class, this.yqlScheme, true);
    }

    @Override
    public SqlConnection getSqlConnection() {
        return sqlConnection;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }


    public void closeCurrentView(FragmentManager fragmentManager) {
        fragmentManager.popBackStack();
    }

    @Override
    public RequestQueue getRequestQueue() {
        return this.requestQueue;
    }
}

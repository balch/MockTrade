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
import android.content.res.Configuration;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import com.balch.android.app.framework.sql.SqlConnection;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.FinanceModelImpl;
import com.balch.mocktrade.finance.IEXFinanceApi;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioSqliteModel;
import com.balch.mocktrade.services.WearSyncService;
import com.balch.mocktrade.settings.Settings;

import net.danlew.android.joda.JodaTimeAndroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

public class TradeApplication extends Application implements TradeModelProvider, ViewProvider {
    private static final String TAG = TradeApplication.class.getSimpleName();

    public static final String DATABASE_NAME = "mocktrade.db";
    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_CREATES_SCRIPT = "sql/create.sql";
    private static final String DATABASE_UPDATE_SCRIPT_FORMAT = "sql/upgrade_%d.sql";

    private static final String DAILY_BACKUP_DATABASE_NAME = "daily_backup";

    private volatile SqlConnection sqlConnection;
    private volatile Settings settings;
    private final ModelApiFactory modelApiFactory = new ModelApiFactory();

    @Override
    public void onCreate() {
        super.onCreate();

        JodaTimeAndroid.init(this);

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
                    .build());
        }

        startService(WearSyncService.getIntent(this, true, true, true, false));

        Completable.fromAction(this::startAlarms)
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    protected void startAlarms() {
        FinanceModel financeModel = getFinanceModel();
        financeModel.setQuoteServiceAlarm();

        PortfolioModel portfolioModel = new PortfolioSqliteModel(this,
                getSqlConnection(), financeModel, getSettings());
        portfolioModel.scheduleOrderServiceAlarmIfNeeded();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public SqlConnection getSqlConnection() {

        // double check lock pattern
        if (sqlConnection == null) {
            synchronized (this) {
                if (sqlConnection == null) {
                    sqlConnection = new SqlConnection(this, DATABASE_NAME, DATABASE_VERSION,
                            DATABASE_CREATES_SCRIPT, DATABASE_UPDATE_SCRIPT_FORMAT);
                }
            }
        }

        return sqlConnection;
    }

    @Override
    public Settings getSettings() {
        // double check lock pattern
        if (settings == null) {
            synchronized (this) {
                if (settings == null) {
                    settings = new Settings(this);
                }
            }
        }

        return settings;
    }

    @Override
    public boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    @Override
    public boolean isLandscape(Context context) {
        return (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    public static boolean backupDatabase(Context context, boolean isDaily) {
        boolean success = false;

        String backupDBPathPrefix;

        if (isDaily) {
            backupDBPathPrefix = DAILY_BACKUP_DATABASE_NAME;
            if (BuildConfig.DEBUG) {
                backupDBPathPrefix += "_debug";
            }
        } else {
            backupDBPathPrefix = String.valueOf(System.currentTimeMillis());
        }

        String backupDBPath = backupDBPathPrefix + "_" + DATABASE_NAME;

        FileChannel src = null;
        FileChannel dst = null;
        try {
            File sd = Environment.getExternalStorageDirectory();

            if (sd.canWrite()) {
                File dbFile = context.getDatabasePath(TradeApplication.DATABASE_NAME);

                File backupDBFile = new File(sd, backupDBPath);

                if (dbFile.exists()) {
                    src = new FileInputStream(dbFile).getChannel();
                    dst = new FileOutputStream(backupDBFile).getChannel();
                    dst.transferFrom(src, 0, src.size());

                    success = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error backing up to Database", e);
        } finally {
            if (src != null) {
                try {
                    src.close();
                } catch (IOException ignored) {
                }
            }

            if (dst != null)
                try {
                    dst.close();
                } catch (IOException ignored) {
                }
        }
        return success;
    }

    public static boolean restoreDatabase(Context context) {
        boolean success = false;

        FileChannel src = null;
        FileChannel dst = null;
        try {
            File sd = Environment.getExternalStorageDirectory();

            if (sd.canWrite()) {
                File dbFile = context.getDatabasePath(TradeApplication.DATABASE_NAME);

                File[] backups = sd.listFiles((dir, name) -> name.startsWith("1") &&
                        name.endsWith("_" + TradeApplication.DATABASE_NAME));

                if (backups != null && backups.length > 1) {
                    Arrays.sort(backups, (object1, object2) ->
                            object1.getName().compareTo(object2.getName()));
                }

                if (backups != null) {
                    src = new FileInputStream(backups[backups.length - 1]).getChannel();
                    dst = new FileOutputStream(dbFile).getChannel();
                    dst.transferFrom(src, 0, src.size());

                    success = true;

                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring Database", e);
        } finally {
            if (src != null) {
                try {
                    src.close();
                } catch (IOException ignored) {
                }
            }

            if (dst != null)
                try {
                    dst.close();
                } catch (IOException ignored) {
                }
        }

        return success;
    }

    @Override
    public ModelApiFactory getModelApiFactory() {
        return modelApiFactory;
    }

    @Override
    public FinanceModel getFinanceModel() {
        return new FinanceModelImpl(this,
                modelApiFactory.getModelApi(IEXFinanceApi.class), getSettings());
    }

}

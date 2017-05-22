/*
 * Author: Balch
 * Created: 7/28/16 11:25 PM
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
 * Copyright (C) 2016
 *
 */

package com.balch.mocktrade.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.TradeModelProvider;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.finance.GoogleFinanceApi;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioSqliteModel;
import com.balch.mocktrade.settings.Settings;
import com.balch.mocktrade.shared.HighlightItem;
import com.balch.mocktrade.shared.PerformanceItem;
import com.balch.mocktrade.shared.WatchConfigItem;
import com.balch.mocktrade.shared.WearDataSync;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WearSyncService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = WearSyncService.class.getSimpleName();

    private static final String EXTRA_SEND_PERF_ITEMS = "extra_send_perf_items";
    private static final String EXTRA_SEND_CONFIG_ITEMS = "extra_send_config_items";
    private static final String EXTRA_SEND_HIGHLIGHTS = "extra_send_highlights";
    private static final String EXTRA_BROADCAST_ACCOUNT_ID = "extra_broadcast_account_id";

    private static final long CONNECTION_TIME_OUT_MS = 1000;
    private GoogleApiClient mGoogleApiClient;

    public static Intent getIntent(Context context, boolean sendPerformanceItems, boolean sendHighlights,
                                   boolean sendConfigItems, boolean broadcastAccountId) {
        Intent intent = new Intent(context, WearSyncService.class);
        intent.putExtra(EXTRA_SEND_PERF_ITEMS, sendPerformanceItems);
        intent.putExtra(EXTRA_SEND_HIGHLIGHTS, sendHighlights);
        intent.putExtra(EXTRA_SEND_CONFIG_ITEMS, sendConfigItems);
        intent.putExtra(EXTRA_BROADCAST_ACCOUNT_ID, broadcastAccountId);
        return intent;
    }

    public static Intent getIntent(Context context) {
        return getIntent(context, true, true, false, false);
    }

    public WearSyncService() {
        super(WearSyncService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        if (mGoogleApiClient.isConnected()) {

            try {

                boolean sendPerformanceItems = intent.getBooleanExtra(EXTRA_SEND_PERF_ITEMS, true);
                boolean sendHighlights = intent.getBooleanExtra(EXTRA_SEND_HIGHLIGHTS, true);
                boolean sendConfigItems = intent.getBooleanExtra(EXTRA_SEND_CONFIG_ITEMS, false);
                boolean broadcastAccountID = intent.getBooleanExtra(EXTRA_BROADCAST_ACCOUNT_ID, false);
                Log.i(TAG, "WearSyncService onHandleIntent");

                TradeModelProvider modelProvider = (TradeModelProvider) this.getApplication();
                PortfolioModel portfolioModel = new PortfolioSqliteModel(modelProvider.getContext(),
                        modelProvider.getSqlConnection(),
                        modelProvider.getModelApiFactory().getModelApi(GoogleFinanceApi.class),
                        modelProvider.getSettings());

                DataItemBuffer dataItems = Wearable.DataApi.getDataItems(mGoogleApiClient).await();
                long accountId = getAccountIdFromDataBuffer(dataItems);

                dataItems.release();
                List<PerformanceItem> performanceItems = portfolioModel.getCurrentSnapshot(accountId);

                if (broadcastAccountID) {
                    PerformanceItemUpdateBroadcaster.broadcast(getApplicationContext(), accountId, -1);
                }

                if (sendConfigItems) {
                    sendConfigItems(modelProvider.getSettings());
                }

                if (sendPerformanceItems && (performanceItems != null)) {
                    sendPerformanceItems(performanceItems);
                }

                if (sendHighlights) {
                    sendHighlights(modelProvider, portfolioModel);
                }

            } catch (Exception ex) {
                Log.e(TAG, "onHandleIntent exception", ex);
            } finally {
                mGoogleApiClient.disconnect();
            }
        } else {
            Log.e(TAG, "Failed to connect to GoogleApiClient");
        }

    }

    private void sendConfigItems(Settings settings) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearDataSync.PATH_WATCH_CONFIG_SYNC);
        putDataMapRequest.getDataMap().putDataMapArrayList(WearDataSync.DATA_WATCH_CONFIG_DATA_ITEMS, getConfigDataMap(settings));
        putDataMapRequest.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest());
        pendingResult.await();
    }

    private void sendPerformanceItems(List<PerformanceItem> performanceItems) {
        ArrayList<DataMap> dataMapList = new ArrayList<>(performanceItems.size());
        for (PerformanceItem item : performanceItems) {
            dataMapList.add(item.toDataMap());
        }

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearDataSync.PATH_SNAPSHOT_SYNC);
        putDataMapRequest.getDataMap().putDataMapArrayList(WearDataSync.DATA_SNAPSHOT_DAILY, dataMapList);
        putDataMapRequest.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest());
        pendingResult.await();
    }

    private void sendHighlights(TradeModelProvider modelProvider, PortfolioModel portfolioModel) {
        Settings settings = modelProvider.getSettings();
        boolean allAccounts = !settings.getBoolean(Settings.Key.PREF_HIDE_EXCLUDE_ACCOUNTS);

        List<Account> accounts = portfolioModel.getAccounts(allAccounts);
        if ((accounts != null) && (accounts.size() > 0)) {

            PerformanceItem totalsPerformanceItem = new PerformanceItem(-1, new Date(),
                    new Money(), new Money(), new Money());

            List<DataMap> accountsDataMapList = new ArrayList<>();

            Highlights highlights = getHighlights(portfolioModel, settings,
                    accounts, allAccounts, totalsPerformanceItem, accountsDataMapList);

            ArrayList<DataMap> dataMapList = getHighlightDataMap(highlights,
                    totalsPerformanceItem, accountsDataMapList);
            publishHighlights(dataMapList);
        }
    }

    private Highlights getHighlights(PortfolioModel portfolioModel,
                               Settings settings,
                               List<Account> accounts, boolean allAccounts,
                               PerformanceItem totalsPerformanceItem,
                               List<DataMap> accountsDataMapList) {

        Resources resources = getResources();

        Highlights highlights = new Highlights();
        boolean demoMode = settings.getBoolean(Settings.Key.PREF_DEMO_MODE);

        Date timestamp = new Date();
        for (Account account : accounts) {
            PerformanceItem performanceItem = new PerformanceItem(-1, new Date(), new Money(), new Money(), new Money());
            List<Investment> investments = portfolioModel.getInvestments(account.getId());

            if (allAccounts || !account.getExcludeFromTotals()) {
                calculateHighlights(highlights, investments);
            }
            performanceItem.aggregate(account.getPerformanceItem(investments, timestamp));

            HighlightItem item = new HighlightItem(HighlightItem.HighlightType.TOTAL_ACCOUNT,
                    resources.getString(R.string.highlight_total_account), account.getName(),
                    performanceItem.getCostBasis(), performanceItem.getValue(),
                    performanceItem.getTodayChange(), performanceItem.getTotalChangePercent(),
                    account.getId());
            accountsDataMapList.add(item.toDataMap());

            if (demoMode || !account.getExcludeFromTotals()) {
                totalsPerformanceItem.aggregate(account.getPerformanceItem(investments, timestamp));
            }
        }

        return highlights;
    }

    private void calculateHighlights(Highlights highlights, List<Investment> investments) {
        for (Investment investment : investments) {
            if (highlights.bestDayPerformer != null) {
                if (investment.getTodayChangePercent() > highlights.bestDayPerformer.getTodayChangePercent()) {
                    highlights.bestDayPerformer = investment;
                }
            } else {
                highlights.bestDayPerformer = investment;
            }

            if (highlights.worstDayPerformer != null) {
                if (investment.getTodayChangePercent() < highlights.worstDayPerformer.getTodayChangePercent()) {
                    highlights.worstDayPerformer = investment;
                }
            } else {
                highlights.worstDayPerformer = investment;
            }

            if (highlights.bestTotalPerformer != null) {
                if (investment.getTotalChangePercent() > highlights.bestTotalPerformer.getTotalChangePercent()) {
                    highlights.bestTotalPerformer = investment;
                }
            } else {
                highlights.bestTotalPerformer = investment;
            }

            if (highlights.worstTotalPerformer != null) {
                if (investment.getTotalChangePercent() < highlights.worstTotalPerformer.getTotalChangePercent()) {
                    highlights.worstTotalPerformer = investment;
                }
            } else {
                highlights.worstTotalPerformer = investment;
            }

        }
    }

    private ArrayList<DataMap> getHighlightDataMap(Highlights highlights,
                                              PerformanceItem totalsPerformanceItem,
                                              List<DataMap> accountsDataMapList) {
        Resources resources = getResources();

        ArrayList<DataMap> dataMapList = new ArrayList<>();
        HighlightItem item = new HighlightItem(HighlightItem.HighlightType.TOTAL_OVERALL,
                resources.getString(R.string.highlight_total_overall), "",
                totalsPerformanceItem.getCostBasis(), totalsPerformanceItem.getValue(),
                totalsPerformanceItem.getTodayChange(), -1, -1);
        dataMapList.add(item.toDataMap());
        dataMapList.addAll(accountsDataMapList);

        if (highlights.bestTotalPerformer != null) {
            dataMapList.add(getDataMapFromInvestment(HighlightItem.HighlightType.PERFORMER_BEST_TOTAL,
                    resources.getString(R.string.highlight_best_total), highlights.bestTotalPerformer));
        }

        if (highlights.bestDayPerformer != null) {
            dataMapList.add(getDataMapFromInvestment(HighlightItem.HighlightType.PERFORMER_BEST_DAY,
                    resources.getString(R.string.highlight_best_day), highlights.bestDayPerformer));
        }

        if (highlights.worstTotalPerformer != null) {
            dataMapList.add(getDataMapFromInvestment(HighlightItem.HighlightType.PERFORMER_WORST_TOTAL,
                    resources.getString(R.string.highlight_worst_total), highlights.worstTotalPerformer));
        }

        if (highlights.worstDayPerformer != null) {
            dataMapList.add(getDataMapFromInvestment(HighlightItem.HighlightType.PERFORMER_WORST_DAY,
                    resources.getString(R.string.highlight_worst_day), highlights.worstDayPerformer));
        }

        return dataMapList;
    }

    private void publishHighlights(ArrayList<DataMap> dataMapList ) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearDataSync.PATH_HIGHLIGHTS_SYNC);
        putDataMapRequest.setUrgent();
        putDataMapRequest.getDataMap().putDataMapArrayList(WearDataSync.DATA_HIGHLIGHTS, dataMapList);
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest());
        pendingResult.await();
    }

    private long getAccountIdFromDataBuffer(DataItemBuffer dataItems) {
        long accountId = -1;
        for (int x = 0; x < dataItems.getCount(); x++) {
            DataItem dataItem = dataItems.get(x);

            if (dataItem.getUri().getPath().equals(WearDataSync.PATH_WATCH_FACE_ACCOUNT_ID)) {
                accountId = getAccountIdFromDataBuffer(dataItem);
            }
        }

        return accountId;
    }

    private long getAccountIdFromDataBuffer(DataItem dataItem) {
        long accountId = -1;
        if (dataItem.getUri().getPath().equals(WearDataSync.PATH_WATCH_FACE_ACCOUNT_ID)) {
            DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
            DataMap dataMap = dataMapItem.getDataMap();
            accountId = dataMap.getLong(WearDataSync.DATA_WATCH_FACE_ACCOUNT_ID, -1);
        }

        return accountId;
    }

    private DataMap getDataMapFromInvestment(HighlightItem.HighlightType highlightType,
                             String description, Investment investment) {
        long accountId = (investment.getAccount() != null) ? investment.getAccount().getId() : -1;
        HighlightItem item = new HighlightItem(highlightType, description,
                investment.getSymbol(), investment.getCostBasis(), investment.getValue(),
                investment.getTodayChange(), investment.getTodayChangePercent(), accountId);
        return item.toDataMap();
    }

    private ArrayList<DataMap> getConfigDataMap(Settings settings) {
        Resources resources = getResources();
        ArrayList<DataMap> dataMaps = new ArrayList<>();
        dataMaps.add(newConfigItem(Settings.Key.PREF_TWENTY_FOUR_HOUR_DISPLAY, resources.getString(R.string.watch_config_item_twenty_four_hour_time), settings).toDataMap());
        dataMaps.add(newConfigItem(Settings.Key.PREF_HIDE_EXCLUDE_ACCOUNTS, resources.getString(R.string.watch_config_item_hide_exclude_account), settings).toDataMap());
        dataMaps.add(newConfigItem(Settings.Key.PREF_DEMO_MODE, resources.getString(R.string.watch_config_item_obfuscate_total), settings).toDataMap());

        return dataMaps;
    }

    private WatchConfigItem newConfigItem(Settings.Key key, String description, Settings settings) {
        return new WatchConfigItem(key.key(), description, settings.getBoolean(key));
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
         Log.e(TAG, "onConnectionFailed: "+result.toString());
    }

    private static class Highlights {
        private Investment bestTotalPerformer = null;
        private Investment worstTotalPerformer = null;
        private Investment bestDayPerformer = null;
        private Investment worstDayPerformer = null;
    }
}
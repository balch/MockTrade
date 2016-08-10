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

import com.balch.android.app.framework.model.ModelFactory;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.shared.HighlightItem;
import com.balch.mocktrade.shared.PerformanceItem;
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
    private static final String EXTRA_SEND_HIGHLIGHTS = "extra_send_highlights";
    private static final String EXTRA_BROADCAST_ACCOUNT_ID = "extra_broadcast_account_id";

    private static final long CONNECTION_TIME_OUT_MS = 1000;
    private GoogleApiClient mGoogleApiClient;

    public static Intent getIntent(Context context, boolean sendPerformanceItems, boolean sendHighlights, boolean broadcastAccountId) {
        Intent intent = new Intent(context, WearSyncService.class);
        intent.putExtra(EXTRA_SEND_PERF_ITEMS, sendPerformanceItems);
        intent.putExtra(EXTRA_SEND_HIGHLIGHTS, sendHighlights);
        intent.putExtra(EXTRA_BROADCAST_ACCOUNT_ID, broadcastAccountId);
        return intent;
    }

    public static Intent getIntent(Context context) {
        return getIntent(context, true, true, false);
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
                boolean broadcastAccountID = intent.getBooleanExtra(EXTRA_BROADCAST_ACCOUNT_ID, false);
                Log.i(TAG, "WearSyncService onHandleIntent");

                ModelProvider modelProvider = (ModelProvider) this.getApplication();
                ModelFactory modelFactory = modelProvider.getModelFactory();
                PortfolioModel portfolioModel = modelFactory.getModel(PortfolioModel.class);

                DataItemBuffer dataItems = Wearable.DataApi.getDataItems(mGoogleApiClient).await();
                long accountId = getAccountIdFromDataBuffer(dataItems);

                dataItems.release();
                List<PerformanceItem> performanceItems = portfolioModel.getCurrentSnapshot(accountId);

                if (broadcastAccountID) {
                    AccountUpdateBroadcaster.broadcast(getApplicationContext(), accountId);
                }

                if (sendPerformanceItems && (performanceItems != null)) {
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

                if (sendHighlights) {
                    boolean allAccounts = !modelProvider.getSettings().getHideExcludeAccounts();
                    List<Account> accounts = portfolioModel.getAccounts(allAccounts);
                    if ((accounts != null) && (accounts.size() > 0)) {

                        PerformanceItem totalsPerformanceItem = new PerformanceItem(-1, new Date(), new Money(), new Money(), new Money());

                        ArrayList<DataMap> accountsDataMapList = new ArrayList<>();

                        Investment bestTotalPerformer = null;
                        Investment worstTotalPerformer = null;
                        Investment bestDayPerformer = null;
                        Investment worstDayPerformer = null;

                        Resources resources = getResources();
                        Date timestamp = new Date();
                        for (Account account : accounts) {
                            PerformanceItem performanceItem = new PerformanceItem(-1, new Date(), new Money(), new Money(), new Money());
                            List<Investment> investments = portfolioModel.getInvestments(account.getId());

                            if (allAccounts || !account.getExcludeFromTotals()) {
                                for (Investment investment : investments) {
                                    if (bestDayPerformer != null) {
                                        if (investment.getTodayChangePercent() > bestDayPerformer.getTodayChangePercent()) {
                                            bestDayPerformer = investment;
                                        }
                                    } else {
                                        bestDayPerformer = investment;
                                    }

                                    if (worstDayPerformer != null) {
                                        if (investment.getTodayChangePercent() < worstDayPerformer.getTodayChangePercent()) {
                                            worstDayPerformer = investment;
                                        }
                                    } else {
                                        worstDayPerformer = investment;
                                    }

                                    if (bestTotalPerformer != null) {
                                        if (investment.getTotalChangePercent() > bestTotalPerformer.getTotalChangePercent()) {
                                            bestTotalPerformer = investment;
                                        }
                                    } else {
                                        bestTotalPerformer = investment;
                                    }

                                    if (worstTotalPerformer != null) {
                                        if (investment.getTotalChangePercent() < worstTotalPerformer.getTotalChangePercent()) {
                                            worstTotalPerformer = investment;
                                        }
                                    } else {
                                        worstTotalPerformer = investment;
                                    }

                                }
                            }
                            performanceItem.aggregate(account.getPerformanceItem(investments, timestamp));

                            HighlightItem item = new HighlightItem(HighlightItem.HighlightType.TOTAL_ACCOUNT,
                                    resources.getString(R.string.highlight_total_account), account.getName(),
                                    performanceItem.getCostBasis(), performanceItem.getValue(),
                                    performanceItem.getTodayChange(), performanceItem.getTotalChangePercent(),
                                    account.getId());
                            accountsDataMapList.add(item.toDataMap());

                            if (!account.getExcludeFromTotals()) {
                                totalsPerformanceItem.aggregate(account.getPerformanceItem(investments, timestamp));
                            }
                        }

                        ArrayList<DataMap> dataMapList = new ArrayList<>();
                        HighlightItem item = new HighlightItem(HighlightItem.HighlightType.TOTAL_OVERALL,
                                resources.getString(R.string.highlight_total_overall), "",
                                totalsPerformanceItem.getCostBasis(), totalsPerformanceItem.getValue(),
                                totalsPerformanceItem.getTodayChange(), -1, -1);
                        dataMapList.add(item.toDataMap());
                        dataMapList.addAll(accountsDataMapList);

                        if (bestTotalPerformer != null) {
                            dataMapList.add(getDataMapFromInvestment(HighlightItem.HighlightType.PERFORMER_BEST_TOTAL,
                                    resources.getString(R.string.highlight_best_total), bestTotalPerformer));
                        }

                        if (bestDayPerformer != null) {
                            dataMapList.add(getDataMapFromInvestment(HighlightItem.HighlightType.PERFORMER_BEST_DAY,
                                    resources.getString(R.string.highlight_best_day), bestDayPerformer));
                        }

                        if (worstTotalPerformer != null) {
                            dataMapList.add(getDataMapFromInvestment(HighlightItem.HighlightType.PERFORMER_WORST_TOTAL,
                                    resources.getString(R.string.highlight_worst_total), worstTotalPerformer));
                        }

                        if (worstDayPerformer != null) {
                            dataMapList.add(getDataMapFromInvestment(HighlightItem.HighlightType.PERFORMER_WORST_DAY,
                                    resources.getString(R.string.highlight_worst_day), worstDayPerformer));
                        }

                        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearDataSync.PATH_HIGHLIGHTS_SYNC);
                        putDataMapRequest.setUrgent();
                        putDataMapRequest.getDataMap().putDataMapArrayList(WearDataSync.DATA_HIGHLIGHTS, dataMapList);
                        PendingResult<DataApi.DataItemResult> pendingResult =
                                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest());
                        pendingResult.await();
                    }
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

    static long getAccountIdFromDataBuffer(DataItemBuffer dataItems) {
        long accountId = -1;
        for (int x = 0; x < dataItems.getCount(); x++) {
            DataItem dataItem = dataItems.get(x);

            if (dataItem.getUri().getPath().equals(WearDataSync.PATH_WATCH_FACE_ACCOUNT_ID)) {
                accountId = getAccountIdFromDataBuffer(dataItem);
            }
        }

        return accountId;
    }

    static long getAccountIdFromDataBuffer(DataItem dataItem) {
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
}
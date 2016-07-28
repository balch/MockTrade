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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.portfolio.SnapshotTotalsSqliteModel;
import com.balch.mocktrade.shared.PerformanceItem;
import com.balch.mocktrade.shared.WearDataSync;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WearSyncService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = WearSyncService.class.getSimpleName();

    private static final long CONNECTION_TIME_OUT_MS = 1000;
    private GoogleApiClient mGoogleApiClient;

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
                Log.i(TAG, "WearSyncService onHandleIntent");

                SnapshotTotalsSqliteModel snapShotModel = new SnapshotTotalsSqliteModel(((ModelProvider) this.getApplication()));
                List<PerformanceItem> performanceItems = snapShotModel.getCurrentSnapshot();

                if (performanceItems != null) {
                    ArrayList<DataMap> dataMapList = new ArrayList<>(performanceItems.size());
                    for (PerformanceItem item : performanceItems) {
                        dataMapList.add(item.toDataMap());
                    }

                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearDataSync.PATH_SNAPSHOT_SYNC);
                    putDataMapRequest.getDataMap().putDataMapArrayList(WearDataSync.DATA_SNAPSHOT_DAILY, dataMapList);
                    putDataMapRequest.setUrgent();
                    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest());
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                            Log.d(TAG, "putDataItem: "+dataItemResult.toString());

                        }
                    });
                    pendingResult.await();
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

    public static Intent getIntent(Context context) {
        return new Intent(context, WearSyncService.class);
    }
}
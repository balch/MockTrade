/*
 * Author: Balch
 * Created: 8/9/16 7:39 PM
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

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.balch.mocktrade.TradeModelProvider;
import com.balch.mocktrade.settings.Settings;
import com.balch.mocktrade.shared.WatchConfigItem;
import com.balch.mocktrade.shared.WearDataSync;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WearSyncListener extends WearableListenerService
implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = WearSyncListener.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private TradeModelProvider modelProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "WearSyncListener: onCreate");
        modelProvider = (TradeModelProvider) this.getApplication();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        if (googleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(googleApiClient, this);
            Wearable.MessageApi.removeListener(googleApiClient, this);
            googleApiClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String uriPath = messageEvent.getPath();
        if (uriPath.equals(WearDataSync.MSG_WATCH_CONFIG_SET)) {
            byte[] rawData = messageEvent.getData();
            WatchConfigItem configItem = new WatchConfigItem(DataMap.fromByteArray(rawData));
            modelProvider.getSettings().setBoolean(Settings.Key.fromKey(configItem.getKey()), configItem.isEnabled());
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataItems) {

        Log.d(TAG, "WearSyncListener: onDataChanged");
        for  (int x = 0; x < dataItems.getCount(); x++) {
            DataEvent dataEvent = dataItems.get(x);
            if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                continue;
            }

            String uriPath = dataEvent.getDataItem().getUri().getPath();
            if (uriPath.equals(WearDataSync.PATH_WATCH_FACE_ACCOUNT_ID)) {
                startService(WearSyncService.getIntent(getApplicationContext(), true, false, false, true));
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "WearSyncListener: onConnected");
        Wearable.DataApi.addListener(googleApiClient, this);
        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "WearSyncListener: onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.toString());

    }
}




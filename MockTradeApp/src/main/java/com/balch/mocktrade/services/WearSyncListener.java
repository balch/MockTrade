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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.balch.mocktrade.shared.WearDataSync;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WearSyncListener extends WearableListenerService
implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = WearSyncListener.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        Log.d(TAG, "WearSyncListener: onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataItems) {

        Log.d(TAG, "WearSyncListener: onDataChanged");
        for  (int x = 0; x < dataItems.getCount(); x++) {
            DataItem dataItem = dataItems.get(x).getDataItem();
            String uriPath = dataItem.getUri().getPath();
            if (uriPath.equals(WearDataSync.PATH_WATCH_FACE_ACCOUNT_ID)) {
                startService(WearSyncService.getIntent(getApplicationContext(), true, false, true));
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "WearSyncListener: onConnected");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        startService(WearSyncService.getIntent(getApplicationContext(), true, false, true));
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "WearSyncListener: onConnectionSuspended");
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.toString());

    }
}




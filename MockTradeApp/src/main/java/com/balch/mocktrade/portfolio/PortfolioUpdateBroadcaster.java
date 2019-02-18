package com.balch.mocktrade.portfolio;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

public class PortfolioUpdateBroadcaster  {
    private static final String TAG = PortfolioUpdateBroadcaster.class.getSimpleName();

    public static final String ACTION = PortfolioUpdateBroadcaster.class.getName();

    static public void broadcast(Context context) {
        Log.d(TAG, "broadcast sent:" + ACTION);
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION));
    }

}

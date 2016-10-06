/*
 * Author: Balch
 * Created: 8/10/16 9:53 PM
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

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class PerformanceItemUpdateBroadcaster {
    private static final String TAG = PerformanceItemUpdateBroadcaster.class.getSimpleName();

    private static final String EXTRA_ACCOUNT_ID = "extra_account_id";
    private static final String EXTRA_DAYS_COUNT = "extra_days_count";

    public static final String ACTION = PerformanceItemUpdateBroadcaster.class.getName();

    public static class PerformanceItemUpdateData {
        public final long accountId;
        public final int  days;

        PerformanceItemUpdateData(long accountId, int days) {
            this.accountId = accountId;
            this.days = days;
        }
    }

    static void broadcast(Context context, long accountId, int days) {
        Log.d(TAG, "broadcast sent:" + ACTION);
        Intent intent = new Intent(ACTION);
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
        intent.putExtra(EXTRA_DAYS_COUNT, days);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static PerformanceItemUpdateData getData(Intent intent) {
        return new PerformanceItemUpdateData(intent.getLongExtra(EXTRA_ACCOUNT_ID, -1), intent.getIntExtra(EXTRA_DAYS_COUNT, -1));
    }

}

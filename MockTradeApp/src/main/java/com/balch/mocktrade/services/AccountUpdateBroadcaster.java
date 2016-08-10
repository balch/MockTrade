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

public class AccountUpdateBroadcaster {
    private static final String TAG = AccountUpdateBroadcaster.class.getSimpleName();

    private static final String EXTRA_ACCOUNT_ID = "extra_account_id";

    public static final String ACTION = AccountUpdateBroadcaster.class.getName();

    static public void broadcast(Context context, long accountId) {
        Log.d(TAG, "broadcast sent:" + ACTION);
        Intent intent = new Intent(ACTION);
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    static public long getAccountId(Intent intent) {
        return intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
    }

}

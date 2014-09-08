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

package com.balch.mocktrade.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.balch.android.app.framework.model.ModelFactory;
import com.balch.mocktrade.TradeApplication;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.portfolio.PortfolioModel;

public class BootReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = BootReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG, "BootReceiver onReceive");
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            ModelFactory modelFactory = TradeApplication.getInstance().getModelFactory();
            FinanceModel financeModel = modelFactory.getModel(FinanceModel.class);
            financeModel.setQuoteServiceAlarm();

            PortfolioModel portfolioModel = modelFactory.getModel(PortfolioModel.class);
            portfolioModel.scheduleOrderServiceAlarmIfNeeded();
        }
    }
}
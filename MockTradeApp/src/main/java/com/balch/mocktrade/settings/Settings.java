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

package com.balch.mocktrade.settings;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.balch.mocktrade.R;

import java.util.TimeZone;

public class Settings {

    protected Application context;

    public Settings(Application context) {
        this.context = context;
        PreferenceManager.setDefaultValues(this.context, R.xml.settings_pref_screen, false);
    }

    protected SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    protected String getPrefKey(int resID) {
        return this.context.getResources().getString(resID);
    }

    // Polls start time is in HH:mm format in PST
    public String geMarketOpenTime() {
        return getSharedPrefs().getString(getPrefKey(R.string.market_open_time), "6:30");
    }

    // Polls end time is in HH:mm format in PST
    public String geMarketCloseTime() {
        return getSharedPrefs().getString(getPrefKey(R.string.market_close_time), "13:00");
    }

    // poll interval specified in seconds
    public int getPollInterval() {
        return Integer.parseInt(getSharedPrefs().getString(getPrefKey(R.string.poll_interval), "300"));
    }

    // poll interval specified in seconds for processing open orders
    public int getPollOrderInterval() {
        return Integer.parseInt(getSharedPrefs().getString(getPrefKey(R.string.poll_interval_order), "30"));
    }

    public TimeZone getSavedSettingsTimeZone() {
        return TimeZone.getTimeZone("America/Los_Angeles");
    }


}

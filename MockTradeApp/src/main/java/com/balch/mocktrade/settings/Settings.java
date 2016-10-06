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
import com.balch.mocktrade.services.WearSyncService;

import java.util.TimeZone;

public class Settings {

    public enum Key {
        PREF_HIDE_EXCLUDE_ACCOUNTS("pref_hide_exclude_accounts", true),
        PREF_DEMO_MODE("pref_demo_mode", true),
        PREF_TWENTY_FOUR_HOUR_DISPLAY("pref_twenty_four_hour_display", true),
        PREF_MARKET_OPEN_TIME("market_open_time", false),
        PREF_MARKET_CLOSE_TIME("market_close_time", false),
        PREF_POLL_INTERVAL("poll_interval", false),
        PREF_POLL_INTERVAL_ORDER("poll_interval_order", false),
        PREF_LAST_SYNC_TIME("pref_last_sync_time", false);

        private final String prefKey;
        private final boolean refreshWatch;
        Key(String value, boolean refreshWatch) {
            this.prefKey = value;
            this.refreshWatch = refreshWatch;
        }

        public String key() {
            return prefKey;
        }

        public boolean isRefreshWatch() {
            return refreshWatch;
        }

        public static Key fromKey(String value) {
            Key key = null;
            for (Key k : Key.values()) {
                if (k.prefKey.equals(value)) {
                    key = k;
                    break;
                }
            }

            return key;
        }
    }

    private Application mContext;

    public Settings(Application context) {
        this.mContext = context;
        PreferenceManager.setDefaultValues(this.mContext, R.xml.settings_pref_screen, false);
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this.mContext);
    }

    // Polls start time is in HH:mm format in PST
    public String geMarketOpenTime() {
        return getSharedPrefs().getString(Key.PREF_MARKET_OPEN_TIME.key(), "6:30");
    }

    // Polls end time is in HH:mm format in PST
    public String geMarketCloseTime() {
        return getSharedPrefs().getString(Key.PREF_MARKET_CLOSE_TIME.key(), "13:00");
    }

    // poll interval specified in seconds
    public int getPollInterval() {
        return Integer.parseInt(getSharedPrefs().getString(Key.PREF_POLL_INTERVAL.key(), "300"));
    }

    // poll interval specified in seconds for processing open orders
    public int getPollOrderInterval() {
        return Integer.parseInt(getSharedPrefs().getString(Key.PREF_POLL_INTERVAL_ORDER.key(), "30"));
    }

    public TimeZone getSavedSettingsTimeZone() {
        return TimeZone.getTimeZone("America/Los_Angeles");
    }

    public boolean getBoolean(Key key) {
        return getSharedPrefs().getBoolean(key.key(), false);
    }

    public void setBoolean(Key key, boolean value) {
        getSharedPrefs()
                .edit()
                .putBoolean(key.key(), value)
                .apply();

        if (key.isRefreshWatch()) {
            mContext.startService(WearSyncService.getIntent(mContext, true, true, true, false));
        }

    }

    public long getLastSyncTime() {
        return getSharedPrefs().getLong(Key.PREF_LAST_SYNC_TIME.key(), 0);
    }

    public void setLastSyncTime(long syncTime) {
        getSharedPrefs()
                .edit()
                .putLong(Key.PREF_LAST_SYNC_TIME.key(), syncTime)
                .apply();
    }

}

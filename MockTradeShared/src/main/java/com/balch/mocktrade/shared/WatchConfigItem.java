/*
 * Author: Balch
 * Created: 8/16/16 6:05 AM
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

package com.balch.mocktrade.shared;

import com.google.android.gms.wearable.DataMap;

public class WatchConfigItem {
    private static final String TAG = WatchConfigItem.class.getSimpleName();

    private static final String DATA_KEY = "data_key";
    private static final String DATA_DESCRIPTION = "data_desc";
    private static final String DATA_ENABLED = "data_enabled";

    private final String key;
    private final String description;
    private boolean enabled;

    public WatchConfigItem(String key, String description, boolean enabled) {
        this.key = key;
        this.description = description;
        this.enabled = enabled;
    }

    public WatchConfigItem(DataMap map) {
        this(map.getString(DATA_KEY), map.getString(DATA_DESCRIPTION), map.getBoolean(DATA_ENABLED));
    }

    public DataMap toDataMap() {
        DataMap map = new DataMap();
        map.putString(DATA_KEY, this.key);
        map.putString(DATA_DESCRIPTION, this.description);
        map.putBoolean(DATA_ENABLED, this.enabled);
        return map;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}


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

package com.balch.android.app.framework.domain.widgets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ControlMap {
    protected Map<String, EditLayout> mFieldNameToControlMap = new HashMap<>();

    public EditLayout put(String key, EditLayout control) {
        return this.mFieldNameToControlMap.put(key, control);
    }

    public <T extends EditLayout> T get(String key) {
        return (T) this.mFieldNameToControlMap.get(key);
    }

    public void clear() {
        this.mFieldNameToControlMap.clear();
    }

    public Set<Map.Entry<String,EditLayout>> entrySet() {
        return this.mFieldNameToControlMap.entrySet();
    }
}
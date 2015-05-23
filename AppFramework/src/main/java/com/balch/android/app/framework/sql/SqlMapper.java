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

package com.balch.android.app.framework.sql;

import android.content.ContentValues;
import android.database.Cursor;

import com.balch.android.app.framework.domain.DomainObject;

import java.util.Map;

public interface SqlMapper<T extends DomainObject> {
    String COLUMN_ID = "_id";
    String COLUMN_CREATE_TIME = "create_time";
    String COLUMN_UPDATE_TIME = "update_time";

    String getTableName();

    ContentValues getContentValues(T domainObject);

    void populate(T domainObject, Cursor cursor, Map<String, Integer> columnMap);

}


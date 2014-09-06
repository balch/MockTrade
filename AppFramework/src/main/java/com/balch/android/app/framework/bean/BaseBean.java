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

package com.balch.android.app.framework.bean;

import android.provider.BaseColumns;

import com.balch.android.app.framework.sql.annotations.SqlColumn;
import com.balch.android.app.framework.sql.SqlTable;
import com.balch.android.app.framework.types.ISO8601DateTime;

import java.io.Serializable;

public abstract class BaseBean implements BaseColumns, SqlTable, Serializable {

    @SqlColumn(name = "_id")
    protected Long id;

    @SqlColumn(name = "create_time")
    protected ISO8601DateTime createTime;

    @SqlColumn(name = "update_time")
    protected ISO8601DateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ISO8601DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(ISO8601DateTime createTime) {
        this.createTime = createTime;
    }

    public ISO8601DateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(ISO8601DateTime updateTime) {
        this.updateTime = updateTime;
    }
}

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

package com.balch.mocktrade.model;


import android.content.Context;

import com.balch.android.app.framework.model.ModelFactory;
import com.balch.android.app.framework.model.ModelInitializer;
import com.balch.android.app.framework.sql.SqlConnection;
import com.balch.mocktrade.settings.Settings;

public abstract class SqliteModel implements ModelInitializer {
    protected ModelProvider modelProvider;

    public SqliteModel() {
    }

    public SqliteModel(ModelProvider modelProvider) {
        this.modelProvider = modelProvider;
    }

    public void initialize(ModelProvider modelProvider) {
        this.modelProvider = modelProvider;
    }

    public Context getContext() {
        return modelProvider.getContext();
    }

    public Settings getSettings() {
        return modelProvider.getSettings();
    }

    public ModelFactory getModelFactory() {
        return modelProvider.getModelFactory();
    }

    public SqlConnection getSqlConnection() {
        return modelProvider.getSqlConnection();
    }

    public ModelProvider getModelProvider() {
        return modelProvider;
    }
}

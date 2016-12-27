/*
 * Author: Balch
 * Created: 12/26/16 4:51 PM
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

package com.balch.android.app.framework;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public interface BasePresenter<V extends View & BaseView> {

    V createView();

    void onCreateBase(Bundle savedInstanceState);

    void onResumeBase();

    void onPauseBase();

    void onStartBase();

    void onStopBase();

    void onDestroyBase();

    void onSaveInstanceStateBase(Bundle outState);

    void onActivityResultBase(int requestCode, int resultCode, Intent data);

    boolean onHandleException(String logMsg, Exception ex);
}

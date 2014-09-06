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

package com.balch.android.app.framework;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;


public abstract class BasePresenter<T extends Application> {

    protected LoaderManager loaderManager;
    protected T application;

    protected BasePresenter() {
    }

    protected BasePresenter(T application, LoaderManager loaderManager) {
        this.application = application;
        this.loaderManager = loaderManager;
    }

    public abstract void initialize(Bundle savedInstanceState);

    protected String getString(int resId) {
        return this.application.getString(resId);
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
    }

    public void onStart() {
    }

    public void onResume() {
    }

    public void onSaveInstanceState(Bundle outState) {
    }

    public void onPause() {
    }

    public void onStop() {
    }

    public void onDestroy() {
    }

    public T getApplication() {
         return this.application;
    }

    public void setApplication(T application) {
        this.application = application;
    }

    public void setLoaderManager(LoaderManager loaderManager) {
        this.loaderManager = loaderManager;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }
}
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.balch.android.app.framework.view.BaseView;

public abstract class BaseActivity<V extends View & BaseView> extends Activity {
    private static final String TAG = BaseActivity.class.getName();

    protected BasePresenter presenter;

    abstract protected void initialize(Bundle savedInstanceState);
    abstract protected BasePresenter createPresenter(V view);
    abstract protected V createView();

    protected void handleException(String logMsg, String displayMsg, Exception ex) {
        if (TextUtils.isEmpty(displayMsg)) {
            displayMsg = ex.toString();
        }

        Toast.makeText(this, logMsg + ": "+ displayMsg, Toast.LENGTH_LONG).show();
        Log.e(TAG, logMsg, ex);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            this.initialize(savedInstanceState);
            V view = this.createView();
            view.initializeLayout();

            this.presenter = this.createPresenter(view);
            if (this.presenter != null) {
                this.presenter.setApplication(this.getApplication());
                this.presenter.setLoaderManager(this.getLoaderManager());
                this.presenter.initialize(savedInstanceState);
            }

            this.setContentView(view);

        } catch (Exception ex) {
            handleException("OnCreate ",ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public void onStart() {
        try {
            super.onStart();
            if (this.presenter != null) {
                presenter.onStart();
            }
        } catch (Exception ex) {
            handleException("onStart ",ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public void onResume() {
        try {
            super.onResume();
            if (this.presenter != null) {
                presenter.onResume();
            }
        } catch (Exception ex) {
            handleException("onResume ",ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        try {
            super.onSaveInstanceState(outState);
            if (this.presenter != null) {
                presenter.onSaveInstanceState(outState);
            }
        } catch (Exception ex) {
            handleException("onSaveInstanceState ",ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public void onPause() {
        try {
            if (this.presenter != null) {
                presenter.onPause();
            }
            super.onPause();
        } catch (Exception ex) {
            handleException("onPause ",ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public void onStop() {
        try {
            if (this.presenter != null) {
                presenter.onStop();
            }
            super.onStop();
        } catch (Exception ex) {
            handleException("onStop",ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public void onDestroy() {
        try {
            if (this.presenter != null) {
                presenter.onDestroy();
            }
        } catch (Exception ex) {
            handleException("onDestroy",ex.getLocalizedMessage(), ex);
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (this.presenter != null) {
                // could have result code collisions
                // make sure to also check for existence of result
                presenter.onActivityResult(requestCode, resultCode, data);
            }
        } catch (Exception ex) {
            handleException("onActivityResult",ex.getLocalizedMessage(), ex);
        }
    }

}

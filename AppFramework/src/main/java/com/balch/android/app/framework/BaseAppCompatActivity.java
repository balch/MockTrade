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

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.balch.android.app.framework.view.BaseView;

/**
 * This class enhances the AppCompatActivity functionality by providing View creation abstraction
 * and error handling.
 *
 * @param <V> Type of BaseView to create
 */
public abstract class BaseAppCompatActivity<V extends View & BaseView> extends AppCompatActivity {
    private static final String TAG = BaseAppCompatActivity.class.getSimpleName();

    private String mClassName;

    abstract protected V createView();

    // override-able activity functions
    protected void onCreateBase(Bundle savedInstanceState) {
    }

    protected void onResumeBase() {
    }

    protected void onPauseBase() {
    }

    protected void onStartBase() {
    }

    protected void onStopBase() {
    }

    protected void onDestroyBase() {
    }

    protected void onSaveInstanceStateBase(Bundle outState) {
    }

    protected void onActivityResultBase(int requestCode, int resultCode, Intent data) {
    }


    protected void onHandleException(String logMsg, String displayMsg, Exception ex) {
    }


    public BaseAppCompatActivity() {
        this.mClassName = this.getClass().getSimpleName();
    }

    private void handleException(String logMsg, String displayMsg, Exception ex) {
        Log.e(TAG, logMsg, ex);
        onHandleException(logMsg, displayMsg, ex);
    }

    @Override
    final protected void onCreate(Bundle savedInstanceState) {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.mClassName + " OnCreate - Begin");
        try {
            super.onCreate(savedInstanceState);
            V view = this.createView();
            view.initializeLayout();

            this.setContentView(view);
            this.onCreateBase(savedInstanceState);

        } catch (Exception ex) {
            handleException("OnCreate ", ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.mClassName + " OnCreate - End (ms):" + sw.stop());
    }

    @Override
    final public void onStart() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.mClassName + " onStart - Begin");
        try {
            super.onStart();
            onStartBase();
        } catch (Exception ex) {
            handleException("onStart ", ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.mClassName + " onStart - End (ms):" + sw.stop());
    }

    @Override
    final public void onResume() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.mClassName + " onResume - Begin");
        try {
            super.onResume();
            onResumeBase();
        } catch (Exception ex) {
            handleException("onResume ", ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.mClassName + " onResume - End (ms):" + sw.stop());
    }

    @Override
    final public void onSaveInstanceState(Bundle outState) {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.mClassName + " onSaveInstanceState - Begin");
        try {
            super.onSaveInstanceState(outState);
            onSaveInstanceStateBase(outState);
        } catch (Exception ex) {
            handleException("onSaveInstanceState ", ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.mClassName + " onSaveInstanceState - End (ms):" + sw.stop());
    }

    @Override
    final public void onPause() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.mClassName + " onPause - Begin");
        try {
            onPauseBase();
            super.onPause();
        } catch (Exception ex) {
            handleException("onPause ", ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.mClassName + " onPause - End (ms):" + sw.stop());
    }

    @Override
    final public void onStop() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.mClassName + " onStop - Begin");

        try {
            onStopBase();
            super.onStop();
        } catch (Exception ex) {
            handleException("onStop", ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.mClassName + " onStop - End (ms):" + sw.stop());
    }

    @Override
    final public void onDestroy() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.mClassName + " onDestroy - Begin");
        try {
            onDestroyBase();
        } catch (Exception ex) {
            handleException("onDestroy", ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.mClassName + " onDestroy - End (ms):" + sw.stop());
        super.onDestroy();
    }

    @Override
    final public void onActivityResult(int requestCode, int resultCode, Intent data) {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.mClassName + " onActivityResult - Begin");
        super.onActivityResult(requestCode, resultCode, data);
        try {
            onActivityResultBase(requestCode, resultCode, data);
        } catch (Exception ex) {
            handleException("onActivityResult", ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.mClassName + " onActivityResult - End (ms):" + sw.stop());
    }

    public Snackbar getSnackbar(View parent, String msg, int length) {
        return Snackbar.make(parent, msg, length);
    }

    public Snackbar getSnackbar(View parent, String msg, int length, int backgroundColorId) {
        Snackbar snackbar = getSnackbar(parent, msg, length);
        ViewGroup group = (ViewGroup) snackbar.getView();
        group.setBackgroundColor(ContextCompat.getColor(this, backgroundColorId));
        return snackbar;
    }

    /**
     * TODO: figure out why android.support.design.R.id.snackbar_text
     * does not resolve in aar
     */
    public Snackbar getSnackbar(View parent, String msg, int length,
                                int backgroundColorId, int textColorId, int snackBarTextId) {
        Snackbar snackbar = getSnackbar(parent, msg, length, backgroundColorId);
        ViewGroup group = (ViewGroup) snackbar.getView();

        ((TextView) group.findViewById(snackBarTextId))
                .setTextColor(ContextCompat.getColor(this, textColorId));

        return snackbar;
    }

}

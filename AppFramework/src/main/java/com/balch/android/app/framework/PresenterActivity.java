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
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.ColorRes;
import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * This class enhances the AppCompatActivity functionality by providing View creation abstraction,
 * and error handling.
 *
 * @param <V> Type of BaseView to create
 */
public abstract class PresenterActivity<V extends View & BaseView, P extends BasePresenter>
        extends AppCompatActivity  {
    private static final String TAG = PresenterActivity.class.getSimpleName();

    private String className;

    protected P presenter;

    /**
     * Override abstract method to create a view of type V used by the Presenter.
     * The view id will be managed by this class if not specified
     * @return View containing view logic in the MVP pattern
     */
    protected abstract V createView();

    protected abstract P createPresenter(V view);

    // override-able activity functions
    public void onCreateBase(Bundle savedInstanceState) {
    }

    public void onResumeBase() {
    }

    public void onPauseBase() {
    }

    public void onStartBase() {
    }

    public void onStopBase() {
    }

    public void onDestroyBase() {
    }

    public void onSaveInstanceStateBase(Bundle outState) {
    }

    public void onActivityResultBase(int requestCode, int resultCode, Intent data) {
    }

    public boolean onHandleException(String logMsg, Exception ex) {
        return false;
    }

    public PresenterActivity() {
        this.className = this.getClass().getSimpleName();
    }

    private boolean handleException(String logMsg, Exception ex) {
        Log.e(TAG, logMsg, ex);
        return onHandleException(logMsg, ex);
    }

    @SuppressWarnings("unchecked")
    @Override
    final protected void onCreate(Bundle savedInstanceState) {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className + " OnCreate - Begin");
        try {
            super.onCreate(savedInstanceState);
            V view = this.createView();

            this.setContentView(view);

            Application application = getApplication();
            if (!(application instanceof ModelProvider)) {
                throw new IllegalStateException("Android Application object must be derived from Model Provider");
            }

            presenter = this.createPresenter(view);

            this.onCreateBase(savedInstanceState);

        } catch (Exception ex) {
            if (!handleException("OnCreate ", ex)) {
                throw ex;
            }
        }
        Log.i(TAG, this.className + " OnCreate - End (ms):" + sw.stop());
    }

    @Override
    final public void onStart() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className + " onStart - Begin");
        try {
            super.onStart();
            onStartBase();
        } catch (Exception ex) {
            if (!handleException("onStart ", ex)) {
                throw ex;
            }
        }
        Log.i(TAG, this.className + " onStart - End (ms):" + sw.stop());
    }

    @Override
    final public void onResume() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className + " onResume - Begin");
        try {
            super.onResume();
            onResumeBase();
        } catch (Exception ex) {
            if (!handleException("onResume ", ex)) {
                throw ex;
            }
        }
        Log.i(TAG, this.className + " onResume - End (ms):" + sw.stop());
    }

    @Override
    final public void onSaveInstanceState(Bundle outState) {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className + " onSaveInstanceState - Begin");
        try {
            super.onSaveInstanceState(outState);
            onSaveInstanceStateBase(outState);
        } catch (Exception ex) {
            if (!handleException("onSaveInstanceState ", ex)) {
                throw ex;
            }
        }
        Log.i(TAG, this.className + " onSaveInstanceState - End (ms):" + sw.stop());
    }

    @Override
    final public void onPause() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className + " onPause - Begin");
        try {
            onPauseBase();
            super.onPause();
        } catch (Exception ex) {
            if (!handleException("onPause ", ex)) {
                throw ex;
            }
        }
        Log.i(TAG, this.className + " onPause - End (ms):" + sw.stop());
    }

    @Override
    final public void onStop() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className + " onStop - Begin");

        try {
            onStopBase();
            super.onStop();
        } catch (Exception ex) {
            if (!handleException("onStop", ex)) {
                throw ex;
            }
        }
        Log.i(TAG, this.className + " onStop - End (ms):" + sw.stop());
    }

    @Override
    final public void onDestroy() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className + " onDestroy - Begin");
        try {
            onDestroyBase();
            presenter.cleanup();
            presenter = null;
        } catch (Exception ex) {
            if (!handleException("onDestroy", ex)) {
                throw ex;
            }
        }
        Log.i(TAG, this.className + " onDestroy - End (ms):" + sw.stop());
        super.onDestroy();
    }

    @Override
    final public void onActivityResult(int requestCode, int resultCode, Intent data) {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className + " onActivityResult - Begin");
        super.onActivityResult(requestCode, resultCode, data);
        try {
            onActivityResultBase(requestCode, resultCode, data);
        } catch (Exception ex) {
            if (!handleException("onActivityResult", ex)) {
                throw ex;
            }
        }
        Log.i(TAG, this.className + " onActivityResult - End (ms):" + sw.stop());
    }

    public Snackbar getSnackbar(View parent, String msg, int length) {
        return Snackbar.make(parent, msg, length);
    }

    public Snackbar getSnackbar(View parent, String msg, int length,
                                @ColorRes int backgroundColorId) {
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
                                @ColorRes int backgroundColorId,
                                @ColorRes int textColorId,
                                @IdRes int snackBarTextId) {
        Snackbar snackbar = getSnackbar(parent, msg, length, backgroundColorId);
        ViewGroup group = (ViewGroup) snackbar.getView();

        ((TextView) group.findViewById(snackBarTextId))
                .setTextColor(ContextCompat.getColor(this, textColorId));

        return snackbar;
    }

}

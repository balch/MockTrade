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
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.balch.android.app.framework.view.BaseView;

public abstract class BaseFragment<P extends BasePresenter, V extends View & BaseView>
        extends Fragment implements Refreshable {
    private static final String TAG = BaseFragment.class.getSimpleName();

    protected P presenter;
    private String className;

    abstract protected P createPresenter(V view);
    abstract protected V createView();

    public BaseFragment() {
        this.className = this.getClass().getSimpleName();
    }

    @Override
    public boolean showRefreshMenu() {
        return false;
    }

    @Override
    public void refresh() {
    }

    protected Application getApplication() {
        return this.getActivity().getApplication();
    }

    protected void handleException(String logMsg, String displayMsg, Exception ex) {
        if (TextUtils.isEmpty(displayMsg)) {
            displayMsg = ex.toString();
        }

        Toast.makeText(this.getActivity(), logMsg + ": "+ displayMsg, Toast.LENGTH_LONG).show();
        Log.e(TAG, logMsg, ex);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className +" onCreateView - Begin");
        try {
            V view = this.createView();
            this.presenter = createPresenter(view);
            this.presenter.setApplication(this.getApplication());
            this.presenter.setLoaderManager(this.getLoaderManager());
            view.initializeLayout();
            this.presenter.initialize(savedInstanceState);

            return view;

        } catch (Exception ex) {
            handleException("OnCreate ", ex.getLocalizedMessage(), ex);
            return null;
        } finally {
            Log.i(TAG, this.className + " onCreateView - End Secs:" + sw.stop());
        }
    }

    @Override
    public void onStart() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className +" onStart - Begin");
        try {
            super.onStart();
            if (presenter != null) {
                presenter.onStart();
            }
        } catch (Exception ex) {
            handleException("onStart ", ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.className + " onStart - End Secs:" + sw.stop());
    }

    @Override
    public void onResume() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className +" onResume - Begin");
        try {
            super.onResume();
            if (presenter != null) {
                presenter.onResume();
            }
        } catch (Exception ex) {
            handleException("onResume ",ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.className + " onResume - End Secs:" + sw.stop());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className +" onSaveInstanceState - Begin");
        try {
            super.onSaveInstanceState(outState);
            if (presenter != null) {
                presenter.onSaveInstanceState(outState);
            }
        } catch (Exception ex) {
            handleException("onSaveInstanceState ",ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.className + " onSaveInstanceState - End Secs:" + sw.stop());
    }

    @Override
    public void onPause() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className +" onPause - Begin");
        try {
            if (presenter != null) {
                presenter.onPause();
            }
            super.onPause();
        } catch (Exception ex) {
            handleException("onPause ",ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.className + " onPause - End Secs:" + sw.stop());
    }

    @Override
    public void onStop() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className +" onStop - Begin");
        try {
            if (presenter != null) {
                presenter.onStop();
            }
            super.onStop();
        } catch (Exception ex) {
            handleException("onStop",ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.className + " onStop - End Secs:" + sw.stop());
    }

    @Override
    public void onDestroy() {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className + " onDestroy - Begin");
        try {
            if (presenter != null) {
                presenter.onDestroy();
            }
        } catch (Exception ex) {
            handleException("onDestroy",ex.getLocalizedMessage(), ex);
        }
        super.onDestroy();
        Log.i(TAG, this.className + " onDestroy - End Secs:" + sw.stop());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        StopWatch sw = StopWatch.newInstance();
        Log.d(TAG, this.className + " onActivityResult - Begin");
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (presenter != null) {
                presenter.onActivityResult(requestCode, resultCode, data);
            }
        } catch (Exception ex) {
            handleException("onActivityResult",ex.getLocalizedMessage(), ex);
        }
        Log.i(TAG, this.className + " onActivityResult - End Secs:" + sw.stop());
    }

}

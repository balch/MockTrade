/*
 * Author: Balch
 * Created: 9/4/14 8:56 PM
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

package com.balch.mocktrade;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.balch.android.app.framework.NavBarActivity;
import com.balch.android.app.framework.Refreshable;

public class WebViewFragment extends Fragment implements Refreshable {

    private static final String ARG_URL = "URL";

    protected WebView webView;

    public WebViewFragment() {

    }

    public WebViewFragment setCustomArguments(String url) {
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        this.setArguments(args);

        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final NavBarActivity navBarActivity = (getActivity() instanceof NavBarActivity) ?
                (NavBarActivity)getActivity() : null;
        if  (navBarActivity != null) {
            navBarActivity.showProgress();
        }

        this.webView = new WebView(container.getContext());
        this.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if  (navBarActivity != null) {
                    navBarActivity.hideProgress();
                }
            }
        });


        this.webView.loadUrl(this.getArguments().getString(ARG_URL));

        return this.webView;
    }

    @Override
    public void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    public void onResume() {
        webView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (this.webView != null) {
            this.webView.destroy();
            this.webView = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean showRefreshMenu() {
        return true;
    }

    @Override
    public void refresh() {

        final NavBarActivity navBarActivity = (getActivity() instanceof NavBarActivity) ?
                (NavBarActivity)getActivity() : null;
        if  (navBarActivity != null) {
            navBarActivity.showProgress();
        }

        this.webView.reload();
    }
}

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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.balch.android.app.framework.nav.NavBar;

public abstract class NavBarActivity extends AppCompatActivity implements ActivityProvider {

    private ProgressBar progressBar;
    private ImageView refreshImageButton;
    protected NavBar navBar;
    protected LinearLayout rootLayout;
    protected FrameLayout frameLayout;

    abstract protected void configureNavBar(NavBar navBar, Bundle savedInstanceState);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.template_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.template_toolbar);
        setSupportActionBar(toolbar);

        progressBar = (ProgressBar)findViewById(R.id.template_progress_bar);

        refreshImageButton = (ImageView) findViewById(R.id.template_refresh_button);
        refreshImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.template_place_holder);
                if (fragment instanceof Refreshable) {
                    ((Refreshable) fragment).refresh();
                }
            }
        });

        this.navBar = (NavBar)findViewById(R.id.nav_bar_main);
        this.rootLayout = (LinearLayout)findViewById(R.id.template_layout);
        this.frameLayout = (FrameLayout)findViewById(R.id.template_place_holder);

        configureNavBar(this.navBar, savedInstanceState);

        if (savedInstanceState == null) {
        }

        final FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                int backStackCount = fragmentManager.getBackStackEntryCount();
                if (backStackCount > 0) {
                    int selectedIndex = Integer.parseInt(fragmentManager.getBackStackEntryAt(backStackCount - 1).getName());
                    if (selectedIndex != NavBarActivity.this.navBar.getSelectedIndex()) {
                        NavBarActivity.this.navBar.setSelectedIndex(selectedIndex);
                    }
                } else {
                    // required b/c the first stack frame has an empty fragment
                    finish();
                }
            }
        });

        this.navBar.completeConfiguration();
    }



    protected void showRefreshMenuOption(boolean show) {
        refreshImageButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showProgress() {
        this.progressBar.setVisibility(View.VISIBLE);
        this.refreshImageButton.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress() {
        this.progressBar.setVisibility(View.GONE);
        this.refreshImageButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void showView(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.template_place_holder, fragment)
                .addToBackStack(String.valueOf(this.navBar.getSelectedIndex()))
                .commit();

        this.showRefreshMenuOption((fragment instanceof Refreshable) && ((Refreshable) fragment).showRefreshMenu());
    }

    @Override
    public FragmentManager getFragManager() {
        return getSupportFragmentManager();
    }

    public void setBackground(Drawable background) {
        this.rootLayout.setBackground(background);
        this.frameLayout.setBackground(background);
    }


}

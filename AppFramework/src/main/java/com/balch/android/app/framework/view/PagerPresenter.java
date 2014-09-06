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

package com.balch.android.app.framework.view;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.balch.android.app.framework.BaseFragment;
import com.balch.android.app.framework.BasePresenter;

import java.util.List;

public class PagerPresenter extends BasePresenter<Application> {
    private static final String TAG = PagerPresenter.class.getName();

    protected final PagerView view;
    protected final FragmentManager fragmentManager;
    protected final List<BaseFragment> fragments;
    protected SlidePagerAdapter pagerAdapter;

    public PagerPresenter(PagerView view, List<BaseFragment> fragments, FragmentManager fragmentManager) {
        this.view = view;
        this.fragments = fragments;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public void initialize(Bundle savedInstanceState) {
        this.pagerAdapter = new SlidePagerAdapter(fragmentManager);
        this.view.setSwipeEnabled(false);
        this.view.setAdapter(this.pagerAdapter);
    }

    public void showPage(int idx) {
        this.view.setCurrentItem(idx);
    }

    private class SlidePagerAdapter extends FragmentStatePagerAdapter {
        public SlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = pagerAdapter.getItem(view.getCurrentItem());
        fragment.onActivityResult(requestCode, resultCode, data);
    }
}
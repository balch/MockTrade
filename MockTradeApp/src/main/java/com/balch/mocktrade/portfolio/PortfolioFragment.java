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

package com.balch.mocktrade.portfolio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.balch.android.app.framework.BaseFragment;
import com.balch.android.app.framework.model.ModelFactory;
import com.balch.mocktrade.MainActivity;
import com.balch.mocktrade.model.ModelProvider;

public class PortfolioFragment extends BaseFragment<PortfolioPresenter, PortfolioView>
        implements PortfolioPresenter.PortfolioPresenterListener{

    @Override
    protected PortfolioPresenter createPresenter(PortfolioView view) {
        ModelFactory modelFactory = ((ModelProvider)this.getApplication()).getModelFactory();
        PortfolioModel model = modelFactory.getModel(PortfolioModel.class);
        return new PortfolioPresenter(model, view, this);
    }

    @Override
    protected PortfolioView createView() {
        return new PortfolioView(this.getActivity());
    }

    @Override
    public boolean showRefreshMenu() {
        return true;
    }

    @Override
    public void refresh() {
        this.presenter.refresh();
    }

    @Override
    public void onStartActivityForResult(Intent intent, int resultCode) {
        startActivityForResult(intent, resultCode);
    }

    @Override
    public void showView(Fragment fragment) {
        ((MainActivity)getActivity()).showView(fragment);
    }

    @Override
    public void showProgress() {
        ((MainActivity)getActivity()).showProgress();
    }

    @Override
    public void hideProgress() {
        ((MainActivity)getActivity()).hideProgress();
    }

    @Override
    public ComponentName onStartService(Intent intent) {
        return getActivity().startService(intent);
    }

    @Override
    public Context getContext() {
        return getActivity();
    }
}

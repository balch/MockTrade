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

import com.balch.android.app.framework.BaseFragment;
import com.balch.android.app.framework.model.ModelFactory;
import com.balch.mocktrade.TradeApplication;

public class PortfolioFragment extends BaseFragment<PortfolioPresenter, PortfolioView> {

    @Override
    protected PortfolioPresenter createPresenter(PortfolioView view) {
        ModelFactory modelFactory = TradeApplication.getInstance().getModelFactory();
        PortfolioModel model = modelFactory.getModel(PortfolioModel.class);
        return new PortfolioPresenter(this, model, view);
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
}

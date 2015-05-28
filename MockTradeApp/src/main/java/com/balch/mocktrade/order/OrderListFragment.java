/*
 * Author: Balch
 * Created: 9/8/14 3:40 PM
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

package com.balch.mocktrade.order;

import android.os.Bundle;

import com.balch.android.app.framework.ActivityProvider;
import com.balch.android.app.framework.BaseFragment;
import com.balch.android.app.framework.model.ModelFactory;
import com.balch.mocktrade.model.ModelProvider;

public class OrderListFragment extends BaseFragment<OrderListPresenter, OrderListView> {

    private static final String ARG_ACCOUNT_ID = "ACCOUNT_ID";

    public OrderListFragment setCustomArguments(Long accountId) {
        Bundle args = new Bundle();
        args.putLong(ARG_ACCOUNT_ID, accountId);
        this.setArguments(args);

        return this;
    }

    @Override
    protected OrderListPresenter createPresenter(OrderListView view) {
        ModelFactory modelFactory = ((ModelProvider)this.getApplication()).getModelFactory();
        OrderModel model = modelFactory.getModel(OrderModel.class);
        return new OrderListPresenter((ActivityProvider)this.getActivity(), this.getArguments().getLong(ARG_ACCOUNT_ID), model, view);
    }

    @Override
    protected OrderListView createView() {
        return new OrderListView(this.getActivity());
    }

    @Override
    public boolean showRefreshMenu() {
        return true;
    }

    @Override
    public void refresh() {
        this.presenter.reload(true);
    }
}

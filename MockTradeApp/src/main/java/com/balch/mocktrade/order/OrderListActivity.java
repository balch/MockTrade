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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.balch.android.app.framework.PresenterActivity;
import com.balch.mocktrade.R;
import com.balch.mocktrade.TradeModelProvider;

public class OrderListActivity extends PresenterActivity<OrderListView, OrderPresenter> {

    private static final String EXTRA_ACCOUNT_ID = "EXTRA_ACCOUNT_ID";

    public static Intent newIntent(Context context, long accountId) {
        Intent intent = new Intent(context, OrderListActivity.class);
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
        return intent;
    }

    @Override
    public void onCreateBase(Bundle bundle) {

        Toolbar toolbar = findViewById(R.id.order_list_view_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        presenter.reload(true);
    }

    @Override
    public OrderListView createView() {
        return new OrderListView(this);
    }

    @Override
    protected OrderPresenter createPresenter(OrderListView view) {
        return new OrderPresenter((TradeModelProvider) getApplication(),
                getOrderViewModel(), getIntent().getLongExtra(EXTRA_ACCOUNT_ID, 0),
                this, view, new OrderPresenter.ActivityBridge() {
            @Override
            public void showProgress(boolean show) {
                OrderListActivity.this.showProgress(show);
            }

            @Override
            public void finish() {
                OrderListActivity.this.finish();
            }
        });
    }

    public void showProgress(boolean show) {
//        this.progressBar.setVisibility(show ? View.VISIBLE: View.GONE);
//        this.refreshImageButton.setVisibility(show ? View.GONE : View.VISIBLE);
    }


    private OrderViewModel getOrderViewModel() {
        return ViewModelProviders.of(this).get(OrderViewModel.class);
    }
}

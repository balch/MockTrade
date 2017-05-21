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

import android.app.AlertDialog;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.balch.android.app.framework.PresenterActivity;
import com.balch.mocktrade.R;
import com.balch.mocktrade.TradeModelProvider;
import com.balch.mocktrade.finance.GoogleFinanceApi;

import java.util.List;

public class OrderListActivity extends PresenterActivity<OrderListView, TradeModelProvider>
        implements LifecycleRegistryOwner {

    private static final String TAG = OrderListActivity.class.getSimpleName();
    private static final String EXTRA_ACCOUNT_ID = "EXTRA_ACCOUNT_ID";

    private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    private OrderViewModel orderViewModel;
    private OrderModel orderModel;
    private Long accountId;

    public static Intent newIntent(Context context, long accountId) {
        Intent intent = new Intent(context, OrderListActivity.class);
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
        return intent;
    }

    @Override
    public void onCreateBase(Bundle bundle) {

        accountId = getIntent().getLongExtra(EXTRA_ACCOUNT_ID, 0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.order_list_view_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        view.setOrderItemViewListener(new OrderItemView.OrderItemViewListener() {
            @Override
            public boolean onCancelOrder(final Order order) {
                new AlertDialog.Builder(OrderListActivity.this)
                        .setTitle(R.string.order_cancel_dlg_title)
                        .setMessage(getString(R.string.order_cancel_dlg_message_format, order.getId(), order.getSymbol()))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    orderModel.cancelOrder(order);
                                    reload(true);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error Canceling Order", ex);
                                    Toast.makeText(OrderListActivity.this, "Error Canceling Order", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }
        });

        orderViewModel.getOrders().observe(this, orderDataObserver);

        reload(true);
    }

    @Override
    public void onDestroyBase() {
        orderViewModel.getOrders().removeObserver(orderDataObserver);
    }

    @Override
    public OrderListView createView() {
        return new OrderListView(this);
    }

    @Override
    protected void createModel(TradeModelProvider modelProvider) {
        orderViewModel = getOrderViewModel();
        if (!orderViewModel.isInitialized()) {
            orderModel = new OrderSqliteModel(modelProvider.getContext(),
                    modelProvider.getModelApiFactory().getModelApi(GoogleFinanceApi.class),
                    modelProvider.getSqlConnection(),
                    modelProvider.getSettings());
            orderViewModel.setOrderModel(orderModel);
        } else {
            orderModel = orderViewModel.getOrderModel();
        }
    }

    public void reload(boolean showProgress) {
        if (showProgress) {
            showProgress();
        }
        orderViewModel.loadOrders(accountId);
    }

    private Observer<List<Order>> orderDataObserver = new Observer<List<Order>>() {
        @Override
        public void onChanged(@Nullable List<Order> data) {
            if (data.size() == 0) {
                finish();
                return;
            }

            view.bind(data);
            hideProgress();
        }
    };


    public void showProgress() {
//        this.progressBar.setVisibility(View.VISIBLE);
//        this.refreshImageButton.setVisibility(View.GONE);
    }

    public void hideProgress() {
//        this.progressBar.setVisibility(View.GONE);
//        this.refreshImageButton.setVisibility(View.VISIBLE);
    }

    private OrderViewModel getOrderViewModel() {
        return ViewModelProviders.of(this).get(OrderViewModel.class);
    }

    @Override
    public LifecycleRegistry getLifecycle() {
        return lifecycleRegistry;
    }
}

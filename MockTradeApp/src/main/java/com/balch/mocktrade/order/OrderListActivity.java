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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.balch.android.app.framework.BaseAppCompatActivity;
import com.balch.android.app.framework.model.ModelFactory;
import com.balch.mocktrade.R;
import com.balch.mocktrade.model.ModelProvider;

import java.util.ArrayList;
import java.util.List;

public class OrderListActivity extends BaseAppCompatActivity<OrderListView>
        implements LoaderManager.LoaderCallbacks<List<Order>>{
    private static final String TAG = OrderListActivity.class.getSimpleName();

    protected static final int ORDER_LOADER_ID = 0;
    private static final String EXTRA_ACCOUNT_ID = "EXTRA_ACCOUNT_ID";

    protected OrderModel mOrderModel;
    protected OrderListView mOrderListView;
    protected Long mAccountId;

    public static Intent newIntent(Context context, long accountId) {
        Intent intent = new Intent(context, OrderListActivity.class);
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);

        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
        return intent;
    }

    @Override
    protected void onCreateBase(Bundle bundle) {

        Toolbar toolbar = (Toolbar) findViewById(R.id.order_list_view_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        ModelFactory modelFactory = ((ModelProvider)this.getApplication()).getModelFactory();
        mOrderModel = modelFactory.getModel(OrderModel.class);

        this.mOrderListView.setOrderItemViewListener(new OrderItemView.OrderItemViewListener() {
            @Override
            public boolean onCancelOrder(final Order order) {
                new AlertDialog.Builder(OrderListActivity.this)
                        .setTitle(R.string.order_cancel_dlg_title)
                        .setMessage(getString(R.string.order_cancel_dlg_message_format, order.getId(), order.getSymbol()))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    mOrderModel.cancelOrder(order);
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
        reload(true);

    }


    @Override
    protected OrderListView createView() {
        this.mOrderListView = new OrderListView(this);
        return this.mOrderListView;
    }

    public void reload(boolean showProgress) {
        if (showProgress) {
            showProgress();
        }
        getSupportLoaderManager().initLoader(ORDER_LOADER_ID, null, this).forceLoad();
    }

    @Override
    public Loader<List<Order>> onCreateLoader(int id, Bundle args) {
        return new OrderLoader(this, this.mAccountId, this.mOrderModel);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<List<Order>> loader, List<Order> data) {

        if (data.size() == 0) {
            finish();
            return;
        }

        this.mOrderListView.bind(data);
        hideProgress();

        // hack to prevent onLoadFinished being called twice
        // http://stackoverflow.com/questions/11293441/android-loadercallbacks-onloadfinished-called-twice/22183247
        getSupportLoaderManager().destroyLoader(ORDER_LOADER_ID);

    }

    @Override
    public void onLoaderReset(Loader<List<Order>> loader) {
        this.mOrderListView.bind(new ArrayList<Order>());
    }

    protected static class OrderLoader extends AsyncTaskLoader<List<Order>> {
        protected OrderModel model;
        protected Long accountId;


        public OrderLoader(Context context, Long accountId, OrderModel model) {
            super(context);
            this.accountId = accountId;
            this.model = model;
        }

        @Override
        public List<Order> loadInBackground() {
            return model.getOpenOrders(this.accountId);
        }
    }


    public void showProgress() {
//        this.progressBar.setVisibility(View.VISIBLE);
//        this.refreshImageButton.setVisibility(View.GONE);
    }

    public void hideProgress() {
//        this.progressBar.setVisibility(View.GONE);
//        this.refreshImageButton.setVisibility(View.VISIBLE);
    }

}

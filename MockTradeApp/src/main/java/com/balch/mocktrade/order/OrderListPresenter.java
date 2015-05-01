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
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.Toast;

import com.balch.android.app.framework.BasePresenter;
import com.balch.mocktrade.R;
import com.balch.mocktrade.TradeApplication;

import java.util.ArrayList;
import java.util.List;

public class OrderListPresenter extends BasePresenter<TradeApplication> implements LoaderManager.LoaderCallbacks<List<Order>> {
    private static final String TAG = OrderListPresenter.class.getName();

    protected static final int ORDER_LOADER_ID = 0;

    protected OrderModel model;
    protected OrderListView view;
    protected Long accountId;

    final Handler handler = new Handler();


    public OrderListPresenter(Long accountId, OrderModel model, OrderListView view) {
        this.accountId = accountId;
        this.model = model;
        this.view = view;
    }

    @Override
    public void initialize(Bundle savedInstanceState) {
        this.view.setOrderItemViewListener(new OrderItemView.OrderItemViewListener() {
            @Override
            public boolean onCancelOrder(final Order order) {
                new AlertDialog.Builder(application.getActivity())
                        .setTitle(R.string.order_cancel_dlg_title)
                        .setMessage(getString(R.string.order_cancel_dlg_message_format, order.getId(), order.getSymbol()))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    model.cancelOrder(order);
                                    reload(true);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error Canceling Order", ex);
                                    Toast.makeText(application, "Error Canceling Order", Toast.LENGTH_LONG).show();
                                }
                            }})
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }
        });
        reload(true);
    }

    public void reload(boolean showProgress) {
        if (showProgress) {
            application.getActivity().showProgress();
        }
        this.loaderManager.initLoader(ORDER_LOADER_ID, null, this).forceLoad();
    }

    @Override
    public Loader<List<Order>> onCreateLoader(int id, Bundle args) {
        return new OrderLoader(this.application, this.accountId, this.model);
    }

    @Override
    public void onLoadFinished(Loader<List<Order>> loader, List<Order> data) {

        if (data.size() == 0) {
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    application.closeCurrentView();
                }
            });
            return;
        }

        this.view.bind(data);
        application.getActivity().hideProgress();

        // hack to prevent onLoadFinished being called twice
        // http://stackoverflow.com/questions/11293441/android-loadercallbacks-onloadfinished-called-twice/22183247
        this.loaderManager.destroyLoader(ORDER_LOADER_ID);
    }

    @Override
    public void onLoaderReset(Loader<List<Order>> loader) {
        this.view.bind(new ArrayList<Order>());
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

}

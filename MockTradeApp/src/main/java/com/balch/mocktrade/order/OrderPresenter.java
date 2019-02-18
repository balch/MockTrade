/*
 * Author: Balch
 * Created: 8/20/17 2:37 PM
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
 * Copyright (C) 2017
 *
 */

package com.balch.mocktrade.order;

import android.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.balch.android.app.framework.BasePresenter;
import com.balch.mocktrade.R;
import com.balch.mocktrade.TradeModelProvider;

import java.util.List;

public class OrderPresenter extends BasePresenter<OrderListView> {

    private static final String TAG = OrderListActivity.class.getSimpleName();

    public interface ActivityBridge {
        void showProgress(boolean show);
        void finish();
    }

    private OrderViewModel orderViewModel;
    private OrderModel orderModel;
    private long accountId;
    private ActivityBridge listener;

    public OrderPresenter(TradeModelProvider modelProvider, OrderViewModel orderViewModel,
                          long accountId, final LifecycleOwner lifecycleOwner,
                          OrderListView view, ActivityBridge listener) {
        super(view);
        final Context context = modelProvider.getContext();
        this.listener = listener;
        this.accountId = accountId;
        this.orderViewModel = orderViewModel;

        if (!orderViewModel.isInitialized()) {
            orderModel = new OrderSqliteModel(modelProvider.getContext(),
                    modelProvider.getFinanceModel(),
                    modelProvider.getSqlConnection(),
                    modelProvider.getSettings());
            orderViewModel.setOrderModel(orderModel);
        } else {
            orderModel = orderViewModel.getOrderModel();
        }

        view.setOrderItemViewListener(order -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.order_cancel_dlg_title)
                    .setMessage(context.getString(R.string.order_cancel_dlg_message_format, order.getId(), order.getSymbol()))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        try {
                            orderModel.cancelOrder(order);
                            reload(true);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error Canceling Order", ex);
                            Toast.makeText(context, "Error Canceling Order", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
            return true;
        });

        orderViewModel.getOrders().observe(lifecycleOwner, orderDataObserver);

    }

    public void reload(boolean showProgress) {
        if (showProgress) {
            listener.showProgress(true);
        }
        orderViewModel.loadOrders(accountId);
    }

    private Observer<List<Order>> orderDataObserver = data -> {
        if (data.size() == 0) {
            listener.finish();
            return;
        }

        view.bind(data);
        listener.showProgress(false);
    };



    @Override
    protected void cleanup() {

        if (orderViewModel != null) {
            orderViewModel.getOrders().removeObserver(orderDataObserver);
            orderViewModel = null;
        }

        orderModel = null;
        listener = null;

        super.cleanup();
    }
}

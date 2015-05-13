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

package com.balch.mocktrade.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.balch.android.app.framework.TemplateActivity;
import com.balch.android.app.framework.model.ModelFactory;
import com.balch.android.app.framework.model.RequestListener;
import com.balch.mocktrade.R;
import com.balch.mocktrade.TradeApplication;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.order.OrderResult;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioPresenter;
import com.balch.mocktrade.receivers.OrderReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderService extends IntentService {
    private static final String TAG = OrderService.class.getName();

    public OrderService() {
        super(OrderService.class.getName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        try {
            ModelFactory modelFactory = TradeApplication.getInstance().getModelFactory();
            FinanceModel financeModel = modelFactory.getModel(FinanceModel.class);
            final PortfolioModel portfolioModel = modelFactory.getModel(PortfolioModel.class);
            final List<Order> orders = portfolioModel.getOpenOrders();

            if (orders.size() > 0) {
                List<String> symbols = new ArrayList<>(orders.size());
                for (Order o : orders) {
                    symbols.add(o.getSymbol());
                }

                financeModel.getQuotes(symbols, new RequestListener<Map<String, Quote>>() {
                    @Override
                    public void onResponse(Map<String, Quote> quoteMap) {
                        try {
                            boolean updateView = false;
                            boolean reschedule = false;
                            for (Order o : orders) {
                                try {
                                    Quote quote = quoteMap.get(o.getSymbol());
                                    OrderResult orderResult = portfolioModel.attemptExecuteOrder(o, quote);
                                    if (orderResult.isSuccess()) {

                                        String msg = (o.getAction() == Order.OrderAction.BUY) ?
                                                String.format(getString(R.string.notification_order_buy_success_format),
                                                        o.getSymbol(), o.getQuantity(),
                                                        orderResult.getPrice().getCurrency(),
                                                        orderResult.getCost().getCurrency()) :
                                                String.format(getString(R.string.notification_order_sell_success_format),
                                                        o.getSymbol(), o.getQuantity(),
                                                        orderResult.getPrice().getCurrency(),
                                                        orderResult.getValue().getCurrency(),
                                                        orderResult.getProfit().getCurrency());


                                        sendNotification(o, msg);
                                        updateView = true;
                                    } else {
                                        reschedule = true;
                                    }

                                } catch (Exception ex) {
                                    Log.e(TAG, "attemptExecuteOrder exception", ex);
                                    sendNotification(o, String.format(getString(R.string.notification_order_error_format),
                                            o.getId(), o.getSymbol(), ex.getMessage()));
                                }
                            }

                            if (updateView) {
                                PortfolioPresenter.updateView(OrderService.this);
                            }

                            if (reschedule) {
                                portfolioModel.scheduleOrderServiceAlarm();
                            }


                        } finally {
                            OrderReceiver.completeWakefulIntent(intent);
                        }
                    }

                    @Override
                    public void onErrorResponse(String error) {
                        // failed to return quotes
                        // Error has been logged
                        OrderReceiver.completeWakefulIntent(intent);
                    }
                });
            }
        } catch (Exception ex) {
            Log.e(TAG, "onHandleIntent exception", ex);
            OrderReceiver.completeWakefulIntent(intent);
        }
    }

    private void sendNotification(Order order, String msg) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(this.getString(R.string.notification_order_title))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                        .setContentText(msg);

        Intent clickIntent = new Intent(this, TemplateActivity.class);

        PendingIntent pendingClickIntent =
                PendingIntent.getActivity(this, 0 , clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingClickIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        int id = (int)(order.getId() % Integer.MAX_VALUE);
        notificationManager.notify(id, builder.build());
    }

    public static Intent getIntent(Context context) {
        return new Intent(context, OrderService.class);
    }

}
/*
 * Author: Balch
 * Created: 9/6/14 10:05 AM
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.receivers.OrderReceiver;
import com.balch.mocktrade.settings.Settings;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Date;

/**
 * This is package-private on purpose!!! It is intended to contain
 * shared functionality between OrderModel implementations
 */
class OrderManager {
    private static final String TAG = OrderManager.class.getSimpleName();

    public interface OrderManagerListener {
        OrderResult executeOrder(Order order, Quote quote, Money price) throws SQLException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException;
        Investment getInvestmentBySymbol(String symbol, Long accountId);
        boolean updateOrder(Order order) throws IllegalAccessException;
    }

    protected final FinanceModel financeModel;
    protected final Context context;
    protected final Settings settings;
    protected final OrderManagerListener listener;

    public OrderManager(Context context, FinanceModel financeModel, Settings settings,
                        OrderManagerListener listener) {
        this.settings = settings;
        this.context = context;
        this.financeModel = financeModel;
        this.listener = listener;
    }

    public void scheduleOrderServiceAlarm(boolean isMarketOpen){
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = OrderReceiver.getIntent(context);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Date startTime = isMarketOpen ?
                new Date(System.currentTimeMillis() + settings.getPollOrderInterval()*1000) :
                this.financeModel.nextMarketOpen();
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                startTime.getTime(),
                pendingIntent);
    }

    public OrderResult attemptExecuteOrder(Order order, Quote quote) throws InvocationTargetException, SQLException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        if (quote == null) {
            throw new IllegalArgumentException("Quote not found");
        }

        OrderResult result;

        switch (order.getStrategy()) {
            case MARKET:
                result = executeMarketOrder(order, quote);
                break;

            case MANUAL:
                result = this.listener.executeOrder(order, quote, order.getLimitPrice());
                break;

            case LIMIT:
                result = executeLimitOrder(order, quote);
                break;

            case STOP_LOSS:
                result = executeStopLossOrder(order, quote);
                break;

            case TRAILING_STOP_AMOUNT_CHANGE:
            case TRAILING_STOP_PERCENT_CHANGE:
                result = executeTrailingStopLossOrder(order, quote);
                break;

            default:
                throw new UnsupportedOperationException();
        }

        return result;
    }

    protected boolean isQuoteValid(Quote quote)  {
        Date tradeDate = quote.getLastTradeTime();
        return (financeModel.isMarketOpen() && isToday(tradeDate));
    }

    protected boolean isToday(Date date) {
        return DateUtils.isToday(date.getTime());
    }

    protected OrderResult executeLimitOrder(Order order, Quote quote) throws InvocationTargetException, SQLException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        OrderResult orderResult = new OrderResult(false, null, null, null, 0);
        if (this.isQuoteValid(quote)) {
            int compareQuoteToLimit = quote.getPrice().compareTo(order.getLimitPrice());
            if ( ((order.getAction() == Order.OrderAction.BUY)  && (compareQuoteToLimit <= 0)) ||
                    ((order.getAction() == Order.OrderAction.SELL) && (compareQuoteToLimit >= 0))) {
                orderResult = this.listener.executeOrder(order, quote, quote.getPrice());
            }
        }

        return orderResult;
    }

    protected OrderResult executeTrailingStopLossOrder(Order order, Quote quote) throws InvocationTargetException, SQLException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        if (order.getAction() == Order.OrderAction.BUY) {
            throw new UnsupportedOperationException("Cannot have a Stop Loss order if the action is BUY");
        }

        boolean highestPriceChanged = false;

        if (order.getHighestPrice().getDollars() == 0.0) {
            Investment investment = listener.getInvestmentBySymbol(order.getSymbol(), order.getAccount().getId());
            if (investment == null) {
                throw new IllegalArgumentException("Can't sell and investment you don't own");
            }

            order.setHighestPrice(investment.getPrice());
            highestPriceChanged = true;
        }

        OrderResult orderResult = new OrderResult(false, null, null, null, 0);
        if (this.isQuoteValid(quote)) {
            if (quote.getPrice().compareTo(order.getHighestPrice()) > 0) {
                order.setHighestPrice(quote.getPrice());
                highestPriceChanged = true;
            } else {
                Money delta = Money.subtract(order.getHighestPrice(), quote.getPrice());

                boolean executeOrder;
                if (order.getStrategy() == Order.OrderStrategy.TRAILING_STOP_AMOUNT_CHANGE) {
                    executeOrder = (delta.compareTo(order.getStopPrice()) >= 0);
                } else if (order.getStrategy() == Order.OrderStrategy.TRAILING_STOP_PERCENT_CHANGE) {
                    double percent = delta.getDollars() * 100f / order.getHighestPrice().getDollars();
                    executeOrder = percent >= order.getStopPercent();
                } else {
                    throw new IllegalArgumentException("Invalid Order Strategy: " + order.getStrategy());
                }

                if (executeOrder) {
                    orderResult = this.listener.executeOrder(order, quote, quote.getPrice());
                }
            }
        }

        if (highestPriceChanged) {
            if (!listener.updateOrder(order)) {
                throw new IllegalArgumentException("Error updating order");
            }
        }

        return orderResult;
    }

    protected OrderResult executeStopLossOrder(Order order, Quote quote) throws InvocationTargetException, SQLException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        if (order.getAction() == Order.OrderAction.BUY) {
            throw new UnsupportedOperationException("Cannot have a Stop Loss order if the action is BUY");
        }

        OrderResult orderResult = new OrderResult(false, null, null, null, 0);
        if (this.isQuoteValid(quote)) {
            int compareQuoteToLimit = quote.getPrice().compareTo(order.getLimitPrice());
            if (compareQuoteToLimit <= 0) {
                orderResult = this.listener.executeOrder(order, quote, quote.getPrice());
            }
        }

        return orderResult;
    }

    protected OrderResult executeMarketOrder(Order order, Quote quote) throws InvocationTargetException, SQLException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        OrderResult orderResult = new OrderResult(false, null, null, null, 0);
        if (this.isQuoteValid(quote)) {
            orderResult = this.listener.executeOrder(order, quote, quote.getPrice());
        }

        return orderResult;
    }

}

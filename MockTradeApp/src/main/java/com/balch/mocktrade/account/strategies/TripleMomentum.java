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

package com.balch.mocktrade.account.strategies;

import android.util.Log;

import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.portfolio.PortfolioUpdateBroadcaster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TripleMomentum extends BaseStrategy {
    private static final String TAG = TripleMomentum.class.getSimpleName();

    private static final double TRAILING_PERCENTAGE = 2.0;

    private static final String[] SYMBOLS = {"TQQQ", "SQQQ"};

    public void initialize(Account account) {
        executeStrategy(Arrays.asList(SYMBOLS), account);
    }

    private void executeStrategy(final List<String> symbols, final Account account) {
        Map<String, Quote> response = this.financeModel.getQuotes(symbols).blockingFirst();
        if (response != null) {
            for (Quote quote : response.values()) {
                double fundsPerOrder = account.getAvailableFunds().getDollars() / (double) symbols.size();

                long quantity = (long) (fundsPerOrder / quote.getPrice().getDollars());
                Order order = new Order();
                order.setAccount(account);
                order.setSymbol(quote.getSymbol());
                order.setAction(Order.OrderAction.BUY);
                order.setStrategy(Order.OrderStrategy.MANUAL);
                order.setLimitPrice(quote.getPrice());
                order.setQuantity(quantity);

                portfolioModel.createOrder(order);
                try {
                    portfolioModel.attemptExecuteOrder(order, quote);

                    Order sellOrder = new Order();
                    sellOrder.setAccount(account);
                    sellOrder.setSymbol(quote.getSymbol());
                    sellOrder.setStrategy(Order.OrderStrategy.TRAILING_STOP_PERCENT_CHANGE);
                    sellOrder.setAction(Order.OrderAction.SELL);
                    sellOrder.setStopPercent(TRAILING_PERCENTAGE);
                    sellOrder.setQuantity(quantity);
                    portfolioModel.createOrder(sellOrder);

                } catch (Exception e) {
                    Log.e(TAG, "Error executing order", e);
                }
            }

            PortfolioUpdateBroadcaster.broadcast(context);
        }

    }

    @Override
    public void dailyUpdate(Account account, List<Investment> investments,
                            Map<String, Quote> quoteMap) {
        Set<String> currentSymbols = new HashSet<>(Arrays.asList(SYMBOLS));
        if ((investments != null) && (investments.size() > 0)) {
            for (Investment investment : investments) {
                currentSymbols.remove(investment.getSymbol());
            }
        }

        if (currentSymbols.size() > 0) {
            executeStrategy(new ArrayList<>(currentSymbols), account);
        }
    }

}

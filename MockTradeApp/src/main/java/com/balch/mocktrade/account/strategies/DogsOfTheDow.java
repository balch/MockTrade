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

import com.balch.android.app.framework.model.RequestListener;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.MainActivity;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.order.Order;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class DogsOfTheDow extends BaseStrategy {
    private static final String TAG = DogsOfTheDow.class.getSimpleName();

    protected static final String[] DOW_SYMBOLS=
            {"AXP","BA","CAT","CSCO","CVX","DD","XOM","GE","GS","HD",
            "IBM","INTC","JNJ","KO","JPM","MCD","MMM","MRK","MSFT","NKE",
            "PFE","PG","T","TRV","UNH","UTX","VZ","V","WMT","DIS"};

    public void initialize(final Account account) {
        this.financeModel.getQuotes(Arrays.asList(DOW_SYMBOLS), new RequestListener<Map<String, Quote>>() {
            @Override
            public void onResponse(Map<String, Quote> response) {
                SortedMap<Money, Quote> sortedQuoteMap = new TreeMap<>();
                for (Quote quote : response.values()) {
                    sortedQuoteMap.put(quote.getDividendPerShare(), quote);
                }

                if (sortedQuoteMap.size() > 0) {
                    Object[] sortedQuotes = sortedQuoteMap.values().toArray();
                    int numberOfStocks = Math.min(sortedQuotes.length, 10);
                    double fundsPerOrder = account.getAvailableFunds().getDollars() / (double) numberOfStocks;

                    for (int x = sortedQuotes.length - numberOfStocks; x < sortedQuotes.length; x++) {
                        Quote quote = (Quote) sortedQuotes[x];
                        Order order = new Order();
                        order.setAccount(account);
                        order.setSymbol(quote.getSymbol());
                        order.setAction(Order.OrderAction.BUY);
                        order.setStrategy(Order.OrderStrategy.MANUAL);
                        order.setLimitPrice(quote.getPrice());
                        order.setQuantity((long) (fundsPerOrder / quote.getPrice().getDollars()));

                        portfolioModel.createOrder(order);
                        try {
                            portfolioModel.attemptExecuteOrder(order, quote);
                        } catch (Exception e) {
                            Log.e(TAG, "Error executing order", e);
                        }
                    }

                    MainActivity.updateView(context);
                }
            }

            @Override
            public void onErrorResponse(String error) {

            }
        });
    }

    @Override
    public void dailyUpdate(Account account, List<Investment> investments,
                            Map<String, Quote> quoteMap) {
        if ((investments != null) && (investments.size() > 0)) {
            Calendar createDate = new GregorianCalendar();
            createDate.setTime(investments.get(0).getCreateTime());

            Calendar now = new GregorianCalendar();
            if (createDate.get(Calendar.YEAR) < now.get(Calendar.YEAR)) {
                Account updatedAccount = sellAll(account, investments, quoteMap);
                initialize(updatedAccount);
            }
        }
    }

    protected Account sellAll(Account account, List<Investment> investments,
                           Map<String, Quote> quoteMap) {
        for (Investment i : investments) {
            Quote quote = quoteMap.get(i.getSymbol());
            Order order = new Order();
            order.setAccount(account);
            order.setSymbol(quote.getSymbol());
            order.setAction(Order.OrderAction.SELL);
            order.setStrategy(Order.OrderStrategy.MANUAL);
            order.setLimitPrice(quote.getPrice());
            order.setQuantity(i.getQuantity());

            portfolioModel.createOrder(order);
            try {
                portfolioModel.attemptExecuteOrder(order, quote);
            } catch (Exception e) {
                Log.e(TAG, "Error executing order", e);
            }
        }

        // requery the account to get the latest values
        return portfolioModel.getAccount(account.getId());
    }

}

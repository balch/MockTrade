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
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.balch.android.app.framework.model.RequestListener;
import com.balch.mocktrade.TradeApplication;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.strategies.BaseStrategy;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioPresenter;
import com.balch.mocktrade.receivers.QuoteReceiver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuoteService extends IntentService {
    private static final String TAG = QuoteService.class.getName();

    protected static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("MM/dd/yyyy");
    protected static String lastUpdateDate = "";  // move this to shared prefs?

    public QuoteService() {
        super(QuoteService.class.getName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        try {
            Log.i(TAG, "QuoteService onHandleIntent");

            TradeApplication application = (TradeApplication)this.getApplication();
            FinanceModel financeModel = application.getModelFactory().getModel(FinanceModel.class);
            final PortfolioModel portfolioModel = application.getModelFactory().getModel(PortfolioModel.class);
            final List<Investment> investments = portfolioModel.getAllInvestments();

            if (investments.size() > 0) {
                final List<Account> accounts = portfolioModel.getAllAccounts();
                Map<Long, Account> accountMap = new HashMap<Long, Account>(accounts.size());
                for (Account a : accounts ) {
                    accountMap.put(a.getId(), a);
                }

                final Map<Long, List<Investment>> accountIdToInvestmentMap = new HashMap<Long, List<Investment>>(accounts.size());
                List<String> symbols = new ArrayList<String>(investments.size());
                for (Investment i : investments) {
                    symbols.add(i.getSymbol());

                    List<Investment> list = accountIdToInvestmentMap.get(i.getAccount().getId());
                    if (list == null) {
                        list = new ArrayList<Investment>();
                        accountIdToInvestmentMap.put(i.getAccount().getId(), list);
                    }
                    list.add(i);
                }

                financeModel.getQuotes(symbols, new RequestListener<Map<String, Quote>>() {
                    @Override
                    public void onResponse(Map<String, Quote> quoteMap) {
                        try {
                            for (Investment i : investments) {
                                try {
                                    Quote quote = quoteMap.get(i.getSymbol());
                                    if (quote != null) {
                                        i.setPrevDayClose(quote.getPreviousClose());
                                        i.setPrice(quote.getPrice());
                                        portfolioModel.updateInvestment(i);
                                    }
                                } catch (Exception ex) {
                                    Log.e(TAG, "updateInvestment exception", ex);
                                }
                            }

                            processAccountStrategies(accounts, accountIdToInvestmentMap, quoteMap);

                            PortfolioPresenter.updateView(QuoteService.this);
                        } finally {
                            QuoteReceiver.completeWakefulIntent(intent);
                        }
                    }

                    @Override
                    public void onErrorResponse(String error) {
                        // failed to return quotes
                        // Error has been logged
                        try {

                        } finally {
                            QuoteReceiver.completeWakefulIntent(intent);
                        }
                    }
                });
            }

            // if the market is closed reset alarm to next market open time
            if (!financeModel.isInPollTime()) {
                financeModel.setQuoteServiceAlarm();
            }

        } catch (Exception ex) {
            Log.e(TAG, "onHandleIntent exception", ex);
            QuoteReceiver.completeWakefulIntent(intent);
        }
    }

    public static Intent getIntent(Context context) {
        return new Intent(context, QuoteService.class);
    }

    protected void processAccountStrategies(List<Account> accounts,
                                            Map<Long, List<Investment>> accountIdToInvestmentMap,
                                            Map<String, Quote> quoteMap) {
        boolean doDailyUpdate = false;
        String today = DATE_FORMATTER.format(new Date());
        if (!today.equals(lastUpdateDate)) {
            lastUpdateDate = today;
            doDailyUpdate = true;
        }

        for (Account account : accounts) {
            Class<? extends BaseStrategy> strategyClazz = account.getStrategy().getStrategyClazz();
            if (strategyClazz != null) {
                try {
                    TradeApplication application = (TradeApplication)this.getApplication();
                    BaseStrategy strategy = BaseStrategy.createStrategy(strategyClazz, this, application.getModelFactory());
                    if (doDailyUpdate) {
                        strategy.dailyUpdate(account, accountIdToInvestmentMap.get(account.getId()), quoteMap);
                    }
                    strategy.pollUpdate(account, accountIdToInvestmentMap.get(account.getId()), quoteMap);
                } catch (Exception ex) {
                    Log.e(TAG, "Error calling strategy.pollUpdate", ex);
                }
            }
        }
    }
}
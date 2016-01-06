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
import android.os.PowerManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.balch.android.app.framework.model.ModelFactory;
import com.balch.android.app.framework.model.RequestListener;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.strategies.BaseStrategy;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioUpdateBroadcaster;
import com.balch.mocktrade.settings.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuoteService extends IntentService {
    private static final String TAG = QuoteService.class.getSimpleName();

    public static final int SNAPSHOT_DAYS_TO_KEEP = 120;
    public static final String WAKE_LOCK_TAG = "QuoteServiceWakeLockTag";

    private Settings mSettings;

    public QuoteService() {
        super(QuoteService.class.getName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        boolean completeWakefulIntentOnExit = false;

        try {
            Log.i(TAG, "QuoteService onHandleIntent");

            // get the investment list from the db
            ModelFactory modelFactory = ((ModelProvider)this.getApplication()).getModelFactory();
            FinanceModel financeModel = modelFactory.getModel(FinanceModel.class);
            final PortfolioModel portfolioModel = modelFactory.getModel(PortfolioModel.class);
            final List<Investment> investments = portfolioModel.getAllInvestments();
            mSettings = ((ModelProvider)this.getApplication()).getSettings();

            if (investments.size() > 0) {
                final List<Account> accounts = portfolioModel.getAccounts(true);

                final Map<Long, List<Investment>> accountIdToInvestmentMap = new HashMap<>(accounts.size());
                List<String> symbols = new ArrayList<>(investments.size());
                for (Investment i : investments) {
                    symbols.add(i.getSymbol());

                    // aggregate investments by account
                    List<Investment> list = accountIdToInvestmentMap.get(i.getAccount().getId());
                    if (list == null) {
                        list = new ArrayList<>();
                        accountIdToInvestmentMap.put(i.getAccount().getId(), list);
                    }
                    list.add(i);
                }

                // get quotes over the wire
                financeModel.getQuotes(symbols, new RequestListener<Map<String, Quote>>() {
                    @Override
                    public void onResponse(Map<String, Quote> quoteMap) {
                        try {
                            for (Investment i : investments) {
                                try {
                                    Quote quote = quoteMap.get(i.getSymbol());
                                    if (quote != null) {
                                        i.setPrevDayClose(quote.getPreviousClose());
                                        i.setPrice(quote.getPrice(), quote.getLastTradeTime());
                                        portfolioModel.updateInvestment(i);
                                    }
                                } catch (Exception ex) {
                                    Log.e(TAG, "updateInvestment exception", ex);
                                }
                            }

                            boolean isFirstSyncOfDay = !DateUtils.isToday(mSettings.getLastSyncTime());
                            if (isFirstSyncOfDay) {
                                portfolioModel.purgeSnapshots(SNAPSHOT_DAYS_TO_KEEP);
                            }

                            portfolioModel.createSnapshotTotals(accounts, accountIdToInvestmentMap);

                            processAccountStrategies(accounts, accountIdToInvestmentMap, quoteMap, isFirstSyncOfDay);

                            mSettings.setLastSyncTime(System.currentTimeMillis());
                        } finally {
                            PortfolioUpdateBroadcaster.broadcast(QuoteService.this);
                            releaseWakeLock();
                        }
                    }

                    @Override
                    public void onErrorResponse(String error) {
                        // failed to return quotes
                        // Error has been logged
                        PortfolioUpdateBroadcaster.broadcast(QuoteService.this);

                        releaseWakeLock();
                    }
                });
            } else {
                completeWakefulIntentOnExit = true;
                PortfolioUpdateBroadcaster.broadcast(QuoteService.this);
            }

            // if the market is closed reset alarm to next market open time
            if (!financeModel.isInPollTime()) {
                financeModel.setQuoteServiceAlarm();
            }

        } catch (Exception ex) {
            completeWakefulIntentOnExit = true;
            Log.e(TAG, "onHandleIntent exception", ex);
        } finally {
            if (completeWakefulIntentOnExit) {
                releaseWakeLock();
            }
        }
    }

    public static Intent getIntent(Context context) {
        return new Intent(context, QuoteService.class);
    }

    protected void processAccountStrategies(List<Account> accounts,
                                            Map<Long, List<Investment>> accountIdToInvestmentMap,
                                            Map<String, Quote> quoteMap, boolean doDailyUpdate) {
        for (Account account : accounts) {
            Class<? extends BaseStrategy> strategyClazz = account.getStrategy().getStrategyClazz();
            if (strategyClazz != null) {
                try {
                    ModelFactory modelFactory = ((ModelProvider)this.getApplication()).getModelFactory();
                    BaseStrategy strategy = BaseStrategy.createStrategy(strategyClazz, this, modelFactory);
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

    private void releaseWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
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
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LongSparseArray;

import com.balch.mocktrade.TradeModelProvider;
import com.balch.mocktrade.TradeApplication;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.strategies.BaseStrategy;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioSqliteModel;
import com.balch.mocktrade.portfolio.PortfolioUpdateBroadcaster;
import com.balch.mocktrade.receivers.QuoteReceiver;
import com.balch.mocktrade.settings.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuoteService extends IntentService {
    private static final String TAG = QuoteService.class.getSimpleName();

    public static final int SNAPSHOT_DAYS_TO_KEEP = 3650;

    public QuoteService() {
        super(QuoteService.class.getName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        try {
            Log.i(TAG, "QuoteService onHandleIntent");

            // get the investment list from the db
            TradeModelProvider modelProvider = ((TradeModelProvider) this.getApplication());
            FinanceModel financeModel = modelProvider.getFinanceModel();
            final PortfolioModel portfolioModel = new PortfolioSqliteModel(modelProvider.getContext(),
                    modelProvider.getSqlConnection(),
                    financeModel,
                    modelProvider.getSettings());
            final List<Investment> investments = portfolioModel.getAllInvestments();
            Settings settings = ((TradeModelProvider) this.getApplication()).getSettings();

            if (investments.size() > 0) {
                final List<Account> accounts = portfolioModel.getAccounts(true);

                final LongSparseArray<List<Investment>> accountIdToInvestmentMap = new LongSparseArray<>(accounts.size());
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
                try {
                    Map<String, Quote> quoteMap = financeModel.getQuotes(symbols).blockingFirst();
                    if (quoteMap != null) {
                        boolean newHasQuotes = false;
                        for (Investment i : investments) {
                            try {
                                Quote quote = quoteMap.get(i.getSymbol());
                                if (quote != null) {
                                    if (quote.getLastTradeTime().after(i.getLastTradeTime())) {
                                        newHasQuotes = true;
                                        i.setPrevDayClose(quote.getPreviousClose());
                                        i.setPrice(quote.getPrice(), quote.getLastTradeTime());
                                        portfolioModel.updateInvestment(i);
                                    }
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "updateInvestment exception", ex);
                            }
                        }

                        boolean isFirstSyncOfDay = !DateUtils.isToday(settings.getLastSyncTime());
                        if (isFirstSyncOfDay) {
                            portfolioModel.purgeSnapshots(SNAPSHOT_DAYS_TO_KEEP);
                        }

                        if (newHasQuotes) {
                            portfolioModel.createSnapshotTotals(accounts, accountIdToInvestmentMap);
                        }

                        processAccountStrategies(accounts, accountIdToInvestmentMap, quoteMap, isFirstSyncOfDay);

                        settings.setLastSyncTime(System.currentTimeMillis());

                        startService(WearSyncService.getIntent(getApplicationContext()));
                    }
                } catch (Exception ex1) {
                    Log.e(TAG, "updateInvestment exception", ex1);

                }
            }

            // if the market is closed reset alarm to next market open time
            if (!financeModel.isInPollTime()) {
                TradeApplication.backupDatabase(getApplicationContext(), true);
                financeModel.setQuoteServiceAlarm();
            }

        } catch (Exception ex) {
            Log.e(TAG, "financeModel.getQuotes(symbols exception", ex);
        } finally {
            PortfolioUpdateBroadcaster.broadcast(QuoteService.this);
            QuoteReceiver.completeWakefulIntent(intent);
        }
    }

    public static Intent getIntent(Context context) {
        return new Intent(context, QuoteService.class);
    }

    protected void processAccountStrategies(List<Account> accounts,
                                            LongSparseArray<List<Investment>> accountIdToInvestmentMap,
                                            Map<String, Quote> quoteMap, boolean doDailyUpdate) {
        for (Account account : accounts) {
            Class<? extends BaseStrategy> strategyClazz = account.getStrategy().getStrategyClazz();
            if (strategyClazz != null) {
                try {
                    TradeModelProvider modelProvider = ((TradeModelProvider)this.getApplication());
                    BaseStrategy strategy = BaseStrategy.createStrategy(strategyClazz,
                            modelProvider.getContext(),
                            modelProvider.getFinanceModel(),
                            modelProvider.getSqlConnection(),
                            modelProvider.getSettings());
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
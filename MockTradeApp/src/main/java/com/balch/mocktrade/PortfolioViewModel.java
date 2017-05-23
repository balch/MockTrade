/*
 * Author: Balch
 * Created: 5/20/17 11:18 PM
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

package com.balch.mocktrade;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.portfolio.PortfolioData;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioUpdateBroadcaster;
import com.balch.mocktrade.settings.Settings;
import com.balch.mocktrade.shared.PerformanceItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PortfolioViewModel extends ViewModel {
    private static final String TAG = PortfolioViewModel.class.getSimpleName();

    private PortfolioModel portfolioModel;
    private Settings appSettings;

    private long graphSelectedAccountId = -1;
    private int graphDaysToReturn = -1;

    private final MutableLiveData<List<PerformanceItem>> liveGraphData = new MutableLiveData<>();
    private final MutableLiveData<PortfolioData> livePortfolioData = new MutableLiveData<>();

    private LocalBroadcastManager localBroadcastManager = null;
    private Disposable disposableGraphData = null;
    private Disposable disposablePortfolioData = null;
    private UpdateReceiver updateReceiver;

    boolean isInitialized() {
        return (portfolioModel != null);
    }

    PortfolioModel getPortfolioModel() {
        return portfolioModel;
    }

    void setPortfolioModel(PortfolioModel portfolioModel) {
        this.portfolioModel = portfolioModel;
    }

    public void setAppSettings(Settings appSettings) {
        this.appSettings = appSettings;
    }

    @Override
    protected void onCleared() {
        disposeGraphData();
        disposePortfolioData();
        if (localBroadcastManager != null) {
            localBroadcastManager.unregisterReceiver(updateReceiver);
            updateReceiver = null;
            localBroadcastManager = null;
        }
    }

    LiveData<List<PerformanceItem>> getGraphData(Context context) {
        setUpdateReceiver(context);
        return liveGraphData;
    }

    LiveData<PortfolioData> getPortfolioData(Context context) {
        setUpdateReceiver(context);
        return livePortfolioData;
    }

    void loadPortfolioData() {
        disposePortfolioData();
        disposablePortfolioData = Observable.just(true)
                .subscribeOn(Schedulers.io())
                .map(aBoolean -> {
                    PortfolioData portfolioData = loadPortfolioDataInBackground();
                    if (portfolioData == null) {
                        portfolioData = new PortfolioData();
                    }
                    return portfolioData;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(livePortfolioData::setValue,
                        throwable -> Log.e(TAG, "loadPortfolioData exception", throwable ));
    }

    void loadGraphData() {
        disposeGraphData();
        disposableGraphData = Observable.just(true)
                .subscribeOn(Schedulers.io())
                .map(aBoolean -> {
                    List<PerformanceItem> performanceItems =
                            (graphDaysToReturn < 2) ?
                                portfolioModel.getCurrentSnapshot(graphSelectedAccountId) :
                                portfolioModel.getCurrentDailySnapshot(graphSelectedAccountId, graphDaysToReturn);

                    if (performanceItems == null) {
                        performanceItems = new ArrayList<>();
                    }
                    return performanceItems;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(liveGraphData::setValue,
                        throwable -> Log.e(TAG, "loadGraphData exception", throwable ));

    }

    void setGraphSelectionCriteria(long accountID, int days) {
        graphSelectedAccountId = accountID;
        graphDaysToReturn = days;
        loadGraphData();
    }

    private PortfolioData loadPortfolioDataInBackground() {

        PortfolioData portfolioData = new PortfolioData();

        List<Account> accounts = portfolioModel.getAccounts(!appSettings.getBoolean(Settings.Key.PREF_HIDE_EXCLUDE_ACCOUNTS));

        portfolioData.addAccounts(accounts);
        portfolioData.addInvestments(portfolioModel.getAllInvestments());
        portfolioData.setLastSyncTime(new Date(appSettings.getLastSyncTime()));
        portfolioData.setLastQuoteTime(portfolioModel.getLastQuoteTime());

        List<Order> openOrders = portfolioModel.getOpenOrders();
        for (Order o : openOrders) {
            portfolioData.addToOpenOrderCount(o.getAccount().getId());
        }

        // populate investment account object
        // hopefully one day investment.account will be set in the model
        for (Account a : portfolioData.getAccounts()) {
            List<Investment> investments = portfolioData.getInvestments(a.getId());
            if (investments != null) {
                for (Investment i : investments) {
                    i.setAccount(a);
                }
            }
        }
        return portfolioData;
    }

    private void disposeGraphData() {
        if (disposableGraphData != null) {
            disposableGraphData.dispose();
            disposableGraphData = null;
        }
    }

    private void disposePortfolioData() {
        if (disposablePortfolioData != null) {
            disposablePortfolioData.dispose();
            disposablePortfolioData = null;
        }
    }

    private void setUpdateReceiver(Context context) {
        if (localBroadcastManager == null) {
            updateReceiver = new UpdateReceiver();
            localBroadcastManager = LocalBroadcastManager.getInstance(context);
            localBroadcastManager.registerReceiver(updateReceiver, new IntentFilter(PortfolioUpdateBroadcaster.ACTION));
        }
    }

    private class UpdateReceiver extends BroadcastReceiver {
        private UpdateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            loadGraphData();
            loadPortfolioData();
        }
    }
}



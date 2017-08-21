/*
 * Author: Balch
 * Created: 8/20/17 3:00 PM
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

import android.Manifest;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.balch.android.app.framework.BasePresenter;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.finance.GoogleFinanceApi;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.portfolio.AccountViewHolder;
import com.balch.mocktrade.portfolio.PortfolioAdapter;
import com.balch.mocktrade.portfolio.PortfolioData;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioSqliteModel;
import com.balch.mocktrade.settings.Settings;
import com.balch.mocktrade.shared.PerformanceItem;

import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainPresenter extends BasePresenter<MainPortfolioView> {
    private static final String TAG = MainPresenter.class.getSimpleName();

    public interface ActivityBridge {
        void showProgress(boolean show);
        void showSnackBar(View parent, String displayMsg, @ColorRes int colorId);
        void requestStoragePermission(int requestCode);

        void startNewAccountActivity();
        void startOrderListActivity(Account account);
        void startNewOrderActivity(Order order, @StringRes int stringId);
    }

    private Settings appSetting;

    private PortfolioModel portfolioModel;

    private PortfolioViewModel portfolioViewModel;
    LiveData<List<PerformanceItem>> performanceItemLiveData;
    LiveData<PortfolioData> portfolioLiveData;

    private PortfolioAdapter portfolioAdapter;

    private Disposable disposableNewAccount = null;
    private ViewProvider viewProvider;
    private ActivityBridge listener;

    public MainPresenter(TradeModelProvider modelProvider, ViewProvider viewProvider,
                         PortfolioViewModel portfolioViewModel,
                         LifecycleOwner lifecycleOwner, MainPortfolioView view,
                         ActivityBridge listener) {
        super(view);

        this.listener = listener;
        this.viewProvider = viewProvider;
        this.portfolioViewModel = portfolioViewModel;
        appSetting = modelProvider.getSettings();
        if (!portfolioViewModel.isInitialized()) {
            portfolioModel = new PortfolioSqliteModel(modelProvider.getContext(),
                    modelProvider.getSqlConnection(),
                    modelProvider.getModelApiFactory().getModelApi(GoogleFinanceApi.class),
                    modelProvider.getSettings());
            portfolioViewModel.setPortfolioModel(portfolioModel);
            portfolioViewModel.setAppSettings(appSetting);
        } else {
            portfolioModel = portfolioViewModel.getPortfolioModel();
        }

        final Context context = modelProvider.getContext();
        performanceItemLiveData = portfolioViewModel.getGraphData(context);
        performanceItemLiveData.observe(lifecycleOwner, graphDataObserver);

        portfolioLiveData = portfolioViewModel.getPortfolioData(context);
        portfolioLiveData.observe(lifecycleOwner, portfolioDataObserver);

        setupAdapter(context);

    }

    private Observer<List<PerformanceItem>> graphDataObserver = data -> {
        view.setDailyGraphData(data);
//          mMainPortfolioView.setDailyGraphData(generateRandomTestData());

        listener.showProgress(false);
    };

    private Observer<PortfolioData> portfolioDataObserver = data -> displayPortfolioData(data);

    private void displayPortfolioData(PortfolioData data) {
        PerformanceItem performanceItem = new PerformanceItem(-1, new Date(), new Money(), new Money(), new Money());

        int accountsWithTotals = 0;

        Date timestamp = new Date();
        boolean demoMode = appSetting.getBoolean(Settings.Key.PREF_DEMO_MODE);
        for (Account account : data.getAccounts()) {
            if (demoMode || !account.getExcludeFromTotals()) {
                List<Investment> investments = data.getInvestments(account.getId());
                performanceItem.aggregate(account.getPerformanceItem(investments, timestamp));

                accountsWithTotals++;
            }
        }

        view.setSyncTimes(data.getLastSyncTime(), data.getLastQuoteTime());

        boolean showTotals = (!viewProvider.isLandscape(view.getContext()) || viewProvider.isTablet(view.getContext())) &&
                (accountsWithTotals > 1);
        view.setTotals(showTotals, performanceItem);

        portfolioAdapter.bind(data);

        view.setDailyGraphDataAccounts(data.getAccounts());

        listener.showProgress(false);
    }


    public boolean getHideExcludeAccounts() {
        return appSetting.getBoolean(Settings.Key.PREF_HIDE_EXCLUDE_ACCOUNTS);
    }

    public void setHideExcludeAccounts(boolean enabled) {
        appSetting.setBoolean(Settings.Key.PREF_HIDE_EXCLUDE_ACCOUNTS, enabled);
        view.resetSelectedAccountID();
        updateView();
    }

    public boolean getDemoMode() {
        return appSetting.getBoolean(Settings.Key.PREF_DEMO_MODE);
    }

    public void setDemoMode(boolean enabled) {
        appSetting.setBoolean(Settings.Key.PREF_DEMO_MODE, enabled);
        updateView();
    }

    public void updateAccount(long accountId, int days) {
        portfolioViewModel.setGraphSelectionCriteria(accountId, days);
        view.setAccountSpinner(accountId);
    }

    public void updateView() {
        portfolioViewModel.loadGraphData();
        portfolioViewModel.loadPortfolioData();
    }

    private void createNewAccountAsync(final Account account) {
        listener.showProgress(true);

        disposeNewAccount();
        disposableNewAccount = Observable.just(true)
                .subscribeOn(Schedulers.io())
                .map(aBoolean -> {
                    portfolioModel.createAccount(account);
                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                            listener.showProgress(false);
                            updateView();
                        },
                        throwable -> {
                            listener.showProgress(false);
                            Log.e(TAG, "createNewAccountAsync error", throwable);
                        });
    }

    private void disposeNewAccount() {
        if (disposableNewAccount != null) {
            disposableNewAccount.dispose();
            disposableNewAccount = null;
        }
    }

    protected void setupAdapter(final Context context) {

        portfolioAdapter = new PortfolioAdapter(appSetting, viewProvider);
        portfolioAdapter.setListener(new PortfolioAdapter.PortfolioAdapterListener() {
            @Override
            public boolean onLongClickAccount(final Account account) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.account_delete_dlg_title)
                        .setMessage(String.format(context.getString(R.string.account_delete_dlg_message_format), account.getName()))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            try {
                                portfolioModel.deleteAccount(account);
                                updateView();
                            } catch (Exception ex) {
                                Log.e(TAG, "Error Deleting account", ex);
                                Toast.makeText(context, "Error deleting account", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            @Override
            public boolean onLongClickInvestment(Investment investment) {
                showNewSellOrderActivity(investment);
                return true;
            }

            @Override
            public void createNewAccount() {
                listener.startNewAccountActivity();
            }

            @Override
            public void createNewDogsAccount() {
                String name = view.getResources().getString(R.string.quickstart_account_name);
                String desc = view.getResources().getString(R.string.quickstart_account_desc);

                Account newAccount = new Account(name, desc,
                        new Money(100000.0), Account.Strategy.DOGS_OF_THE_DOW, false);
                createNewAccountAsync(newAccount);
            }
        });

        portfolioAdapter.setAccountItemViewListener(new AccountViewHolder.AccountItemViewListener() {
            @Override
            public void onTradeButtonClicked(Account account) {
                showNewBuyOrderActivity(account);
            }

            @Override
            public void onShowOpenOrdersClicked(Account account) {
                listener.startOrderListActivity(account);
            }
        });
        this.view.setPortfolioAdapter(portfolioAdapter);
    }

    public boolean handleException(String logMsg, Exception ex) {
        String displayMsg = ex.getLocalizedMessage();
        if (TextUtils.isEmpty(displayMsg)) {
            displayMsg = ex.toString();
        }

        listener.showSnackBar(view, displayMsg, R.color.failure);
        return true;
    }


    public void backupDatabaseToSDCard(Context context, int requestCode) {
        if (isStoragePermissionsGranted(context, requestCode)) {

            boolean success = TradeApplication.backupDatabase(context, false);
            String msg = view.getResources().getString(success ? R.string.menu_backup_db_success : R.string.menu_backup_db_fail);

            listener.showSnackBar(view, msg, success ? R.color.success : R.color.failure);
        }
    }

    public void restoreLatestDatabase(Context context, int requestCode) {
        if (isStoragePermissionsGranted(context, requestCode)) {
            boolean success = TradeApplication.restoreDatabase(context);

            String msg = view.getResources().getString(success ? R.string.menu_restore_db_success : R.string.menu_restore_db_fail);
            listener.showSnackBar(view, msg, success ? R.color.success : R.color.failure);
        }
    }

    public void createNewOrder(Order order) {
        if (order != null) {
            portfolioModel.createOrder(order);
            updateView();

            portfolioModel.processOrders(view.getContext(),
                    (order.getStrategy() == Order.OrderStrategy.MANUAL));
        }
    }

    public void createNewAccount(Account account) {
        if (account != null) {
            Account newAccount = new Account(account.getName(), account.getDescription(),
                    account.getInitialBalance(), account.getStrategy(), account.getExcludeFromTotals());
            createNewAccountAsync(newAccount);
        }
    }

    private boolean isStoragePermissionsGranted(Context context, int requestCode) {
        boolean hasPerms = true;
        if ((ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {

            listener.requestStoragePermission(requestCode);
            hasPerms = false;
        }

        return hasPerms;
    }

    protected void showNewBuyOrderActivity(Account account) {
        Order order = new Order();
        order.setAccount(account);
        order.setAction(Order.OrderAction.BUY);
        order.setStrategy(Order.OrderStrategy.MARKET);

        listener.startNewOrderActivity(order, R.string.order_create_buy_title);
    }

    protected void showNewSellOrderActivity(Investment investment) {
        Order order = new Order();
        order.setSymbol(investment.getSymbol());
        order.setQuantity(investment.getQuantity());
        order.setAccount(investment.getAccount());
        order.setAction(Order.OrderAction.SELL);
        order.setStrategy(Order.OrderStrategy.MARKET);

        listener.startNewOrderActivity(order, R.string.order_create_sell_title);

    }

    @Override
    protected void cleanup() {

        performanceItemLiveData.removeObserver(graphDataObserver);
        portfolioLiveData.removeObserver(portfolioDataObserver);
        disposeNewAccount();

        appSetting = null;
        portfolioModel = null;
        portfolioViewModel = null;
        viewProvider = null;
        listener = null;

        super.cleanup();
    }
}

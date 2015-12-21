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

package com.balch.mocktrade.portfolio;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.balch.android.app.framework.BasePresenter;
import com.balch.android.app.framework.domain.EditActivity;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.TradeApplication;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountEditController;
import com.balch.mocktrade.account.AccountItemView;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.order.OrderEditController;
import com.balch.mocktrade.order.OrderListFragment;
import com.balch.mocktrade.services.QuoteService;

import java.util.List;

public class PortfolioPresenter extends BasePresenter<TradeApplication> implements LoaderManager.LoaderCallbacks<PortfolioData> {
    private static final String TAG = PortfolioPresenter.class.getSimpleName();

    public interface PortfolioPresenterListener {
        void onStartActivityForResult(Intent intent, int resultCode);
        void showView(Fragment fragment);
        void showProgress();
        void hideProgress();
        ComponentName onStartService(Intent intent);
        Context getContext();
    }

    protected static final int ACCOUNT_LOADER_ID = 0;

    protected static final int NEW_ACCOUNT_RESULT = 0;
    protected static final int NEW_ORDER_RESULT = 1;

    final protected PortfolioModel model;
    final protected PortfolioView view;
    final protected PortfolioPresenterListener portfolioPresenterListener;

    protected PortfolioAdapter portfolioAdapter;
    protected QuoteUpdateReceiver quoteUpdateReceiver;

    public PortfolioPresenter(PortfolioModel model, PortfolioView view, PortfolioPresenterListener portfolioPresenterListener) {
        this.model = model;
        this.view = view;
        this.portfolioPresenterListener = portfolioPresenterListener;
    }

    @Override
    public void initialize(Bundle savedInstanceState) {
        this.view.setPortfolioViewListener(new PortfolioView.PortfolioViewListener() {
            @Override
            public void onCreateNewAccount() {
                showNewAccountActivity();
            }
        });
        this.quoteUpdateReceiver = new QuoteUpdateReceiver(this);

        setupAdapter();
        reload(true);
    }

    @Override
    public void onResume() {
        this.application.registerReceiver(quoteUpdateReceiver, new IntentFilter(QuoteUpdateReceiver.getAction()));
    }

    @Override
    public void onPause() {
        this.application.unregisterReceiver(quoteUpdateReceiver);
    }

    protected void setupAdapter() {
        this.portfolioAdapter = new PortfolioAdapter(this.application);
        this.portfolioAdapter.setListener(new PortfolioAdapter.PortfolioAdapterListener() {
            @Override
            public boolean onLongClickAccount(final Account account) {
                new AlertDialog.Builder(portfolioPresenterListener.getContext())
                        .setTitle(R.string.account_delete_dlg_title)
                        .setMessage(String.format(getString(R.string.account_delete_dlg_message_format), account.getName()))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    model.deleteAccount(account);
                                    reload(true);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error Deleting account", ex);
                                    Toast.makeText(application, "Error deleting account", Toast.LENGTH_LONG).show();
                                }
                            }})
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            @Override
            public boolean onLongClickInvestment(Investment investment) {
                showNewSellOrderActivity(investment);
                return true;
            }
        });

        this.portfolioAdapter.setAccountItemViewListener(new AccountItemView.AccountItemViewListener() {
            @Override
            public void onTradeButtonClicked(Account account) {
                showNewBuyOrderActivity(account);
            }

            @Override
            public void onShowOpenOrdersClicked(Account account) {
                portfolioPresenterListener.showView(new OrderListFragment().setCustomArguments(account.getId()));
            }
        });
        this.view.setPortfolioAdapter(this.portfolioAdapter);
    }

    /**
     * Reload update the UI from the data in the database
     */
    private void reload(boolean showProgress) {
        if (showProgress) {
            portfolioPresenterListener.showProgress();
        }
        this.loaderManager.initLoader(ACCOUNT_LOADER_ID, null, this).forceLoad();
    }

    /**
     * Refresh launches the quote service which will call the QuoteUpdateReceiver
     * to update the UI once the quotes are fetched
     */
    public void refresh() {
        portfolioPresenterListener.showProgress();
        portfolioPresenterListener.onStartService(QuoteService.getIntent(this.application));
    }

    static public void updateView(Context context) {
        context.sendBroadcast(QuoteUpdateReceiver.getIntent());
    }

    @Override
    public Loader<PortfolioData> onCreateLoader(int id, Bundle args) {
        return new PortfolioLoader(this.application, this.model);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<PortfolioData> loader, PortfolioData data) {
        PerformanceItem performanceItem = new PerformanceItem(new Money(), new Money(), new Money());

        int accountsWithTotals = 0;
        Account totals = new Account(this.getString(R.string.account_totals_label), "", new Money(0), Account.Strategy.NONE, false);

        for (Account account : data.getAccounts()) {
            if (!account.getExcludeFromTotals()) {
                totals.aggregate(account);
                List<Investment> investments = data.getInvestments(account.getId());
                performanceItem.aggregate(account.getPerformanceItem(investments));

                accountsWithTotals++;
            }
        }

        this.view.setTotals((accountsWithTotals > 1), totals, performanceItem);
        this.portfolioAdapter.bind(data);
        portfolioAdapter.notifyDataSetChanged();

        this.view.post(new Runnable() {
            @Override
            public void run() {
                view.explandList();
                portfolioPresenterListener.hideProgress();
            }
        });

        // hack to prevent onLoadFinished being called twice
        // http://stackoverflow.com/questions/11293441/android-loadercallbacks-onloadfinished-called-twice/22183247
        this.loaderManager.destroyLoader(ACCOUNT_LOADER_ID);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<PortfolioData> loader) {
        this.portfolioAdapter.clear();
    }

    protected static class PortfolioLoader extends AsyncTaskLoader<PortfolioData> {
        protected PortfolioModel model;

        public PortfolioLoader(Context context, PortfolioModel model) {
            super(context);
            this.model = model;
        }

        @Override
        public PortfolioData loadInBackground() {
            PortfolioData portfolioData = new PortfolioData();
            portfolioData.addAccounts(model.getAllAccounts());
            portfolioData.addInvestments(model.getAllInvestments());

            List<Order> openOrders = model.getOpenOrders();
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
    }

    protected void showNewAccountActivity() {
        Intent intent = EditActivity.getIntent(this.view.getContext(), R.string.account_create_title,
                new Account("", "", new Money(100000.0), Account.Strategy.NONE, false),
                new AccountEditController(), 0, 0);

        this.portfolioPresenterListener.onStartActivityForResult(intent, NEW_ACCOUNT_RESULT);
    }

    protected void showNewBuyOrderActivity(Account account) {
        Order order = new Order();
        order.setAccount(account);
        order.setAction(Order.OrderAction.BUY);
        order.setStrategy(Order.OrderStrategy.MARKET);
        Intent intent = EditActivity.getIntent(this.view.getContext(), R.string.order_create_buy_title,
                order, new OrderEditController(), R.string.order_edit_ok_button_new, 0);

        this.portfolioPresenterListener.onStartActivityForResult(intent, NEW_ORDER_RESULT);
    }

    protected void showNewSellOrderActivity(Investment investment) {
        Order order = new Order();
        order.setSymbol(investment.getSymbol());
        order.setQuantity(investment.getQuantity());
        order.setAccount(investment.getAccount());
        order.setAction(Order.OrderAction.SELL);
        order.setStrategy(Order.OrderStrategy.MARKET);

        Intent intent = EditActivity.getIntent(this.view.getContext(),
                R.string.order_create_sell_title,
                order, new OrderEditController(), R.string.order_edit_ok_button_new, 0);

        this.portfolioPresenterListener.onStartActivityForResult(intent, NEW_ORDER_RESULT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == NEW_ACCOUNT_RESULT) {
                Account account = EditActivity.getResult(data);
                if (account != null) {
                    // create a new Account instance to make sure the account is initialized correctly
                    model.createAccount(new Account(account.getName(), account.getDescription(),
                            account.getInitialBalance(), account.getStrategy(), account.getExcludeFromTotals()));
                    reload(true);
                }
            } else if (requestCode == NEW_ORDER_RESULT) {
                Order order = EditActivity.getResult(data);
                if (order != null) {
                    model.createOrder(order);
                    reload(true);

                    model.processOrders(this.application,
                            (order.getStrategy() == Order.OrderStrategy.MANUAL));
                }
            }
        }
    }

    public static class QuoteUpdateReceiver extends BroadcastReceiver {
        protected final PortfolioPresenter portfolioPresenter;

        private QuoteUpdateReceiver(PortfolioPresenter portfolioPresenter) {
            this.portfolioPresenter = portfolioPresenter;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.portfolioPresenter.reload(false);
        }

        private static Intent getIntent() {
            return new Intent(getAction());
        }

        private static String getAction() {
            return QuoteUpdateReceiver.class.getName();
        }
    }
}

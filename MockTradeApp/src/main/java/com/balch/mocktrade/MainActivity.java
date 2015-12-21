package com.balch.mocktrade;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.balch.android.app.framework.BaseAppCompatActivity;
import com.balch.android.app.framework.domain.EditActivity;
import com.balch.android.app.framework.model.ModelFactory;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountEditController;
import com.balch.mocktrade.account.AccountItemView;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.order.OrderEditController;
import com.balch.mocktrade.order.OrderListActivity;
import com.balch.mocktrade.portfolio.PerformanceItem;
import com.balch.mocktrade.portfolio.PortfolioAdapter;
import com.balch.mocktrade.portfolio.PortfolioData;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.services.QuoteService;
import com.balch.mocktrade.settings.SettingsActivity;

import java.util.List;

public class MainActivity extends BaseAppCompatActivity<MainPortfolioView>
            implements LoaderManager.LoaderCallbacks<PortfolioData> {
    private static final String TAG = MainActivity.class.getSimpleName();

    protected static final int ACCOUNT_LOADER_ID = 0;

    protected static final int NEW_ACCOUNT_RESULT = 0;
    protected static final int NEW_ORDER_RESULT = 1;

    protected PortfolioModel model;
    protected MainPortfolioView view;

    private ProgressBar progressBar;
    private ImageView refreshImageButton;

    protected PortfolioAdapter portfolioAdapter;
    protected QuoteUpdateReceiver quoteUpdateReceiver;

    @Override
    protected void onCreateBase(Bundle bundle) {
        ModelFactory modelFactory = ((ModelProvider)this.getApplication()).getModelFactory();
        model = modelFactory.getModel(PortfolioModel.class);

        Toolbar toolbar = (Toolbar) findViewById(R.id.portfolio_view_toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.portfolio_view_settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(SettingsActivity.newIntent(MainActivity.this));
            }
        });

        progressBar = (ProgressBar)findViewById(R.id.portfolio_view_progress_bar);

        refreshImageButton = (ImageView) findViewById(R.id.portfolio_view_refresh_button);
        refreshImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               refresh();
            }
        });

        findViewById(R.id.portfolio_view_new_portfolio_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewAccountActivity();
            }
        });

        this.quoteUpdateReceiver = new QuoteUpdateReceiver();

        setupAdapter();
        reload(true);

    }

    @Override
    protected MainPortfolioView createView() {
        this.view = new MainPortfolioView(this);
        return this.view;
    }

    @Override
    protected void onStartBase() {
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(quoteUpdateReceiver, new IntentFilter(QuoteUpdateReceiver.class.getName()));
    }

    @Override
    protected void onStopBase() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(quoteUpdateReceiver);
    }

    protected void setupAdapter() {
        this.portfolioAdapter = new PortfolioAdapter(this);
        this.portfolioAdapter.setListener(new PortfolioAdapter.PortfolioAdapterListener() {
            @Override
            public boolean onLongClickAccount(final Account account) {
                new AlertDialog.Builder(MainActivity.this)
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
                                    Toast.makeText(MainActivity.this, "Error deleting account", Toast.LENGTH_LONG).show();
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
                startActivity(OrderListActivity.newIntent(MainActivity.this, account.getId()));
            }
        });
        this.view.setPortfolioAdapter(this.portfolioAdapter);
    }

    /**
     * Reload update the UI from the data in the database
     */
    private void reload(boolean showProgress) {
        if (showProgress) {
            showProgress();
        }
        getSupportLoaderManager().initLoader(ACCOUNT_LOADER_ID, null, this).forceLoad();
    }

    /**
     * Refresh launches the quote service which will call the QuoteUpdateReceiver
     * to update the UI once the quotes are fetched
     */
    public void refresh() {
        showProgress();
        startService(QuoteService.getIntent(this));
    }

    static public void updateView(Context context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(QuoteUpdateReceiver.class.getName()));
    }

    @Override
    public Loader<PortfolioData> onCreateLoader(int id, Bundle args) {
        return new PortfolioLoader(this, this.model);
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
                view.expandList();
                hideProgress();
            }
        });

        // hack to prevent onLoadFinished being called twice
        // http://stackoverflow.com/questions/11293441/android-loadercallbacks-onloadfinished-called-twice/22183247
        getSupportLoaderManager().destroyLoader(ACCOUNT_LOADER_ID);
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

        startActivityForResult(intent, NEW_ACCOUNT_RESULT);
    }

    protected void showNewBuyOrderActivity(Account account) {
        Order order = new Order();
        order.setAccount(account);
        order.setAction(Order.OrderAction.BUY);
        order.setStrategy(Order.OrderStrategy.MARKET);
        Intent intent = EditActivity.getIntent(this.view.getContext(), R.string.order_create_buy_title,
                order, new OrderEditController(), R.string.order_edit_ok_button_new, 0);

        startActivityForResult(intent, NEW_ORDER_RESULT);
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

        startActivityForResult(intent, NEW_ORDER_RESULT);
    }

    @Override
    protected void onActivityResultBase(int requestCode, int resultCode, Intent data) {
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

                    model.processOrders(this,
                            (order.getStrategy() == Order.OrderStrategy.MANUAL));
                }
            }
        }
    }

    public void showProgress() {
        this.progressBar.setVisibility(View.VISIBLE);
        this.refreshImageButton.setVisibility(View.GONE);
    }

    public void hideProgress() {
        this.progressBar.setVisibility(View.GONE);
        this.refreshImageButton.setVisibility(View.VISIBLE);
    }


    public class QuoteUpdateReceiver extends BroadcastReceiver {
        private QuoteUpdateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            reload(false);
        }

    }
}


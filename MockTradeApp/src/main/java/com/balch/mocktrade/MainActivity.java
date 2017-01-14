package com.balch.mocktrade;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.balch.android.app.framework.PresenterActivity;
import com.balch.android.app.framework.domain.EditActivity;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountEditController;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.order.OrderEditController;
import com.balch.mocktrade.order.OrderListActivity;
import com.balch.mocktrade.portfolio.AccountViewHolder;
import com.balch.mocktrade.portfolio.GraphDataLoader;
import com.balch.mocktrade.portfolio.PortfolioAdapter;
import com.balch.mocktrade.portfolio.PortfolioData;
import com.balch.mocktrade.portfolio.PortfolioLoader;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioSqliteModel;
import com.balch.mocktrade.services.PerformanceItemUpdateBroadcaster;
import com.balch.mocktrade.services.QuoteService;
import com.balch.mocktrade.settings.Settings;
import com.balch.mocktrade.settings.SettingsActivity;
import com.balch.mocktrade.shared.PerformanceItem;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

public class MainActivity extends PresenterActivity<MainPortfolioView, TradeModelProvider> {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PORTFOLIO_LOADER_ID = 0;
    private static final int GRAPH_LOADER_ID = 1;

    private static final int NEW_ACCOUNT_RESULT = 0;
    private static final int NEW_ORDER_RESULT = 1;

    private static final int PERMS_REQUEST_BACKUP = 0;
    private static final int PERMS_REQUEST_RESTORE = 1;

    private PortfolioModel mPortfolioModel;
    private MainPortfolioView mMainPortfolioView;

    private PortfolioAdapter mPortfolioAdapter;

    private MenuItem mMenuProgressBar;
    private MenuItem mMenuRefreshButton;
    private MenuItem mMenuHideExcludeAccounts;
    private MenuItem mMenuDemoMode;

    private Handler mUIHandler = new Handler(Looper.getMainLooper());
    private Settings mSettings;

    private GraphDataLoader mGraphDataLoader;
    private PortfolioLoader mPortfolioLoader;

    private AccountUpdateReceiver mAccountUpdateReceiver;

    private LoaderManager.LoaderCallbacks<PortfolioData> mPortfolioDataLoaderCallback =
            new LoaderManager.LoaderCallbacks<PortfolioData>() {
                @Override
                public Loader<PortfolioData> onCreateLoader(int id, Bundle args) {
                    return new PortfolioLoader(MainActivity.this, mPortfolioModel, mSettings);
                }

                @Override
                public void onLoadFinished(Loader<PortfolioData> loader, PortfolioData data) {
                    PerformanceItem performanceItem = new PerformanceItem(-1, new Date(), new Money(), new Money(), new Money());

                    int accountsWithTotals = 0;

                    Date timestamp = new Date();
                    boolean demoMode = mSettings.getBoolean(Settings.Key.PREF_DEMO_MODE);
                    for (Account account : data.getAccounts()) {
                        if (demoMode || !account.getExcludeFromTotals()) {
                            List<Investment> investments = data.getInvestments(account.getId());
                            performanceItem.aggregate(account.getPerformanceItem(investments, timestamp));

                            accountsWithTotals++;
                        }
                    }

                    mMainPortfolioView.setSyncTimes(data.getLastSyncTime(), data.getLastQuoteTime());

                    ViewProvider viewProvider = (ViewProvider) getApplication();

                    boolean showTotals = (!viewProvider.isLandscape(MainActivity.this) || viewProvider.isTablet(MainActivity.this)) &&
                            (accountsWithTotals > 1);
                    mMainPortfolioView.setTotals(showTotals, performanceItem);

                    mPortfolioAdapter.bind(data);

                    mMainPortfolioView.setDailyGraphDataAccounts(data.getAccounts());

                    hideProgress();

                }

                @Override
                public void onLoaderReset(Loader<PortfolioData> loader) {
                    mPortfolioAdapter.clear(true);
                }

            };

    private LoaderManager.LoaderCallbacks<List<PerformanceItem>> mGraphDataLoaderCallback =
            new LoaderManager.LoaderCallbacks<List<PerformanceItem>>() {
                @Override
                public Loader<List<PerformanceItem>> onCreateLoader(int id, Bundle args) {
                    return new GraphDataLoader(MainActivity.this, mPortfolioModel);
                }

                @Override
                public void onLoadFinished(Loader<List<PerformanceItem>> loader, List<PerformanceItem> data) {
                    mMainPortfolioView.setDailyGraphData(data);
//                      mMainPortfolioView.setDailyGraphData(generateRandomTestData());

                    hideProgress();

                }

                @Override
                public void onLoaderReset(Loader<List<PerformanceItem>> loader) {
                }

            };

    @Override
    public void onCreateBase(Bundle bundle) {

        ViewProvider viewProvider = (ViewProvider) getApplication();

        if (viewProvider.isTablet(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.portfolio_view_toolbar);
        if (toolbar != null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);

            // tint the overflow icon
            ColorStateList colorSelector = ContextCompat.getColorStateList(this, R.color.nav_on_color);
            Drawable icon = toolbar.getOverflowIcon();
            if (icon != null) {
                DrawableCompat.setTintList(icon, colorSelector);
                toolbar.setOverflowIcon(icon);
            }
        }

        setupAdapter();

        final AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.portfolio_view_app_bar);
        if (appBarLayout != null) {
            if (bundle == null) {
                mUIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        appBarLayout.setExpanded(false);
                    }
                }, 500);
            } else {
                appBarLayout.setExpanded(false, false);
            }
        }
    }

    @Override
    public MainPortfolioView createView() {
        mMainPortfolioView = new MainPortfolioView(this, new MainPortfolioView.MainPortfolioViewListener() {
            @Override
            public void onGraphSelectionChanged(long accountId, int daysToReturn) {
                mGraphDataLoader.setSelectionCriteria(accountId, daysToReturn);
            }
        });
        return mMainPortfolioView;
    }

    @Override
    protected void createModel(TradeModelProvider modelProvider) {
        mSettings = modelProvider.getSettings();
        mPortfolioModel = new PortfolioSqliteModel(modelProvider);
    }

    @Override
    public void onResumeBase() {
        // onRestoreInstanceState is called after onStart but before OnResume
        // creating the loaders in onResume ensure the views are correctly restored
        if ((mPortfolioLoader == null) || (mGraphDataLoader == null)) {
            LoaderManager loaderManager = getSupportLoaderManager();
            mPortfolioLoader = (PortfolioLoader) loaderManager.initLoader(PORTFOLIO_LOADER_ID, null, mPortfolioDataLoaderCallback);
            mGraphDataLoader = (GraphDataLoader) loaderManager.initLoader(GRAPH_LOADER_ID, null, mGraphDataLoaderCallback);
        }
    }

    @Override
    public void onStartBase() {

        if (mAccountUpdateReceiver == null) {
            mAccountUpdateReceiver = new AccountUpdateReceiver();

            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(mAccountUpdateReceiver, new IntentFilter(PerformanceItemUpdateBroadcaster.ACTION));
        }
    }

    @Override
    public void onStopBase() {
        if (mAccountUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mAccountUpdateReceiver);
            mAccountUpdateReceiver = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);
        mMenuProgressBar = menu.findItem(R.id.menu_progress_bar);
        mMenuRefreshButton = menu.findItem(R.id.menu_refresh);
        mMenuHideExcludeAccounts = menu.findItem(R.id.menu_hide_exclude_accounts);
        mMenuDemoMode = menu.findItem(R.id.menu_demo_mode);

        // tint all the menu item icons
        ColorStateList colorSelector = ContextCompat.getColorStateList(this, R.color.nav_on_color);
        for (int x = 0; x < menu.size(); x++) {
            MenuItem item = menu.getItem(x);
            Drawable icon = item.getIcon();
            if (icon != null) {
                DrawableCompat.setTintList(icon, colorSelector);
                item.setIcon(icon);
            }
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        mMenuHideExcludeAccounts.setChecked(mSettings.getBoolean(Settings.Key.PREF_HIDE_EXCLUDE_ACCOUNTS));
        mMenuDemoMode.setChecked(mSettings.getBoolean(Settings.Key.PREF_DEMO_MODE));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        boolean handled = false;
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(SettingsActivity.newIntent(this));
                handled = true;
                break;
            case R.id.menu_refresh:
                refresh();
                handled = true;
                break;
            case R.id.menu_new_portfolio:
                showNewAccountActivity();
                handled = true;
                break;
            case R.id.menu_backup_db:
                backupDatabaseToSDCard();

                handled = true;
                break;
            case R.id.menu_restore_db:
                restoreLatestDatabase();
                handled = true;
                break;
            case R.id.menu_hide_exclude_accounts:
                boolean hideExcludeAccounts = !mMenuHideExcludeAccounts.isChecked();
                mSettings.setBoolean(Settings.Key.PREF_HIDE_EXCLUDE_ACCOUNTS, hideExcludeAccounts);
                mMenuHideExcludeAccounts.setChecked(hideExcludeAccounts);
                mMainPortfolioView.resetSelectedAccountID();
                updateView();
                handled = true;
                break;
            case R.id.menu_demo_mode:
                boolean demoMode = !mMenuDemoMode.isChecked();
                mSettings.setBoolean(Settings.Key.PREF_DEMO_MODE, demoMode);
                mMenuDemoMode.setChecked(demoMode);
                updateView();
                handled = true;
                break;
        }

        return handled;
    }


    @Override
    public boolean onHandleException(String logMsg, Exception ex) {
        String displayMsg = ex.getLocalizedMessage();
        if (TextUtils.isEmpty(displayMsg)) {
            displayMsg = ex.toString();
        }

        getSnackbar(mMainPortfolioView, displayMsg, Snackbar.LENGTH_LONG,
                R.color.snackbar_background,
                R.color.failure,
                android.support.design.R.id.snackbar_text)
                .show();

        return true;
    }

    private void backupDatabaseToSDCard() {

        if (isStoragePermissionsGranted(PERMS_REQUEST_BACKUP)) {

            boolean success = TradeApplication.backupDatabase(this, false);

            String msg = getResources().getString(success ? R.string.menu_backup_db_success : R.string.menu_backup_db_fail);

            getSnackbar(mMainPortfolioView, msg, Snackbar.LENGTH_LONG,
                    R.color.snackbar_background,
                    success ? R.color.success : R.color.failure,
                    android.support.design.R.id.snackbar_text)
                    .show();
        }
    }

    private void restoreLatestDatabase() {

        if (isStoragePermissionsGranted(PERMS_REQUEST_RESTORE)) {
            boolean success = TradeApplication.restoreDatabase(this);

            String msg = getResources().getString(success ? R.string.menu_restore_db_success : R.string.menu_restore_db_fail);

            getSnackbar(mMainPortfolioView, msg, Snackbar.LENGTH_LONG,
                    R.color.snackbar_background,
                    success ? R.color.success : R.color.failure,
                    android.support.design.R.id.snackbar_text)
                    .show();
        }
    }

    private boolean isStoragePermissionsGranted(int requestCode) {
        boolean hasPerms = true;
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {

            hasPerms = false;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    requestCode);
        }
        return hasPerms;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMS_REQUEST_BACKUP: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    backupDatabaseToSDCard();
                } else {
                    Log.w(TAG, "User Refused Read Storage");
                }
                break;
            }
            case PERMS_REQUEST_RESTORE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    restoreLatestDatabase();
                } else {
                    Log.w(TAG, "User Refused Read Storage");
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    protected void setupAdapter() {
        ViewProvider viewProvider = ((ViewProvider) this.getApplication());

        mPortfolioAdapter = new PortfolioAdapter(mSettings, viewProvider);
        mPortfolioAdapter.setListener(new PortfolioAdapter.PortfolioAdapterListener() {
            @Override
            public boolean onLongClickAccount(final Account account) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.account_delete_dlg_title)
                        .setMessage(String.format(getString(R.string.account_delete_dlg_message_format), account.getName()))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    mPortfolioModel.deleteAccount(account);
                                    updateView();
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error Deleting account", ex);
                                    Toast.makeText(MainActivity.this, "Error deleting account", Toast.LENGTH_LONG).show();
                                }
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
                showNewAccountActivity();
            }

            @Override
            public void createNewDogsAccount() {
                String name = getResources().getString(R.string.quickstart_account_name);
                String desc = getResources().getString(R.string.quickstart_account_desc);

                Account newAccount = new Account(name, desc,
                        new Money(100000.0), Account.Strategy.DOGS_OF_THE_DOW, false);
                new CreateAccountTask(MainActivity.this).execute(newAccount);
            }
        });

        mPortfolioAdapter.setAccountItemViewListener(new AccountViewHolder.AccountItemViewListener() {
            @Override
            public void onTradeButtonClicked(Account account) {
                showNewBuyOrderActivity(account);
            }

            @Override
            public void onShowOpenOrdersClicked(Account account) {
                startActivity(OrderListActivity.newIntent(MainActivity.this, account.getId()));
            }
        });
        this.mMainPortfolioView.setPortfolioAdapter(mPortfolioAdapter);
    }

    /**
     * Refresh launches the quote service which will call the QuoteUpdateReceiver
     * to update the UI once the quotes are fetched
     */
    public void refresh() {
        showProgress();
        startService(QuoteService.getIntent(this));
    }

    protected void showNewAccountActivity() {
        Intent intent = EditActivity.getIntent(mMainPortfolioView.getContext(), R.string.account_create_title,
                new Account("", "", new Money(100000.0), Account.Strategy.NONE, false),
                new AccountEditController(), 0, 0);

        startActivityForResult(intent, NEW_ACCOUNT_RESULT);
    }

    protected void showNewBuyOrderActivity(Account account) {
        Order order = new Order();
        order.setAccount(account);
        order.setAction(Order.OrderAction.BUY);
        order.setStrategy(Order.OrderStrategy.MARKET);
        Intent intent = EditActivity.getIntent(mMainPortfolioView.getContext(), R.string.order_create_buy_title,
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

        Intent intent = EditActivity.getIntent(mMainPortfolioView.getContext(),
                R.string.order_create_sell_title,
                order, new OrderEditController(), R.string.order_edit_ok_button_new, 0);

        startActivityForResult(intent, NEW_ORDER_RESULT);
    }

    @Override
    public void onActivityResultBase(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == NEW_ACCOUNT_RESULT) {
                Account account = EditActivity.getResult(data);
                if (account != null) {
                    Account newAccount = new Account(account.getName(), account.getDescription(),
                            account.getInitialBalance(), account.getStrategy(), account.getExcludeFromTotals());
                    new CreateAccountTask(this).execute(newAccount);
                }
            } else if (requestCode == NEW_ORDER_RESULT) {
                Order order = EditActivity.getResult(data);
                if (order != null) {
                    mPortfolioModel.createOrder(order);
                    mPortfolioLoader.onContentChanged();

                    mPortfolioModel.processOrders(this,
                            (order.getStrategy() == Order.OrderStrategy.MANUAL));
                }
            }
        }
    }

    public void showProgress() {
        if (mMenuProgressBar != null) {
            mMenuProgressBar.setVisible(true);
            mMenuRefreshButton.setVisible(false);
        }
    }

    public void hideProgress() {
        if (mMenuProgressBar != null) {
            mMenuProgressBar.setVisible(false);
            mMenuRefreshButton.setVisible(true);
        }
    }

/*
    private List<PerformanceItem> generateRandomTestData() {
        List<PerformanceItem> performanceItems = new ArrayList<>();

        long currentTime = 1450967400000L;
        long fiveMinutes = 1000L * 60 * 5;
        double costBasis = 100000.0;
        double value = 100000.0;
        double todayChange = 0.0;
        int onePercentCostBases = (int) (costBasis * .1);
        int randomOffset = onePercentCostBases / 2;

        Random random = new Random();
        int samples = (int) (12 * 6.5f);

        for (int x = 0; x < samples; x++) {
            performanceItems.add(new PerformanceItem(-1L, new Date(currentTime),
                    new Money(costBasis), new Money(value), new Money(todayChange)));

            double randChange = randomOffset - (random.nextInt(onePercentCostBases * 100) / 100.0);
            todayChange += randChange;
            value += randChange;
            currentTime += fiveMinutes;
        }

        return performanceItems;
    }
*/

    private void updateView() {
        mGraphDataLoader.forceLoad();
        mPortfolioLoader.forceLoad();
    }

    private static class CreateAccountTask extends AsyncTask<Account, Void, Void> {
        private final WeakReference<MainActivity> mWeakActivity;
        private final PortfolioModel mPortfolioModel;

        private CreateAccountTask(MainActivity activity) {
            this.mWeakActivity = new WeakReference<>(activity);

            // the models are application scope so it is OK to hold a ref to it
            mPortfolioModel = activity.mPortfolioModel;
        }

        @Override
        protected Void doInBackground(Account... accounts) {
            for (Account account : accounts) {
                mPortfolioModel.createAccount(account);
                if (isCancelled()) {
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            MainActivity activity = mWeakActivity.get();
            if ((activity != null) && !activity.isFinishing()) {
                activity.showProgress();
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            MainActivity activity = mWeakActivity.get();
            if ((activity != null) && !activity.isFinishing()) {
                activity.hideProgress();
                activity.mPortfolioLoader.onContentChanged();
            }
        }
    }

    private class AccountUpdateReceiver extends BroadcastReceiver {
        private AccountUpdateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            PerformanceItemUpdateBroadcaster.PerformanceItemUpdateData data = PerformanceItemUpdateBroadcaster.getData(intent);
            mGraphDataLoader.setSelectionCriteria(data.accountId, data.days);
            mGraphDataLoader.forceLoad();
            mMainPortfolioView.setAccountSpinner(data.accountId);
        }
    }

}


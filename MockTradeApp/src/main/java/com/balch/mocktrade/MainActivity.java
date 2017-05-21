package com.balch.mocktrade;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.balch.mocktrade.finance.GoogleFinanceApi;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.order.OrderEditController;
import com.balch.mocktrade.order.OrderListActivity;
import com.balch.mocktrade.portfolio.AccountViewHolder;
import com.balch.mocktrade.portfolio.PortfolioAdapter;
import com.balch.mocktrade.portfolio.PortfolioData;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioSqliteModel;
import com.balch.mocktrade.services.PerformanceItemUpdateBroadcaster;
import com.balch.mocktrade.services.QuoteService;
import com.balch.mocktrade.settings.Settings;
import com.balch.mocktrade.settings.SettingsActivity;
import com.balch.mocktrade.shared.PerformanceItem;

import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends PresenterActivity<MainPortfolioView, TradeModelProvider>
        implements LifecycleRegistryOwner {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int NEW_ACCOUNT_RESULT = 0;
    private static final int NEW_ORDER_RESULT = 1;

    private static final int PERMS_REQUEST_BACKUP = 0;
    private static final int PERMS_REQUEST_RESTORE = 1;

    private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    private PortfolioModel portfolioModel;

    private PortfolioAdapter portfolioAdapter;

    private MenuItem menuProgressBar;
    private MenuItem menuRefreshButton;
    private MenuItem menuHideExcludeAccounts;
    private MenuItem menuDemoMode;

    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private Settings appSetting;

    private PortfolioViewModel portfolioViewModel;
    private Disposable disposableNewAccount = null;

    private AccountUpdateReceiver accountUpdateReceiver;

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
                uiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        appBarLayout.setExpanded(false);
                    }
                }, 500);
            } else {
                appBarLayout.setExpanded(false, false);
            }
        }

        portfolioViewModel.getGraphData(this).observe(this, graphDataObserver);
        portfolioViewModel.getPortfolioData(this).observe(this, portfolioDataObserver);
    }

    @Override
    public void onDestroyBase() {
        portfolioViewModel.getGraphData(this).removeObserver(graphDataObserver);
        portfolioViewModel.getPortfolioData(this).removeObserver(portfolioDataObserver);
        disposeNewAccount();
    }

    @Override
    public MainPortfolioView createView() {
        return new MainPortfolioView(this, new MainPortfolioView.MainPortfolioViewListener() {
            @Override
            public void onGraphSelectionChanged(long accountId, int daysToReturn) {
                portfolioViewModel.setGraphSelectionCriteria(accountId, daysToReturn);
            }
        });
    }

    @Override
    protected void createModel(TradeModelProvider modelProvider) {
        portfolioViewModel = getPortfolioViewModel();
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
    }

    @Override
    public void onStartBase() {
        updateView();

        if (accountUpdateReceiver == null) {
            accountUpdateReceiver = new AccountUpdateReceiver();

            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(accountUpdateReceiver, new IntentFilter(PerformanceItemUpdateBroadcaster.ACTION));
        }
    }

    @Override
    public void onStopBase() {
        if (accountUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(accountUpdateReceiver);
            accountUpdateReceiver = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);
        menuProgressBar = menu.findItem(R.id.menu_progress_bar);
        menuRefreshButton = menu.findItem(R.id.menu_refresh);
        menuHideExcludeAccounts = menu.findItem(R.id.menu_hide_exclude_accounts);
        menuDemoMode = menu.findItem(R.id.menu_demo_mode);

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

        menuHideExcludeAccounts.setChecked(appSetting.getBoolean(Settings.Key.PREF_HIDE_EXCLUDE_ACCOUNTS));
        menuDemoMode.setChecked(appSetting.getBoolean(Settings.Key.PREF_DEMO_MODE));
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
                boolean hideExcludeAccounts = !menuHideExcludeAccounts.isChecked();
                appSetting.setBoolean(Settings.Key.PREF_HIDE_EXCLUDE_ACCOUNTS, hideExcludeAccounts);
                menuHideExcludeAccounts.setChecked(hideExcludeAccounts);
                view.resetSelectedAccountID();
                updateView();
                handled = true;
                break;
            case R.id.menu_demo_mode:
                boolean demoMode = !menuDemoMode.isChecked();
                appSetting.setBoolean(Settings.Key.PREF_DEMO_MODE, demoMode);
                menuDemoMode.setChecked(demoMode);
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

        getSnackbar(view, displayMsg, Snackbar.LENGTH_LONG,
                R.color.snackbar_background,
                R.color.failure,
                android.support.design.R.id.snackbar_text)
                .show();

        return true;
    }

    private Observer<List<PerformanceItem>> graphDataObserver = new Observer<List<PerformanceItem>>() {
        @Override
        public void onChanged(@Nullable List<PerformanceItem> data) {
            view.setDailyGraphData(data);
//          mMainPortfolioView.setDailyGraphData(generateRandomTestData());

            hideProgress();
        }
    };

    private Observer<PortfolioData> portfolioDataObserver = new Observer<PortfolioData>() {
        @Override
        public void onChanged(@Nullable PortfolioData data) {
            displayPortfolioData(data);
        }
    };

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

        ViewProvider viewProvider = (ViewProvider) getApplication();

        boolean showTotals = (!viewProvider.isLandscape(MainActivity.this) || viewProvider.isTablet(MainActivity.this)) &&
                (accountsWithTotals > 1);
        view.setTotals(showTotals, performanceItem);

        portfolioAdapter.bind(data);

        view.setDailyGraphDataAccounts(data.getAccounts());

        hideProgress();
    }

    private void backupDatabaseToSDCard() {
        if (isStoragePermissionsGranted(PERMS_REQUEST_BACKUP)) {

            boolean success = TradeApplication.backupDatabase(this, false);
            String msg = getResources().getString(success ? R.string.menu_backup_db_success : R.string.menu_backup_db_fail);

            getSnackbar(view, msg, Snackbar.LENGTH_LONG,
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

            getSnackbar(view, msg, Snackbar.LENGTH_LONG,
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

        portfolioAdapter = new PortfolioAdapter(appSetting, viewProvider);
        portfolioAdapter.setListener(new PortfolioAdapter.PortfolioAdapterListener() {
            @Override
            public boolean onLongClickAccount(final Account account) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.account_delete_dlg_title)
                        .setMessage(String.format(getString(R.string.account_delete_dlg_message_format), account.getName()))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    portfolioModel.deleteAccount(account);
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
                startActivity(OrderListActivity.newIntent(MainActivity.this, account.getId()));
            }
        });
        this.view.setPortfolioAdapter(portfolioAdapter);
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
        Intent intent = EditActivity.getIntent(view.getContext(), R.string.account_create_title,
                new Account("", "", new Money(100000.0), Account.Strategy.NONE, false),
                new AccountEditController(), 0, 0);

        startActivityForResult(intent, NEW_ACCOUNT_RESULT);
    }

    protected void showNewBuyOrderActivity(Account account) {
        Order order = new Order();
        order.setAccount(account);
        order.setAction(Order.OrderAction.BUY);
        order.setStrategy(Order.OrderStrategy.MARKET);
        Intent intent = EditActivity.getIntent(view.getContext(), R.string.order_create_buy_title,
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

        Intent intent = EditActivity.getIntent(view.getContext(),
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
                    createNewAccountAsync(newAccount);
                }
            } else if (requestCode == NEW_ORDER_RESULT) {
                Order order = EditActivity.getResult(data);
                if (order != null) {
                    portfolioModel.createOrder(order);
                    updateView();

                    portfolioModel.processOrders(this,
                            (order.getStrategy() == Order.OrderStrategy.MANUAL));
                }
            }
        }
    }

    public void showProgress() {
        if (menuProgressBar != null) {
            menuProgressBar.setVisible(true);
            menuRefreshButton.setVisible(false);
        }
    }

    public void hideProgress() {
        if (menuProgressBar != null) {
            menuProgressBar.setVisible(false);
            menuRefreshButton.setVisible(true);
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
        portfolioViewModel.loadGraphData();
        portfolioViewModel.loadPortfolioData();
    }

    private void createNewAccountAsync(final Account account) {
        showProgress();
        disposeNewAccount();
        disposableNewAccount = Observable.just(true)
                .subscribeOn(Schedulers.io())
                .map(new Function<Boolean, Boolean>() {
                    @Override
                    public Boolean apply(@io.reactivex.annotations.NonNull Boolean aBoolean) throws Exception {
                        portfolioModel.createAccount(account);
                        return true;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                               @Override
                               public void accept(@io.reactivex.annotations.NonNull Boolean aBoolean) throws Exception {
                                   hideProgress();
                                   updateView();
                               }
                           },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                                    hideProgress();
                                    Log.e(TAG, "createNewAccountAsync error", throwable);
                                }
                            });
    }

    private void disposeNewAccount() {
        if (disposableNewAccount != null) {
            disposableNewAccount.dispose();
            disposableNewAccount = null;
        }
    }

    private class AccountUpdateReceiver extends BroadcastReceiver {
        private AccountUpdateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            PerformanceItemUpdateBroadcaster.PerformanceItemUpdateData data = PerformanceItemUpdateBroadcaster.getData(intent);
            portfolioViewModel.setGraphSelectionCriteria(data.accountId, data.days);
            view.setAccountSpinner(data.accountId);
        }
    }

    private PortfolioViewModel getPortfolioViewModel() {
        return ViewModelProviders.of(this).get(PortfolioViewModel.class);
    }

    @Override
    public LifecycleRegistry getLifecycle() {
        return lifecycleRegistry;
    }

}


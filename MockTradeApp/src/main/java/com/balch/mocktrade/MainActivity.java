package com.balch.mocktrade;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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

import com.balch.android.app.framework.BaseAppCompatActivity;
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
import com.balch.mocktrade.services.AccountUpdateBroadcaster;
import com.balch.mocktrade.services.QuoteService;
import com.balch.mocktrade.settings.Settings;
import com.balch.mocktrade.settings.SettingsActivity;
import com.balch.mocktrade.shared.PerformanceItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends BaseAppCompatActivity<MainPortfolioView> {
    private static final String TAG = MainActivity.class.getSimpleName();

    protected static final int PORTFOLIO_LOADER_ID = 0;
    protected static final int GRAPH_LOADER_ID = 1;

    protected static final int NEW_ACCOUNT_RESULT = 0;
    protected static final int NEW_ORDER_RESULT = 1;

    protected static final int PERMS_REQUEST_BACKUP = 0;
    protected static final int PERMS_REQUEST_RESTORE = 1;

    protected PortfolioModel mPortfolioModel;
    protected MainPortfolioView mMainPortfolioView;

    protected PortfolioAdapter mPortfolioAdapter;

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

                    boolean showTotals = (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) &&
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
    protected void onCreateBase(Bundle bundle) {

        ModelProvider modelProvider = ((ModelProvider) this.getApplication());

        mSettings = modelProvider.getSettings();
        mPortfolioModel = new PortfolioSqliteModel(modelProvider);

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

        LoaderManager loaderManager = getSupportLoaderManager();
        mPortfolioLoader = (PortfolioLoader)loaderManager.initLoader(PORTFOLIO_LOADER_ID, null, mPortfolioDataLoaderCallback);
        mGraphDataLoader = (GraphDataLoader)loaderManager.initLoader(GRAPH_LOADER_ID, null, mGraphDataLoaderCallback);
    }

    @Override
    protected MainPortfolioView createView() {
        mMainPortfolioView = new MainPortfolioView(this, new MainPortfolioView.MainPortfolioViewListener() {
            @Override
            public void onGraphSelectionChanged(long accountId) {
                mGraphDataLoader.setSelectedAccountId(accountId);
            }
        });
        return mMainPortfolioView;
    }

    @Override
    protected void onStartBase() {
        if (mAccountUpdateReceiver == null) {
            mAccountUpdateReceiver = new AccountUpdateReceiver();

            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(mAccountUpdateReceiver, new IntentFilter(AccountUpdateBroadcaster.ACTION));
        }
    }

    @Override
    protected void onStopBase() {
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
    protected void onHandleException(String logMsg, String displayMsg, Exception ex) {
        if (TextUtils.isEmpty(displayMsg)) {
            displayMsg = ex.toString();
        }

        getSnackbar(mMainPortfolioView, displayMsg, Snackbar.LENGTH_LONG,
                R.color.snackbar_background,
                R.color.failure,
                android.support.design.R.id.snackbar_text)
                .show();

    }

    private void backupDatabaseToSDCard() {

        if (isStoragePermissionsGranted(PERMS_REQUEST_BACKUP)) {
            boolean success = false;

            FileChannel src = null;
            FileChannel dst = null;
            try {
                File sd = Environment.getExternalStorageDirectory();

                if (sd.canWrite()) {
                    File dbFile = getDatabasePath(TradeApplication.DATABASE_NAME);

                    String backupDBPath = System.currentTimeMillis() + "_" + TradeApplication.DATABASE_NAME;
                    File backupDBFile = new File(sd, backupDBPath);

                    if (dbFile.exists()) {
                        src = new FileInputStream(dbFile).getChannel();
                        dst = new FileOutputStream(backupDBFile).getChannel();
                        dst.transferFrom(src, 0, src.size());

                        success = true;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error backing up to Database", e);
            } finally {
                if (src != null) {
                    try {
                        src.close();
                    } catch (IOException ignored) {
                    }
                }

                if (dst != null)
                    try {
                        dst.close();
                    } catch (IOException ignored) {
                    }
            }

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

            boolean success = false;

            FileChannel src = null;
            FileChannel dst = null;
            try {
                File sd = Environment.getExternalStorageDirectory();

                if (sd.canWrite()) {
                    File dbFile = getDatabasePath(TradeApplication.DATABASE_NAME);

                    File[] backups = sd.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.endsWith("_" + TradeApplication.DATABASE_NAME);
                        }
                    });

                    if (backups != null && backups.length > 1) {
                        Arrays.sort(backups, new Comparator<File>() {
                            @Override
                            public int compare(File object1, File object2) {
                                return object1.getName().compareTo(object2.getName());
                            }
                        });
                    }

                    if (backups != null) {
                        src = new FileInputStream(backups[backups.length - 1]).getChannel();
                        dst = new FileOutputStream(dbFile).getChannel();
                        dst.transferFrom(src, 0, src.size());

                        success = true;

                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error restoring Database", e);
            } finally {
                if (src != null) {
                    try {
                        src.close();
                    } catch (IOException ignored) {
                    }
                }

                if (dst != null)
                    try {
                        dst.close();
                    } catch (IOException ignored) {
                    }
            }

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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
        mPortfolioAdapter = new PortfolioAdapter(this);
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
    protected void onActivityResultBase(int requestCode, int resultCode, Intent data) {
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

    private void updateView() {
        mGraphDataLoader.onContentChanged();
        mPortfolioLoader.onContentChanged();
    }

    private static class CreateAccountTask extends AsyncTask<Account, Void, Void> {
        private final WeakReference<MainActivity> mWeakActivity;
        private final PortfolioModel mPortfolioModel;

        private CreateAccountTask(MainActivity activity) {
            this.mWeakActivity = new WeakReference<>(activity);

            // the models are application scope so it is OK to hold a ref to it
            mPortfolioModel = activity.mPortfolioModel;
        }


        protected Void doInBackground(Account... accounts) {
            for (Account account : accounts) {
                mPortfolioModel.createAccount(account);
                if (isCancelled()) {
                    break;
                }
            }
            return null;
        }

        protected void onPreExecute() {
            MainActivity activity = mWeakActivity.get();
            if (activity != null) {
                activity.showProgress();
            }
        }

        protected void onPostExecute(Void v) {
            MainActivity activity = mWeakActivity.get();
            if (activity != null) {
                if (!activity.isFinishing()) {
                    activity.hideProgress();
                    activity.mPortfolioLoader.onContentChanged();
                }
            }
        }
    }

    private class AccountUpdateReceiver extends BroadcastReceiver {
        private AccountUpdateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            long accountId = AccountUpdateBroadcaster.getAccountId(intent);
            mGraphDataLoader.setSelectedAccountId(accountId);
            mGraphDataLoader.forceLoad();
            mMainPortfolioView.setAccountSpinner(accountId);
        }
    }

}


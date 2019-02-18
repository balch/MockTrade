package com.balch.mocktrade;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import androidx.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.balch.android.app.framework.PresenterActivity;
import com.balch.android.app.framework.core.EditActivity;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountEditController;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.order.OrderEditController;
import com.balch.mocktrade.order.OrderListActivity;
import com.balch.mocktrade.services.PerformanceItemUpdateBroadcaster;
import com.balch.mocktrade.services.QuoteService;
import com.balch.mocktrade.settings.SettingsActivity;

public class MainActivity extends PresenterActivity<MainPortfolioView, MainPresenter> {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int NEW_ACCOUNT_RESULT = 0;
    private static final int NEW_ORDER_RESULT = 1;

    private static final int PERMS_REQUEST_BACKUP = 0;
    private static final int PERMS_REQUEST_RESTORE = 1;

    private MenuItem menuProgressBar;
    private MenuItem menuRefreshButton;
    private MenuItem menuHideExcludeAccounts;
    private MenuItem menuDemoMode;

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private AccountUpdateReceiver accountUpdateReceiver;

    @Override
    public void onCreateBase(Bundle bundle) {

        ViewProvider viewProvider = (ViewProvider) getApplication();

        if (viewProvider.isTablet(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        Toolbar toolbar = findViewById(R.id.portfolio_view_toolbar);
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

        final AppBarLayout appBarLayout = findViewById(R.id.portfolio_view_app_bar);
        if (appBarLayout != null) {
            if (bundle == null) {
                uiHandler.postDelayed(() -> appBarLayout.setExpanded(false), 500);
            } else {
                appBarLayout.setExpanded(false, false);
            }
        }
    }

    @Override
    public MainPortfolioView createView() {
        return new MainPortfolioView(this,
                (accountId, daysToReturn) ->
                        getPortfolioViewModel().setGraphSelectionCriteria(accountId, daysToReturn));
    }

    @Override
    protected MainPresenter createPresenter(MainPortfolioView view) {
        Application application = getApplication();
        return new MainPresenter((TradeModelProvider) application,
                (ViewProvider) application,
                getPortfolioViewModel(), this, view,
                new MainPresenter.ActivityBridge() {
                    @Override
                    public void showProgress(boolean show) {
                        MainActivity.this.showProgress(show);
                    }

                    @Override
                    public void startNewAccountActivity() {
                        MainActivity.this.showNewAccountActivity();
                    }

                    @Override
                    public void showSnackBar(View parent, String displayMsg, @ColorRes int colorId) {
                        getSnackbar(parent, displayMsg, Snackbar.LENGTH_LONG,
                                R.color.snackbar_background,
                                colorId,
                                com.google.android.material.R.id.snackbar_text)
                                .show();
                    }

                    @Override
                    public void requestStoragePermission(int requestCode) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                                requestCode);
                    }

                    @Override
                    public void startOrderListActivity(Account account) {
                        startActivity(OrderListActivity.newIntent(MainActivity.this, account.getId()));
                    }

                    @Override
                    public void startNewOrderActivity(Order order, @StringRes int stringId) {
                        Intent intent = EditActivity.getIntent(view.getContext(),
                                stringId, order,
                                new OrderEditController(), R.string.order_edit_ok_button_new, 0);

                        startActivityForResult(intent, NEW_ORDER_RESULT);
                    }

                    @Override
                    public void requestAccountDelete(Account account) {
                        Context context = MainActivity.this;
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.account_delete_dlg_title)
                                .setMessage(String.format(context.getString(R.string.account_delete_dlg_message_format), account.getName()))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                    try {
                                        presenter.deleteAccount(account);
                                    } catch (Exception ex) {
                                        Log.e(TAG, "Error Deleting account", ex);
                                        Toast.makeText(context, "Error deleting account", Toast.LENGTH_LONG).show();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null).show();
                    }
                });
    }

    @Override
    public void onStartBase() {
        presenter.updateView();

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

        menuHideExcludeAccounts.setChecked(presenter.getHideExcludeAccounts());
        menuDemoMode.setChecked(presenter.getDemoMode());
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
                presenter.backupDatabaseToSDCard(this, PERMS_REQUEST_BACKUP);

                handled = true;
                break;
            case R.id.menu_restore_db:
                presenter.restoreLatestDatabase(this, PERMS_REQUEST_RESTORE);
                handled = true;
                break;
            case R.id.menu_hide_exclude_accounts:
                boolean hideExcludeAccounts = !menuHideExcludeAccounts.isChecked();
                menuHideExcludeAccounts.setChecked(hideExcludeAccounts);

                presenter.setHideExcludeAccounts(hideExcludeAccounts);
                handled = true;
                break;
            case R.id.menu_demo_mode:
                boolean demoMode = !menuDemoMode.isChecked();
                menuDemoMode.setChecked(demoMode);
                presenter.setDemoMode(demoMode);
                handled = true;
                break;
        }

        return handled;
    }

    @Override
    public boolean onHandleException(String logMsg, Exception ex) {
        return presenter.handleException(logMsg, ex);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMS_REQUEST_BACKUP: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.backupDatabaseToSDCard(this, PERMS_REQUEST_BACKUP);
                } else {
                    Log.w(TAG, "User Refused Read Storage");
                }
                break;
            }
            case PERMS_REQUEST_RESTORE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.restoreLatestDatabase(this, PERMS_REQUEST_RESTORE);
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

    /**
     * Refresh launches the quote service which will call the QuoteUpdateReceiver
     * to update the UI once the quotes are fetched
     */
    public void refresh() {
        showProgress(true);
        startService(QuoteService.getIntent(this));
    }

    protected void showNewAccountActivity() {
        Intent intent = EditActivity.getIntent(this, R.string.account_create_title,
                new Account("", "", new Money(100000.0), Account.Strategy.NONE, false),
                new AccountEditController(), 0, 0);

        startActivityForResult(intent, NEW_ACCOUNT_RESULT);
    }

    @Override
    public void onActivityResultBase(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == NEW_ACCOUNT_RESULT) {
                Account account = EditActivity.getResult(data);
                presenter.createNewAccount(account);
            } else if (requestCode == NEW_ORDER_RESULT) {
                Order order = EditActivity.getResult(data);
                presenter.createNewOrder(order);
            }
        }
    }

    public void showProgress(boolean show) {
        if (menuProgressBar != null) {
            menuProgressBar.setVisible(show);
            menuRefreshButton.setVisible(!show);
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

    private class AccountUpdateReceiver extends BroadcastReceiver {
        private AccountUpdateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            PerformanceItemUpdateBroadcaster.PerformanceItemUpdateData data = PerformanceItemUpdateBroadcaster.getData(intent);
            presenter.updateAccount(data.accountId, data.days);
        }
    }

    private PortfolioViewModel getPortfolioViewModel() {
        return ViewModelProviders.of(this).get(PortfolioViewModel.class);
    }

}


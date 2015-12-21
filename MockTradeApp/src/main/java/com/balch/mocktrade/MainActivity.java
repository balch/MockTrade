package com.balch.mocktrade;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
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
            implements LoaderManager.LoaderCallbacks<PortfolioData>,
                         AppBarLayout.OnOffsetChangedListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    protected static final int ACCOUNT_LOADER_ID = 0;

    protected static final int NEW_ACCOUNT_RESULT = 0;
    protected static final int NEW_ORDER_RESULT = 1;
    private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.9f;
    private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS = 0.3f;
    private static final int ALPHA_ANIMATIONS_DURATION = 200;

    protected PortfolioModel mPortfolioModel;
    protected MainPortfolioView mMainPortfolioView;

    protected PortfolioAdapter mPortfolioAdapter;
    protected QuoteUpdateReceiver mQuoteUpdateReceiver;

    private boolean mIsTheTitleVisible = false;
    private boolean mIsTheTitleContainerVisible = true;

    private LinearLayout mTitleContainer;
    private TextView mTitle;

    private View mImageViewParallax;
    private FrameLayout mFrameViewParallax;

    private MenuItem mMenuProgressBar;
    private MenuItem mMenuRefreshButton;

    @Override
    protected void onCreateBase(Bundle bundle) {
        ModelFactory modelFactory = ((ModelProvider) this.getApplication()).getModelFactory();
        mPortfolioModel = modelFactory.getModel(PortfolioModel.class);

        mImageViewParallax = findViewById(R.id.portfolio_view_image_view_parallax);
        mFrameViewParallax = (FrameLayout) findViewById(R.id.portfolio_view_frame_layout_parallax);
        mTitle = (TextView) findViewById(R.id.portfolio_view_toolbar_title);
        mTitleContainer = (LinearLayout) findViewById(R.id.portfolio_view_toolbar_title_container);

      
        Toolbar toolbar = (Toolbar) findViewById(R.id.portfolio_view_toolbar);
        toolbar.setTitle("");

        ((AppBarLayout) findViewById(R.id.portfolio_view_app_bar)).addOnOffsetChangedListener(this);

        setSupportActionBar(toolbar);

        // tint the overflow icon
        ColorStateList colorSelector = getResources().getColorStateList(R.color.nav_on_color);
        Drawable icon = toolbar.getOverflowIcon();
        if (icon != null) {
            DrawableCompat.setTintList(icon, colorSelector);
            toolbar.setOverflowIcon(icon);
        }

        mQuoteUpdateReceiver = new QuoteUpdateReceiver();

        startAlphaAnimation(mTitle, 0, View.INVISIBLE);
        initParallaxValues();

        setupAdapter();
    }

    @Override
    protected MainPortfolioView createView() {
        mMainPortfolioView = new MainPortfolioView(this);
        return mMainPortfolioView;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        mMenuProgressBar = menu.findItem(R.id.menu_progress_bar);
        mMenuRefreshButton = menu.findItem(R.id.menu_refresh);

        // tint all the menu item icons
        ColorStateList colorSelector = getResources().getColorStateList(R.color.nav_on_color);
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
        }

        return handled;
    }
    @Override
    protected void onStartBase() {
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mQuoteUpdateReceiver, new IntentFilter(QuoteUpdateReceiver.class.getName()));
    }

    @Override
    protected void onStopBase() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mQuoteUpdateReceiver);
    }

    @Override
    protected void onResumeBase() {
        reload(true);
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
                                    reload(true);
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
        });

        mPortfolioAdapter.setAccountItemViewListener(new AccountItemView.AccountItemViewListener() {
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
        return new PortfolioLoader(this, this.mPortfolioModel);
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

        mMainPortfolioView.setTotals((accountsWithTotals > 1), totals, performanceItem);
        mPortfolioAdapter.bind(data);

        hideProgress();

        // hack to prevent onLoadFinished being called twice
        // http://stackoverflow.com/questions/11293441/android-loadercallbacks-onloadfinished-called-twice/22183247
//        getSupportLoaderManager().destroyLoader(ACCOUNT_LOADER_ID);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<PortfolioData> loader) {
        mPortfolioAdapter.clear(true);
    }

    protected static class PortfolioLoader extends AsyncTaskLoader<PortfolioData> {
        protected PortfolioModel mPortfolioModel;

        public PortfolioLoader(Context context, PortfolioModel model) {
            super(context);
            mPortfolioModel = model;
        }

        @Override
        public PortfolioData loadInBackground() {
            PortfolioData portfolioData = new PortfolioData();
            portfolioData.addAccounts(mPortfolioModel.getAllAccounts());
            portfolioData.addInvestments(mPortfolioModel.getAllInvestments());

            List<Order> openOrders = mPortfolioModel.getOpenOrders();
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
                    // create a new Account instance to make sure the account is initialized correctly
                    mPortfolioModel.createAccount(new Account(account.getName(), account.getDescription(),
                            account.getInitialBalance(), account.getStrategy(), account.getExcludeFromTotals()));
                    reload(true);
                }
            } else if (requestCode == NEW_ORDER_RESULT) {
                Order order = EditActivity.getResult(data);
                if (order != null) {
                    mPortfolioModel.createOrder(order);
                    reload(true);

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

    public class QuoteUpdateReceiver extends BroadcastReceiver {
        private QuoteUpdateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            reload(false);
        }

    }

    private void initParallaxValues() {
        CollapsingToolbarLayout.LayoutParams petDetailsLp =
                (CollapsingToolbarLayout.LayoutParams) mImageViewParallax.getLayoutParams();

        CollapsingToolbarLayout.LayoutParams petBackgroundLp =
                (CollapsingToolbarLayout.LayoutParams) mFrameViewParallax.getLayoutParams();

        petDetailsLp.setParallaxMultiplier(0.9f);
        petBackgroundLp.setParallaxMultiplier(0.3f);

        mImageViewParallax.setLayoutParams(petDetailsLp);
        mFrameViewParallax.setLayoutParams(petBackgroundLp);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;

        handleAlphaOnTitle(percentage);
        handleToolbarTitleVisibility(percentage);
    }

    private void handleToolbarTitleVisibility(float percentage) {
        if (percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {

            if (!mIsTheTitleVisible) {
                startAlphaAnimation(mTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleVisible = true;
            }

        } else {

            if (mIsTheTitleVisible) {
                startAlphaAnimation(mTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleVisible = false;
            }
        }
    }

    private void handleAlphaOnTitle(float percentage) {
        if (percentage >= PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
            if (mIsTheTitleContainerVisible) {
                startAlphaAnimation(mTitleContainer, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleContainerVisible = false;
            }

        } else {

            if (!mIsTheTitleContainerVisible) {
                startAlphaAnimation(mTitleContainer, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleContainerVisible = true;
            }
        }
    }

    public static void startAlphaAnimation(View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }


}


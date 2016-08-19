package com.balch.mocktrade.portfolio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.LocalBroadcastManager;

import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.settings.Settings;

import java.util.Date;
import java.util.List;

/**
 * Adapted From http://www.androiddesignpatterns.com/2012/08/implementing-loaders.html
 */
public class PortfolioLoader extends AsyncTaskLoader<PortfolioData> {
    private static final String TAG = PortfolioLoader.class.getSimpleName();

    private final PortfolioModel mPortfolioModel;
    private final Settings mSettings;

    private PortfolioData mPortfolioData;
    protected UpdateReceiver mUpdateReceiver;

    public PortfolioLoader(Context context, PortfolioModel model, Settings settings) {
        super(context);
        mPortfolioModel = model;
        mSettings = settings;
    }

    @Override
    public PortfolioData loadInBackground() {
        PortfolioData portfolioData = new PortfolioData();

        List<Account> accounts = mPortfolioModel.getAccounts(!mSettings.getBoolean(Settings.Key.PREF_HIDE_EXCLUDE_ACCOUNTS));

        portfolioData.addAccounts(accounts);
        portfolioData.addInvestments(mPortfolioModel.getAllInvestments());
        portfolioData.setLastSyncTime(new Date(mSettings.getLastSyncTime()));
        portfolioData.setLastQuoteTime(mPortfolioModel.getLastQuoteTime());

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

    @Override
    public void deliverResult(PortfolioData data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        PortfolioData oldData = mPortfolioData;
        mPortfolioData = data;

        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }

    }

    @Override
    protected void onStartLoading() {
        if (mPortfolioData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mPortfolioData);
        }

        // Begin monitoring the underlying data source.
        if (mUpdateReceiver == null) {
            mUpdateReceiver = new UpdateReceiver();

            LocalBroadcastManager.getInstance(getContext())
                    .registerReceiver(mUpdateReceiver, new IntentFilter(PortfolioUpdateBroadcaster.ACTION));
        }

        if (takeContentChanged() || mPortfolioData == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader has been stopped.
        onStopLoading();

        // At this point we can release the resources associated with 'mData'.
        if (mPortfolioData != null) {
            mPortfolioData = null;
        }

        // The Loader is being reset, so we should stop monitoring for changes.
        if (mUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(getContext())
                    .unregisterReceiver(mUpdateReceiver);
            mUpdateReceiver = null;
        }
    }

    @Override
    public void onCanceled(PortfolioData data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

    }

    private class UpdateReceiver extends BroadcastReceiver {
        private UpdateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            onContentChanged();
        }

    }
}



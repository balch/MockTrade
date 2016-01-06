package com.balch.mocktrade.portfolio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.LocalBroadcastManager;

import java.util.List;

public class GraphDataLoader extends AsyncTaskLoader<List<PerformanceItem>> {
    private static final String TAG = GraphDataLoader.class.getSimpleName();

    private static final String EXTRA_ACCOUNT_ID = "EXTRA_ACCOUNT_ID";

    private  final PortfolioModel mPortfolioModel;

    private long mSelectedAccountId = -1;
    protected UpdateReceiver mUpdateReceiver;
    protected List<PerformanceItem> mGraphData;

    public GraphDataLoader(Context context, PortfolioModel model) {
        super(context);
        mPortfolioModel = model;
    }

    @Override
    public List<PerformanceItem> loadInBackground() {
        return mPortfolioModel.getCurrentSnapshot(mSelectedAccountId);
    }

    @Override
    public void deliverResult(List<PerformanceItem> data) {
        if (isReset()) {
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<PerformanceItem> oldData = mGraphData;
        mGraphData = data;

        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }

    }

    @Override
    protected void onStartLoading() {
        if (mGraphData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mGraphData);
        }

        // Begin monitoring the underlying data source.
        if (mUpdateReceiver == null) {
            mUpdateReceiver = new UpdateReceiver();

            LocalBroadcastManager.getInstance(getContext())
                    .registerReceiver(mUpdateReceiver, new IntentFilter(UpdateReceiver.class.getName()));
        }

        if (takeContentChanged() || mGraphData == null) {
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
        if (mGraphData != null) {
            mGraphData = null;
        }

        // The Loader is being reset, so we should stop monitoring for changes.
        if (mUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(getContext())
                    .unregisterReceiver(mUpdateReceiver);
            mUpdateReceiver = null;
        }
    }

    static public void update(Context context) {
        update(context, null);
    }

    static public void update(Context context, Long accountId) {
        Intent intent = new Intent(UpdateReceiver.class.getName());
        if (accountId != null) {
            intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private class UpdateReceiver extends BroadcastReceiver {
        private UpdateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(EXTRA_ACCOUNT_ID)) {
                mSelectedAccountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
            }
            onContentChanged();
        }

    }


}



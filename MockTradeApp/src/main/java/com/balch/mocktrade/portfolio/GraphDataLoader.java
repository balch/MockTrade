package com.balch.mocktrade.portfolio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.LocalBroadcastManager;

import com.balch.mocktrade.shared.PerformanceItem;

import java.util.List;

public class GraphDataLoader extends AsyncTaskLoader<List<PerformanceItem>> {
    private static final String TAG = GraphDataLoader.class.getSimpleName();

    private  final PortfolioModel mPortfolioModel;

    private long mSelectedAccountId = -1;
    private int mDaysToReturn = -1;
    private UpdateReceiver mUpdateReceiver;
    private List<PerformanceItem> mGraphData;

    public GraphDataLoader(Context context, PortfolioModel model) {
        super(context);
        mPortfolioModel = model;
    }

    @Override
    public List<PerformanceItem> loadInBackground() {

        return (mDaysToReturn < 2) ?
                mPortfolioModel.getCurrentSnapshot(mSelectedAccountId) :
                mPortfolioModel.getCurrentDailySnapshot(mSelectedAccountId, mDaysToReturn);
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
                    .registerReceiver(mUpdateReceiver, new IntentFilter(PortfolioUpdateBroadcaster.ACTION));
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


    public void setSelectionCriteria(long accountID, int days) {
        mSelectedAccountId = accountID;
        mDaysToReturn = days;
        onContentChanged();
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



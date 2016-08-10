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

package com.balch.mocktrade;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.balch.android.app.framework.view.BaseView;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.portfolio.DailyGraphView;
import com.balch.mocktrade.portfolio.PortfolioAdapter;
import com.balch.mocktrade.portfolio.SummaryTotalsView;
import com.balch.mocktrade.shared.PerformanceItem;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainPortfolioView extends LinearLayout implements BaseView {

    public interface MainPortfolioViewListener {
        void onGraphSelectionChanged(long accountId);
    }

    protected PortfolioAdapter mPortfolioAdapter;
    protected RecyclerView mPortfolioList;

    protected SummaryTotalsView mPortfolioSummary;

    protected TextView mLastQuoteTime;
    protected TextView mLastSyncTime;

    protected DailyGraphView mDailyGraphView;
    protected TextView mEmptyGraphView;
    protected LinearLayout mGraphLayout;
    protected TextView mGraphTimeTitle;
    protected Spinner mGraphSpinner;

    protected ArrayAdapter<String> mGraphAccountAdapter;

    // variables that need to be persisted
    protected List<Long> mAccountIds = new ArrayList<>();
    protected int mSelectedPosition = 0;

    protected final MainPortfolioViewListener mMainPortfolioViewListener;

    public MainPortfolioView(Context context, MainPortfolioViewListener mainPortfolioViewListener) {
        super(context);
        this.mMainPortfolioViewListener = mainPortfolioViewListener;
    }

    @Override
    public void initializeLayout() {
        inflate(getContext(), R.layout.portfolio_view_main, this);

        mPortfolioList = (RecyclerView) findViewById(R.id.portfolio_list);
        mPortfolioList.setLayoutManager(new LinearLayoutManager(getContext()));

        mPortfolioSummary = (SummaryTotalsView) findViewById(R.id.portfolio_view_summary_view);

        mLastQuoteTime = (TextView) findViewById(R.id.portfolio_view_last_quote);
        mLastSyncTime = (TextView) findViewById(R.id.portfolio_view_last_sync);

        mDailyGraphView = (DailyGraphView) findViewById(R.id.portfolio_view_daily_graph);
        mEmptyGraphView = (TextView) findViewById(R.id.portfolio_view_daily_graph_empty);
        mGraphLayout = (LinearLayout) findViewById(R.id.portfolio_view_daily_graph_layout);
        mGraphTimeTitle = (TextView) findViewById(R.id.portfolio_view_graph_time_title);

        mGraphSpinner = (Spinner) findViewById(R.id.portfolio_view_graph_spinner);

        mGraphAccountAdapter = new ArrayAdapter<>(getContext(), R.layout.portfolio_view_graph_spinner_text);
        mGraphAccountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mGraphSpinner.setAdapter(mGraphAccountAdapter);

        mGraphSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mSelectedPosition != position) {
                    mSelectedPosition = position;
                    if (mMainPortfolioViewListener != null) {
                        mMainPortfolioViewListener.onGraphSelectionChanged(getSelectedAccountId());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSelectedPosition = 0;
            }
        });
    }

    public long getSelectedAccountId() {
        long accountId = -1;
        if (mSelectedPosition < mAccountIds.size()) {
            accountId = mAccountIds.get(mSelectedPosition);
        }

        return accountId;
    }

    public void resetSelectedAccountID() {
        mSelectedPosition = 0;
    }

    public void setPortfolioAdapter(PortfolioAdapter portfolioAdapter) {
        mPortfolioAdapter = portfolioAdapter;
        mPortfolioList.setAdapter(portfolioAdapter);
    }

    public void setTotals(boolean showTotals, PerformanceItem performanceItem) {
        mPortfolioSummary.setVisibility(showTotals ? VISIBLE : GONE);

        if (showTotals) {
            mPortfolioSummary.bind(performanceItem);
        }
    }

    public void setSyncTimes(Date lastSync, Date lastQuote) {
        Resources resources = getResources();
        mLastSyncTime.setText(resources.getString(R.string.portfolio_view_last_sync, getDateTimeString(lastSync)));
        mLastQuoteTime.setText(resources.getString(R.string.portfolio_view_last_quote, getDateTimeString(lastQuote)));
    }

    private String getDateTimeString(Date date) {
        String result = "";
        if ((date != null) && (date.getTime() > 0)) {
            result = DateUtils.isToday(date.getTime()) ?
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(date) :
                    DateFormat.getDateInstance(DateFormat.SHORT).format(date);
        }
        return result;
    }

    public void animateGraph() {
        mDailyGraphView.animateGraph();
    }


    public void setDailyGraphDataAccounts(List<Account> accounts) {
        if (accounts != null) {

            mGraphAccountAdapter.setNotifyOnChange(false);
            mGraphAccountAdapter.clear();
            mGraphAccountAdapter.add(getContext().getString(R.string.portfolio_view_graph_spinner_totals));

            mAccountIds.clear();
            mAccountIds.add(-1L);

            for (Account account : accounts) {
                mGraphAccountAdapter.add(account.getName());
                mAccountIds.add(account.getId());
            }
            mGraphAccountAdapter.setNotifyOnChange(true);
            mGraphAccountAdapter.notifyDataSetChanged();

            if (mSelectedPosition != AdapterView.INVALID_POSITION) {
                mGraphSpinner.setSelection(mSelectedPosition);
            }
        }

    }

    public void setAccountSpinner(long accountID) {
        mSelectedPosition = mAccountIds.contains(accountID) ? mAccountIds.indexOf(accountID) : 0;
        mGraphSpinner.setSelection(mSelectedPosition);
    }

    public void setDailyGraphData(List<PerformanceItem> performanceItems) {
        if ((performanceItems != null) && (performanceItems.size() >= 2)) {

            Date timestamp =  performanceItems.get(0).getTimestamp();
            if (DateUtils.isToday(timestamp.getTime())) {
                mGraphTimeTitle.setText(R.string.portfolio_view_time_title_today);
            } else {
                mGraphTimeTitle.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(timestamp));
            }
            mDailyGraphView.bind(performanceItems);
            mEmptyGraphView.setVisibility(GONE);
            mGraphLayout.setVisibility(VISIBLE);


        } else {
            mGraphLayout.setVisibility(GONE);
            mEmptyGraphView.setVisibility(VISIBLE);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.mSelectedPosition = this.mSelectedPosition;
        ss.mAccountIds = this.mAccountIds;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());

        this.mSelectedPosition = ss.mSelectedPosition;
        this.mAccountIds = ss.mAccountIds;
    }

    static class SavedState extends BaseSavedState {
        int mSelectedPosition;
        List<Long> mAccountIds;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mSelectedPosition = in.readInt();
            mAccountIds = new ArrayList<>();
            in.readList(mAccountIds, getClass().getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mSelectedPosition);
            out.writeList(mAccountIds);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}

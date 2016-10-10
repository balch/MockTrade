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
import android.support.annotation.LayoutRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.balch.android.app.framework.BaseView;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.portfolio.PortfolioAdapter;
import com.balch.mocktrade.portfolio.SummaryTotalsView;
import com.balch.mocktrade.shared.PerformanceItem;
import com.balch.mocktrade.shared.view.DailyGraphView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainPortfolioView extends LinearLayout implements BaseView {

    public interface MainPortfolioViewListener {
        void onGraphSelectionChanged(long accountId, int daysToReturn);
    }

    private final static int [] GRAPH_TIME_VALUES = {-1, 7, 30, 90, 180, 365};
    private final static int GRAPH_TIME_HOURLY_INDEX = 0;
    private final static DateFormat DATE_FORMAT_SHORT = DateFormat.getDateInstance(DateFormat.SHORT);

    private RecyclerView mPortfolioList;

    private SummaryTotalsView mPortfolioSummary;

    private TextView mLastQuoteTime;
    private TextView mLastSyncTime;

    private DailyGraphView mDailyGraphView;
    private TextView mEmptyGraphView;
    private Spinner mAccountGraphSpinner;

    private ArrayAdapter<String> mGraphAccountAdapter;
    private GraphTimeAdapter mGraphTimeAdapter;

    // variables that need to be persisted
    private List<Long> mAccountIds = new ArrayList<>();
    private int mAccountSelectedPosition = 0;
    private int mGraphTimeSelectedPosition = 0;

    private final MainPortfolioViewListener mMainPortfolioViewListener;

    public MainPortfolioView(Context context, MainPortfolioViewListener mainPortfolioViewListener) {
        super(context);
        this.mMainPortfolioViewListener = mainPortfolioViewListener;
    }

    @Override
    public void initializeLayout() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.portfolio_view_main, this);

        mPortfolioList = (RecyclerView) findViewById(R.id.portfolio_list);
        mPortfolioList.setLayoutManager(new LinearLayoutManager(getContext()));

        mPortfolioSummary = (SummaryTotalsView) findViewById(R.id.portfolio_view_summary_view);

        mLastQuoteTime = (TextView) findViewById(R.id.portfolio_view_last_quote);
        mLastSyncTime = (TextView) findViewById(R.id.portfolio_view_last_sync);

        mDailyGraphView = (DailyGraphView) findViewById(R.id.portfolio_view_daily_graph);
        mEmptyGraphView = (TextView) findViewById(R.id.portfolio_view_daily_graph_empty);

        mAccountGraphSpinner = (Spinner) findViewById(R.id.portfolio_view_account_graph_spinner);
        Spinner timeGraphSpinner = (Spinner) findViewById(R.id.portfolio_view_time_graph_spinner);

        mGraphAccountAdapter = new ArrayAdapter<>(getContext(), R.layout.portfolio_view_graph_spinner_text);
        mGraphAccountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAccountGraphSpinner.setAdapter(mGraphAccountAdapter);

        mAccountGraphSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mAccountSelectedPosition != position) {
                    mAccountSelectedPosition = position;
                    if (mMainPortfolioViewListener != null) {
                        mMainPortfolioViewListener.onGraphSelectionChanged(getSelectedAccountId(), getGraphDays());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mAccountSelectedPosition = 0;
            }
        });

        Resources resources = getResources();

        mGraphTimeAdapter = new GraphTimeAdapter(getContext(), R.layout.portfolio_view_graph_spinner_text, android.R.layout.simple_spinner_dropdown_item);
        mGraphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_today));
        mGraphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_last_week));
        mGraphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_last_month));
        mGraphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_last_90_days));
        mGraphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_last_180_days));
        mGraphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_last_year));
        timeGraphSpinner.setAdapter(mGraphTimeAdapter);

        timeGraphSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mGraphTimeSelectedPosition != position) {
                    mGraphTimeSelectedPosition = position;
                    if (mMainPortfolioViewListener != null) {
                        mMainPortfolioViewListener.onGraphSelectionChanged(getSelectedAccountId(), getGraphDays());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGraphTimeSelectedPosition = 0;
            }
        });

    }

    public long getSelectedAccountId() {
        long accountId = -1;
        if (mAccountSelectedPosition < mAccountIds.size()) {
            accountId = mAccountIds.get(mAccountSelectedPosition);
        }

        return accountId;
    }

    public int getGraphDays() {
        int days = -1;
        if (mGraphTimeSelectedPosition < GRAPH_TIME_VALUES.length) {
            days = GRAPH_TIME_VALUES[mGraphTimeSelectedPosition];
        }
        return days;
    }

    public void resetSelectedAccountID() {
        mAccountSelectedPosition = 0;
    }

    public void setPortfolioAdapter(PortfolioAdapter portfolioAdapter) {
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

            if (mAccountSelectedPosition != AdapterView.INVALID_POSITION) {
                mAccountGraphSpinner.setSelection(mAccountSelectedPosition);
            }
        }

    }

    public void setAccountSpinner(long accountID) {
        mAccountSelectedPosition = mAccountIds.contains(accountID) ? mAccountIds.indexOf(accountID) : 0;
        mAccountGraphSpinner.setSelection(mAccountSelectedPosition);
    }

    public void setDailyGraphData(List<PerformanceItem> performanceItems) {
        if ((performanceItems != null) && (performanceItems.size() >= 2)) {

            if (mGraphTimeSelectedPosition == GRAPH_TIME_HOURLY_INDEX) {
                Date timestamp = performanceItems.get(performanceItems.size() - 1).getTimestamp();
                String label = (DateUtils.isToday(timestamp.getTime()) ?
                        getResources().getString(R.string.portfolio_view_time_title_today) :
                        DATE_FORMAT_SHORT.format(timestamp));

                if (!mGraphTimeAdapter.getItem(GRAPH_TIME_HOURLY_INDEX).equals(label)) {
                    mGraphTimeAdapter.set(GRAPH_TIME_HOURLY_INDEX, label);
                }
            }

            mDailyGraphView.bind(performanceItems, (mGraphTimeSelectedPosition == GRAPH_TIME_HOURLY_INDEX));
            mEmptyGraphView.setVisibility(GONE);
            mDailyGraphView.setVisibility(VISIBLE);


        } else {
            mDailyGraphView.setVisibility(GONE);
            mEmptyGraphView.setVisibility(VISIBLE);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.mAccountSelectedPosition = this.mAccountSelectedPosition;
        ss.mAccountIds = this.mAccountIds;
        ss.mGraphTimeSelectedPosition = this.mGraphTimeSelectedPosition;

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

        this.mAccountSelectedPosition = ss.mAccountSelectedPosition;
        this.mAccountIds = ss.mAccountIds;
        this.mGraphTimeSelectedPosition = ss.mGraphTimeSelectedPosition;
    }

    static class SavedState extends BaseSavedState {
        int mAccountSelectedPosition;
        int mGraphTimeSelectedPosition;
        List<Long> mAccountIds;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mAccountSelectedPosition = in.readInt();
            mAccountIds = new ArrayList<>();
            in.readList(mAccountIds, getClass().getClassLoader());
            mGraphTimeSelectedPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mAccountSelectedPosition);
            out.writeList(mAccountIds);
            out.writeInt(mGraphTimeSelectedPosition);
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

    private static class GraphTimeAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private final int mResource;
        private final int mDropDownResource;

        private final List<String> mDataItems = new ArrayList<>();

        private GraphTimeAdapter(Context context, @LayoutRes int resource, @LayoutRes int dropdownResource) {
            this.mInflater = LayoutInflater.from(context);
            this.mResource = resource;
            this.mDropDownResource = dropdownResource;
        }

        @Override
        public int getCount() {
            return mDataItems.size();
        }

        @Override
        public String getItem(int position) {
            return mDataItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void add(String item) {
            mDataItems.add(item);
        }

        public void set(int position, String text) {
            mDataItems.set(position, text);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(position, convertView, parent, mResource);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(position, convertView, parent, mDropDownResource);
        }

        private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
            View view;
            TextView text;

            if (convertView == null) {
                view = mInflater.inflate(resource, parent, false);
            } else {
                view = convertView;
            }

            text = (TextView) view;
            text.setText(getItem(position));

            return view;
        }

    }
}

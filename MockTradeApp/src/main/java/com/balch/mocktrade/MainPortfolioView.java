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
import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.EditState;
import com.balch.android.app.framework.domain.ViewHint;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.order.StockSymbolLayout;
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

    private RecyclerView portfolioRecycler;

    private SummaryTotalsView portfolioSummary;

    private TextView lastQuoteTime;
    private TextView lastSyncTime;

    private DailyGraphView dailyGraphView;
    private TextView emptyGraphView;
    private Spinner accountGraphSpinner;

    private ArrayAdapter<String> graphAccountAdapter;
    private GraphTimeAdapter graphTimeAdapter;

    // variables that need to be persisted
    private List<Long> accountIds = new ArrayList<>();
    private int accountSelectedPosition = 0;
    private int graphTimeSelectedPosition = 0;

    private final MainPortfolioViewListener listener;

    public MainPortfolioView(Context context, MainPortfolioViewListener listener) {
        super(context);
        this.listener = listener;
        initializeLayout();
    }

    private void initializeLayout() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.portfolio_view_main, this);

        portfolioRecycler = (RecyclerView) findViewById(R.id.portfolio_list);
        portfolioRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        portfolioSummary = (SummaryTotalsView) findViewById(R.id.portfolio_view_summary_view);

        lastQuoteTime = (TextView) findViewById(R.id.portfolio_view_last_quote);
        lastSyncTime = (TextView) findViewById(R.id.portfolio_view_last_sync);

        dailyGraphView = (DailyGraphView) findViewById(R.id.portfolio_view_daily_graph);
        emptyGraphView = (TextView) findViewById(R.id.portfolio_view_daily_graph_empty);

        graphAccountAdapter = new ArrayAdapter<>(getContext(), R.layout.portfolio_view_graph_spinner_text);
        graphAccountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        accountGraphSpinner = (Spinner) findViewById(R.id.portfolio_view_account_graph_spinner);
        if (accountGraphSpinner != null) {
            accountGraphSpinner.setAdapter(graphAccountAdapter);

            accountGraphSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (accountSelectedPosition != position) {
                        accountSelectedPosition = position;
                        if (listener != null) {
                            listener.onGraphSelectionChanged(getSelectedAccountId(), getGraphDays());
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    accountSelectedPosition = 0;
                }
            });
        }

        Resources resources = getResources();

        Spinner timeGraphSpinner = (Spinner) findViewById(R.id.portfolio_view_time_graph_spinner);
        if (timeGraphSpinner != null) {
            graphTimeAdapter = new GraphTimeAdapter(getContext(), R.layout.portfolio_view_graph_spinner_text, android.R.layout.simple_spinner_dropdown_item);
            graphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_today));
            graphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_last_week));
            graphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_last_month));
            graphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_last_90_days));
            graphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_last_180_days));
            graphTimeAdapter.add(resources.getString(R.string.portfolio_view_time_title_last_year));
            timeGraphSpinner.setAdapter(graphTimeAdapter);

            timeGraphSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (graphTimeSelectedPosition != position) {
                        graphTimeSelectedPosition = position;
                        if (listener != null) {
                            listener.onGraphSelectionChanged(getSelectedAccountId(), getGraphDays());
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    graphTimeSelectedPosition = 0;
                }
            });
        }

        StockSymbolLayout stockSymbolLayout = (StockSymbolLayout) findViewById(R.id.portfolio_nav_stock_picker);
        if (stockSymbolLayout != null) {
            ColumnDescriptor columnDescriptor = new ColumnDescriptor(null, null, R.string.order_symbol_label, EditState.CHANGEABLE,
                    new ViewHint[]{new ViewHint(ViewHint.Hint.MAX_CHARS, "32"), new ViewHint(ViewHint.Hint.NOT_EMPTY, "true")}, 1, null);
            stockSymbolLayout.bind(columnDescriptor);
        }
    }

    public long getSelectedAccountId() {
        long accountId = -1;
        if (accountSelectedPosition < accountIds.size()) {
            accountId = accountIds.get(accountSelectedPosition);
        }

        return accountId;
    }

    public int getGraphDays() {
        int days = -1;
        if (graphTimeSelectedPosition < GRAPH_TIME_VALUES.length) {
            days = GRAPH_TIME_VALUES[graphTimeSelectedPosition];
        }
        return days;
    }

    public void resetSelectedAccountID() {
        accountSelectedPosition = 0;
    }

    public void setPortfolioAdapter(PortfolioAdapter portfolioAdapter) {
        portfolioRecycler.setAdapter(portfolioAdapter);
    }

    public void setTotals(boolean showTotals, PerformanceItem performanceItem) {
        portfolioSummary.setVisibility(showTotals ? VISIBLE : GONE);

        if (showTotals) {
            portfolioSummary.bind(performanceItem);
        }
    }

    public void setSyncTimes(Date lastSync, Date lastQuote) {
        Resources resources = getResources();
        lastSyncTime.setText(resources.getString(R.string.portfolio_view_last_sync, getDateTimeString(lastSync)));
        lastQuoteTime.setText(resources.getString(R.string.portfolio_view_last_quote, getDateTimeString(lastQuote)));
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
        dailyGraphView.animateGraph();
    }

    public void setDailyGraphDataAccounts(List<Account> accounts) {
        if ((accountGraphSpinner != null) && (accounts != null)) {

            graphAccountAdapter.setNotifyOnChange(false);
            graphAccountAdapter.clear();
            graphAccountAdapter.add(getContext().getString(R.string.portfolio_view_graph_spinner_totals));

            accountIds.clear();
            accountIds.add(-1L);

            for (Account account : accounts) {
                graphAccountAdapter.add(account.getName());
                accountIds.add(account.getId());
            }
            graphAccountAdapter.setNotifyOnChange(true);
            graphAccountAdapter.notifyDataSetChanged();

            if (accountSelectedPosition != AdapterView.INVALID_POSITION) {
                accountGraphSpinner.setSelection(accountSelectedPosition);
            }
        }

    }

    public void setAccountSpinner(long accountID) {
        accountSelectedPosition = accountIds.contains(accountID) ? accountIds.indexOf(accountID) : 0;
        accountGraphSpinner.setSelection(accountSelectedPosition);
    }

    public void setDailyGraphData(List<PerformanceItem> performanceItems) {

        if ((dailyGraphView != null) && (emptyGraphView != null)) {
            if ((performanceItems != null) && (performanceItems.size() >= 2)) {

                if (graphTimeSelectedPosition == GRAPH_TIME_HOURLY_INDEX) {
                    Date timestamp = performanceItems.get(performanceItems.size() - 1).getTimestamp();
                    String label = (DateUtils.isToday(timestamp.getTime()) ?
                            getResources().getString(R.string.portfolio_view_time_title_today) :
                            DATE_FORMAT_SHORT.format(timestamp));

                    if (!graphTimeAdapter.getItem(GRAPH_TIME_HOURLY_INDEX).equals(label)) {
                        graphTimeAdapter.set(GRAPH_TIME_HOURLY_INDEX, label);
                    }
                }

                dailyGraphView.bind(performanceItems, (graphTimeSelectedPosition == GRAPH_TIME_HOURLY_INDEX));
                emptyGraphView.setVisibility(GONE);
                dailyGraphView.setVisibility(VISIBLE);


            } else {
                dailyGraphView.setVisibility(GONE);
                emptyGraphView.setVisibility(VISIBLE);
            }
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.mAccountSelectedPosition = this.accountSelectedPosition;
        ss.mAccountIds = this.accountIds;
        ss.mGraphTimeSelectedPosition = this.graphTimeSelectedPosition;

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

        this.accountSelectedPosition = ss.mAccountSelectedPosition;
        this.accountIds = ss.mAccountIds;
        this.graphTimeSelectedPosition = ss.mGraphTimeSelectedPosition;
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

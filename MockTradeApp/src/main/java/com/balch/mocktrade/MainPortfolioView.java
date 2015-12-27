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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.view.BaseView;
import com.balch.mocktrade.portfolio.DailyGraphView;
import com.balch.mocktrade.portfolio.PerformanceItem;
import com.balch.mocktrade.portfolio.PortfolioAdapter;
import com.balch.mocktrade.portfolio.SummaryTotalsView;

import java.text.DateFormat;
import java.util.Date;

public class MainPortfolioView extends LinearLayout implements BaseView {

    protected PortfolioAdapter mPortfolioAdapter;
    protected RecyclerView mPortfolioList;

    protected SummaryTotalsView mPortfolioSummary;

    protected TextView mLastQuoteTime;
    protected TextView mLastSyncTime;

    protected DailyGraphView mDailyGraphView;

    public MainPortfolioView(Context context) {
        super(context);
    }

    public MainPortfolioView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MainPortfolioView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
            result = DateFormat.getDateInstance(DateFormat.SHORT).format(date);
            String today = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date());
            if (today.equals(result)) {
                result = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
            }
        }
        return result;
    }

    public void animateGraph() {
        mDailyGraphView.animateGraph();
    }

}

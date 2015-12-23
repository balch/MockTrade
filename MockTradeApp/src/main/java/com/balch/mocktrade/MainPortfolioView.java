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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.balch.android.app.framework.view.BaseView;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.portfolio.AccountViewHolder;
import com.balch.mocktrade.portfolio.PerformanceItem;
import com.balch.mocktrade.portfolio.PortfolioAdapter;

public class MainPortfolioView extends LinearLayout implements BaseView {

    protected PortfolioAdapter mPortfolioAdapter;
    protected RecyclerView mPortfolioList;

    protected SummaryAdapter mSummaryAdapter;
    protected RecyclerView mPortfolioSummary;

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
        mPortfolioList = (RecyclerView)findViewById(R.id.portfolio_list);
        mPortfolioList.setLayoutManager(new LinearLayoutManager(getContext()));

        mSummaryAdapter = new SummaryAdapter();

        mPortfolioSummary = (RecyclerView)findViewById(R.id.portfolio_view_summary_view);
        // specify a LinearLayoutManager that supports wrap content
        mPortfolioSummary.setLayoutManager(new org.solovyev.android.views.llm.LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mPortfolioSummary.setHasFixedSize(true);
        mPortfolioSummary.setAdapter(mSummaryAdapter);

    }

    public void setPortfolioAdapter(PortfolioAdapter portfolioAdapter) {
        mPortfolioAdapter = portfolioAdapter;
        mPortfolioList.setAdapter(portfolioAdapter);
    }


    public void setTotals(boolean showTotals, Account totals, PerformanceItem performanceItem) {

        mPortfolioSummary.setVisibility(showTotals ? VISIBLE : GONE);

        if (showTotals) {
            mSummaryAdapter.bind(totals, performanceItem);
        }
    }

    private static class SummaryAdapter extends RecyclerView.Adapter<AccountViewHolder> {

        private Account mTotals;
        private PerformanceItem mPerformanceItem;

        public SummaryAdapter() {
        }

        public void bind(Account totals, PerformanceItem performanceItem) {
            mTotals = totals;
            mPerformanceItem = performanceItem;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return 1;
        }

        @Override
        public AccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new AccountViewHolder(parent, null);
        }

        @Override
        public void onBindViewHolder(AccountViewHolder holder, int position) {
            if (mTotals != null) {
                holder.bind(mTotals, mPerformanceItem, 0);
            }
        }

    }

}

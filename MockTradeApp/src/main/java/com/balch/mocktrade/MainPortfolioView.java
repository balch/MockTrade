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
import android.util.AttributeSet;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

import com.balch.android.app.framework.view.BaseView;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountItemView;
import com.balch.mocktrade.portfolio.PerformanceItem;
import com.balch.mocktrade.portfolio.PortfolioAdapter;

public class MainPortfolioView extends LinearLayout implements BaseView {

    protected PortfolioAdapter mPortfolioAdapter;
    protected ExpandableListView mPortfolioList;
    protected AccountItemView mTotalsView;

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
        inflate(getContext(), R.layout.main_portfolio_view, this);
        this.mPortfolioList = (ExpandableListView)findViewById(R.id.portfolio_list);
    }

    public void setPortfolioAdapter(PortfolioAdapter portfolioAdapter) {
        this.mPortfolioAdapter = portfolioAdapter;
        this.mPortfolioList.setAdapter(portfolioAdapter);
    }

    public void expandList() {
        for (int i = 0; i < this.mPortfolioAdapter.getGroupCount(); i++) {
            this.mPortfolioList.expandGroup(i);
        }

        this.mPortfolioList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true;
            }
        });
    }

    public void setTotals(boolean showTotals, Account totals, PerformanceItem performanceItem) {
        if (showTotals) {
            if (this.mTotalsView == null) {
                this.mTotalsView = new AccountItemView(this.getContext());
                this.mPortfolioList.addFooterView(this.mTotalsView);
            }
            mTotalsView.bind(totals, performanceItem, 0);

        } else {
            if (this.mTotalsView != null) {
                this.mPortfolioList.removeFooterView(this.mTotalsView);
                this.mTotalsView = null;
            }
        }
    }


}

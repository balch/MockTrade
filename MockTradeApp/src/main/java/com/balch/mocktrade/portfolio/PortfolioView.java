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

package com.balch.mocktrade.portfolio;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

import com.balch.android.app.framework.view.BaseView;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountItemView;

public class PortfolioView extends LinearLayout implements BaseView {

    public interface PortfolioViewListener {
        void onCreateNewAccount();
    }

    protected PortfolioViewListener portfolioViewListener;
    protected PortfolioAdapter portfolioAdapter;
    protected ExpandableListView portfolioList;
    protected Button createAccountButton;
    protected AccountItemView totalsView;

    public PortfolioView(Context context) {
        super(context);
    }

    public PortfolioView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PortfolioView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void initializeLayout() {
        inflate(getContext(), R.layout.portfolio_view, this);
        this.portfolioList = (ExpandableListView)findViewById(R.id.portfolio_list);

        View headerView = inflate(getContext(), R.layout.portfolio_view_header, null);
        this.portfolioList.addHeaderView(headerView);
        this.portfolioList.setHeaderDividersEnabled(false);
        this.createAccountButton = (Button)headerView.findViewById(R.id.account_add_button);
        this.createAccountButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PortfolioView.this.portfolioViewListener != null) {
                    PortfolioView.this.portfolioViewListener.onCreateNewAccount();
                }
            }
        });
    }

    @Override
    public void destroy() {

    }

    public void setPortfolioAdapter(PortfolioAdapter portfolioAdapter) {
        this.portfolioAdapter = portfolioAdapter;
        this.portfolioList.setAdapter(portfolioAdapter);
    }

    public void explandList() {
        for (int i = 0; i < this.portfolioAdapter.getGroupCount(); i++) {
            this.portfolioList.expandGroup(i);
        }

        this.portfolioList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true;
            }
        });
    }

    public void setTotals(boolean showTotals, Account totals, PerformanceItem performanceItem) {
        if (showTotals) {
            if (this.totalsView == null) {
                this.totalsView = new AccountItemView(this.getContext());
                this.portfolioList.addFooterView(this.totalsView);
            }
            totalsView.bind(totals, performanceItem, 0);

        } else {
            if (this.totalsView != null) {
                this.portfolioList.removeFooterView(this.totalsView);
                this.totalsView = null;
            }
        }
    }

    public void setPortfolioViewListener(PortfolioViewListener portfolioViewListener) {
        this.portfolioViewListener = portfolioViewListener;
    }

}

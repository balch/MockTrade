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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountItemView;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.investment.InvestmentItemView;

import java.util.List;

public class PortfolioAdapter extends BaseExpandableListAdapter {

    public interface PortfolioAdapterListener {
        boolean onLongClickAccount(Account account);
        boolean onLongClickInvestment(Investment investment);
    }
    protected AccountItemView.AccountItemViewListener accountItemViewListener;

    protected Context context;
    protected PortfolioAdapterListener listener;
    protected PortfolioData portfolioData = new PortfolioData();

    public PortfolioAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getGroupCount() {
        return this.portfolioData.getAccounts().size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        List<Investment> investments = this.portfolioData.getInvestments(this.getGroupId(groupPosition));
        return (investments != null) ? investments.size() : 0;
    }

    @Override
    public Account getGroup(int groupPosition) {
        return this.portfolioData.getAccounts().get(groupPosition);
    }

    @Override
    public Investment getChild(int groupPosition, int childPosition) {
        List<Investment> investments = this.portfolioData.getInvestments(this.getGroupId(groupPosition));
        return investments.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return this.getGroup(groupPosition).getId();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return this.getChild(groupPosition,childPosition).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        AccountItemView accountItemView;

        if (convertView == null) {
            accountItemView = new AccountItemView(this.context);

            accountItemView.setLongClickable(true);
            accountItemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    boolean consumed = false;
                    if (listener != null) {
                        consumed = listener.onLongClickAccount(((AccountItemView) v).getAccount());
                    }
                    return consumed;
                }
            });

            if (accountItemViewListener != null) {
                accountItemView.setAccountItemViewListener(accountItemViewListener);
            }
        } else {
            accountItemView = (AccountItemView) convertView;
        }

        Account account = this.getGroup(groupPosition);
        List<Investment> investments = this.portfolioData.getInvestments(account.getId());
        PerformanceItem performanceItem = account.getPerformanceItem(investments);
        accountItemView.bind(account, performanceItem, this.portfolioData.getOpenOrderCount(account.getId()));
        return accountItemView;
    }

    @Override
    public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        InvestmentItemView investmentItemView;

        if (convertView == null) {
            investmentItemView = new InvestmentItemView(this.context);

            investmentItemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    boolean consumed = false;
                    if (listener != null) {
                        Investment investment = ((InvestmentItemView) v).getInvestment();
                        consumed = listener.onLongClickInvestment(investment);
                    }
                    return consumed;
                }
            });
        } else {
            investmentItemView = (InvestmentItemView) convertView;
        }

        Investment investment = this.getChild(groupPosition, childPosition);
        investmentItemView.bind(investment);

        // only allow selling the investment if it is not part of an account strategy
        investmentItemView.setLongClickable(investment.getAccount().getStrategy() == Account.Strategy.NONE);
        return investmentItemView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public void bind(PortfolioData portfolioData) {
        this.portfolioData = portfolioData;
    }

    public void clear() {
        this.portfolioData = new PortfolioData();
    }

    public void setAccountItemViewListener(AccountItemView.AccountItemViewListener accountItemViewListener) {
        this.accountItemViewListener = accountItemViewListener;
    }

    public void setListener(PortfolioAdapterListener listener) {
        this.listener = listener;
    }
}

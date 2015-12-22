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
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountItemView;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.investment.InvestmentItemView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PortfolioAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private static final int VIEW_TYPE_ACCOUNT_HEADER = 0;
    private static final int VIEW_TYPE_PORTFOLIO_ITEM = 1;
    private static final int VIEW_TYPE_PORTFOLIO_SUMMARY = 2;

    public interface PortfolioAdapterListener {
        boolean onLongClickAccount(Account account);
        boolean onLongClickInvestment(Investment investment);
    }
    protected AccountItemView.AccountItemViewListener mAccountItemViewListener;

    protected Context mContext;
    protected PortfolioAdapterListener mPortfolioAdapterListener;
    protected PortfolioData mPortfolioData = new PortfolioData();

    protected int mTotalItemCount = 0;
    protected Map<Integer, Account> mPositionToAccountMap = new TreeMap<>();
    protected Map<Long, Integer> mAccountIdToTotalCountMap = new HashMap<>();

    protected AccountItemView mFooterView;

    public PortfolioAdapter(Context context) {
        mContext = context;
        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                setupInternalStructures();
            }
        });
    }


    public void bind(PortfolioData portfolioData) {
        mPortfolioData = portfolioData;
        notifyDataSetChanged();
    }

    public void clear(boolean notifiy) {
        mPortfolioData = new PortfolioData();
        if (notifiy) {
            notifyDataSetChanged();
        }
    }

    private void setupInternalStructures() {
        mTotalItemCount = 0;
        mPositionToAccountMap.clear();
        mAccountIdToTotalCountMap.clear();

        // setup a couple maps to enable finding the Investment position from
        // the absolute position
        for (Account account : mPortfolioData.getAccounts()) {
            long accountId = account.getId();
            mPositionToAccountMap.put(mTotalItemCount, account);
            mAccountIdToTotalCountMap.put(accountId, mTotalItemCount);
            mTotalItemCount += mPortfolioData.getInvestments(accountId).size() + 1;
        }

        if (mFooterView != null) {
            mTotalItemCount++;
        }
    }

    public void setAccountItemViewListener(AccountItemView.AccountItemViewListener accountItemViewListener) {
        mAccountItemViewListener = accountItemViewListener;
    }

    public void setListener(PortfolioAdapterListener listener) {
        mPortfolioAdapterListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return mPositionToAccountMap.containsKey(position) ?
                VIEW_TYPE_ACCOUNT_HEADER :
                isSummaryViewType(position) ?
                        VIEW_TYPE_PORTFOLIO_SUMMARY : VIEW_TYPE_PORTFOLIO_ITEM;
    }

    private boolean isSummaryViewType(int position) {
        return ((mFooterView != null) && (position == mTotalItemCount - 1));
    }

    @Override
    public int getItemCount() {
        return mTotalItemCount;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case VIEW_TYPE_ACCOUNT_HEADER:
                viewHolder = new AccountItemView(parent, mAccountItemViewListener);
                break;
            case VIEW_TYPE_PORTFOLIO_ITEM:
                viewHolder = new InvestmentItemView(parent);
                break;
            case VIEW_TYPE_PORTFOLIO_SUMMARY:
                viewHolder = mFooterView;
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Account account = mPositionToAccountMap.get(position);
        if (account != null) {
            AccountItemView accountItemView = (AccountItemView) holder;

            List<Investment> investments = mPortfolioData.getInvestments(account.getId());
            PerformanceItem performanceItem = account.getPerformanceItem(investments);

            accountItemView.bind(account, performanceItem, mPortfolioData.getOpenOrderCount(account.getId()));

            View view = accountItemView.itemView;
            view.setLongClickable(true);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    boolean consumed = false;
                    if (mPortfolioAdapterListener != null) {
                        consumed = mPortfolioAdapterListener.onLongClickAccount(account);
                    }
                    return consumed;
                }
            });

        } else if (!isSummaryViewType(position)) {

            Map.Entry<Integer, Account> lastEntry = null;
            for (Map.Entry<Integer, Account> entry : mPositionToAccountMap.entrySet()) {
                if (entry.getKey() > position) {
                    break;
                }

                lastEntry = entry;
            }

            if (lastEntry != null) {
                long accountId = lastEntry.getValue().getId();

                // find the investment position from the absolute position
                int investmentPosition = position - mAccountIdToTotalCountMap.get(accountId)  - 1;
                final Investment investment = mPortfolioData.getInvestments(accountId).get(investmentPosition);

                InvestmentItemView investmentItemView = (InvestmentItemView) holder;
                investmentItemView.bind(investment);

                View view = investmentItemView.itemView;
                view.setLongClickable(true);
                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        boolean consumed = false;
                        if (mPortfolioAdapterListener != null) {
                            consumed = mPortfolioAdapterListener.onLongClickInvestment(investment);
                        }
                        return consumed;
                    }
                });

            }
        }

    }

    public void addFooterView(AccountItemView footerView) {
        if (mFooterView == null) {
            mTotalItemCount++;
        }
        mFooterView = footerView;
        notifyDataSetChanged();
    }

    public void removeFooterView(AccountItemView footerView) {
        if (mFooterView != null) {
            mTotalItemCount--;
        }
        mFooterView = null;
        notifyDataSetChanged();
    }

}

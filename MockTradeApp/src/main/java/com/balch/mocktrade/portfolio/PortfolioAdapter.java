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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.settings.Settings;
import com.balch.mocktrade.shared.PerformanceItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PortfolioAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ACCOUNT_HEADER = 0;
    private static final int VIEW_TYPE_PORTFOLIO_ITEM = 1;
    private static final int VIEW_TYPE_NEW_ACCOUNT_ITEM = 2;

    public interface PortfolioAdapterListener {
        boolean onLongClickAccount(Account account);
        boolean onLongClickInvestment(Investment investment);
        void createNewAccount();
        void createNewDogsAccount();
    }

    private AccountViewHolder.AccountItemViewListener mAccountItemViewListener;

    private final Settings mSettings;

    private PortfolioAdapterListener mPortfolioAdapterListener;
    private PortfolioData mPortfolioData = new PortfolioData();

    private List<Object> mDataList;

    public PortfolioAdapter(Settings settings) {
        mSettings = settings;
    }

    public void bind(PortfolioData portfolioData) {

        mPortfolioData = portfolioData;
        mDataList = new ArrayList<>();

        for (Account account : portfolioData.getAccounts()) {
            mDataList.add(account);
            mDataList.addAll(portfolioData.getInvestments(account.getId()));
        }

        notifyDataSetChanged();
    }

    public void clear(boolean notify) {
        mDataList = null;
        mPortfolioData = null;
        if (notify) {
            notifyDataSetChanged();
        }
    }

    public void setAccountItemViewListener(AccountViewHolder.AccountItemViewListener accountItemViewListener) {
        mAccountItemViewListener = accountItemViewListener;
    }

    public void setListener(PortfolioAdapterListener listener) {
        mPortfolioAdapterListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return isEmpty() ? VIEW_TYPE_NEW_ACCOUNT_ITEM :
                (mDataList.get(position) instanceof Investment) ?
                        VIEW_TYPE_PORTFOLIO_ITEM : VIEW_TYPE_ACCOUNT_HEADER;
    }

    @Override
    public int getItemCount() {
        // ensure that we always have one so we can show the empty view
        return (!isEmpty()) ? mDataList.size() : 1;
    }

    private boolean isEmpty() {
        return ((mDataList == null) || (mDataList.size() == 0));
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case VIEW_TYPE_ACCOUNT_HEADER:
                viewHolder = new AccountViewHolder(parent, mAccountItemViewListener, mSettings);
                break;
            case VIEW_TYPE_PORTFOLIO_ITEM:
                viewHolder = new InvestmentViewHolder(parent);
                break;
            case VIEW_TYPE_NEW_ACCOUNT_ITEM:
                viewHolder = new NewAccountViewHolder(parent, mPortfolioAdapterListener);
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof NewAccountViewHolder) {
        } else if (holder instanceof AccountViewHolder) {
            AccountViewHolder accountViewHolder = (AccountViewHolder) holder;

            final Account account = (Account) mDataList.get(position);

            List<Investment> investments = mPortfolioData.getInvestments(account.getId());
            PerformanceItem performanceItem = account.getPerformanceItem(investments, new Date());

            accountViewHolder.bind(account, performanceItem, mPortfolioData.getOpenOrderCount(account.getId()));

            View view = accountViewHolder.itemView;
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

        } else {

            InvestmentViewHolder investmentItemView = (InvestmentViewHolder) holder;

            final Investment investment = (Investment) mDataList.get(position);
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

    private static class NewAccountViewHolder extends RecyclerView.ViewHolder {

        NewAccountViewHolder(ViewGroup parent, final PortfolioAdapterListener listener) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.portfolio_view_holder_empty, parent, false));

            itemView.findViewById(R.id.portfolio_view_holder_empty_create)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (listener != null) {
                                listener.createNewAccount();
                            }

                        }
                    });

            itemView.findViewById(R.id.portfolio_view_holder_empty_dogs)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (listener != null) {
                                listener.createNewDogsAccount();
                            }

                        }
                    });
        }
    }
}

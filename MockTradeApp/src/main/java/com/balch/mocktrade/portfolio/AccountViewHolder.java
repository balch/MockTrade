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

import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.utils.TextFormatUtils;


public class AccountViewHolder extends RecyclerView.ViewHolder {

    public interface AccountItemViewListener {
        void onTradeButtonClicked(Account account);
        void onShowOpenOrdersClicked(Account account);
    }

    protected AccountItemViewListener mAccountItemViewListener;
    protected TextView mName;
    protected TextView mCurrentBalance;
    protected Button mOpenOrders;
    protected TextView mDayPerformance;
    protected TextView mTotalPerformance;
    protected LinearLayout mValueLayout;
    protected Button mTradeButton;
    protected Account mAccount;

    public AccountViewHolder(ViewGroup parent, AccountItemViewListener listener) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.portfolio_view_holder_account, parent, false));
        mAccountItemViewListener = listener;

        mName = (TextView) itemView.findViewById(R.id.account_item_name);
        mCurrentBalance = (TextView) itemView.findViewById(R.id.account_item_current_balance);
        mDayPerformance = (TextView) itemView.findViewById(R.id.account_item_day_performance);
        mTotalPerformance = (TextView) itemView.findViewById(R.id.account_item_total_performance);
        mTradeButton = (Button) itemView.findViewById(R.id.account_item_trade_button);
        mOpenOrders = (Button) itemView.findViewById(R.id.account_item_open_order_button);
        mValueLayout = (LinearLayout) itemView.findViewById(R.id.account_item_value_layout);

    }

    public void bind(Account account, PerformanceItem performanceItem, int openOrderCount) {
        mAccount = account;
        mName.setText(account.getName());

        mCurrentBalance.setText(performanceItem.getValue().getFormatted());

        int flags = (account.getExcludeFromTotals()) ?
                mName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG :
                mName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG;
        mCurrentBalance.setPaintFlags(flags);


        mTotalPerformance.setText(TextFormatUtils.getLongChangePercentText(itemView.getContext(),
                performanceItem.getTotalChange().getDollars(), performanceItem.getTotalChangePercent(), R.string.total_change_label));
        mDayPerformance.setText(TextFormatUtils.getShortChangeText(itemView.getContext(),
                performanceItem.getTodayChange().getDollars(), R.string.day_change_label));

        boolean allowTrade = ((account.getId() != null) && (account.getStrategy()== Account.Strategy.NONE));
        mTradeButton.setVisibility(allowTrade ? View.VISIBLE : View.GONE);

        mOpenOrders.setVisibility((openOrderCount > 0) ? View.VISIBLE : View.GONE);
        if (openOrderCount > 0) {
            mOpenOrders.setText(String.format(itemView.getResources().getString(R.string.account_item_open_orders_format), openOrderCount));
        }

        this.mTradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAccountItemViewListener != null) {
                    mAccountItemViewListener.onTradeButtonClicked(mAccount);
                }
            }
        });

        this.mOpenOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAccountItemViewListener != null) {
                    mAccountItemViewListener.onShowOpenOrdersClicked(mAccount);
                }
            }
        });

        boolean isTablet = itemView.getResources().getBoolean(R.bool.isTablet);
        if (isTablet) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mValueLayout.getLayoutParams();
            layoutParams.weight = 2;
            mValueLayout.setLayoutParams(layoutParams);
        }
    }

    public Account getAccount() {
        return mAccount;
    }

}


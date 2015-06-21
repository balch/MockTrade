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

package com.balch.mocktrade.account;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.mocktrade.R;
import com.balch.mocktrade.portfolio.PerformanceItem;
import com.balch.mocktrade.utils.TextFormatUtils;


public class AccountItemView extends LinearLayout {

    public interface AccountItemViewListener {
        void onTradeButtonClicked(Account account);
        void onShowOpenOrdersClicked(Account account);
    }

    protected AccountItemViewListener accountItemViewListener;
    protected TextView name;
    protected TextView currentBalance;
    protected Button openOrders;
    protected TextView dayPerformance;
    protected TextView totalPerformance;
    protected LinearLayout valueLayout;
    protected Button   tradeButton;
    protected Account account;

    public AccountItemView(Context context) {
        super(context);
        initialize();
    }

    public AccountItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public AccountItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        inflate(getContext(), R.layout.account_item_view, this);
        this.name = (TextView)findViewById(R.id.account_item_name);
        this.currentBalance = (TextView)findViewById(R.id.account_item_current_balance);
        this.dayPerformance = (TextView)findViewById(R.id.account_item_day_performance);
        this.totalPerformance = (TextView)findViewById(R.id.account_item_total_performance);
        this.tradeButton = (Button)findViewById(R.id.account_item_trade_button);
        this.openOrders = (Button)findViewById(R.id.account_item_open_order_button);
        this.valueLayout = (LinearLayout)findViewById(R.id.account_item_value_layout);

        this.tradeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (accountItemViewListener != null) {
                    accountItemViewListener.onTradeButtonClicked(account);
                }
            }
        });

        this.openOrders.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (accountItemViewListener != null) {
                    accountItemViewListener.onShowOpenOrdersClicked(account);
                }
            }
        });

        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        if (isTablet) {
            LayoutParams layoutParams = (LayoutParams) this.valueLayout.getLayoutParams();
            layoutParams.weight = 2;
            this.valueLayout.setLayoutParams(layoutParams);
        }
    }

    public void bind(Account account, PerformanceItem performanceItem, int openOrderCount) {
        this.account = account;
        this.name.setText(account.getName());
        this.currentBalance.setText(performanceItem.getValue().getFormatted());

        this.name.setTypeface(null, account.getExcludeFromTotals() ? Typeface.BOLD_ITALIC : Typeface.BOLD);
        this.currentBalance.setTypeface(null, account.getExcludeFromTotals() ? Typeface.BOLD_ITALIC : Typeface.BOLD);

        this.totalPerformance.setText(TextFormatUtils.getLongChangePercentText(this.getContext(),
                performanceItem.getTotalChange().getDollars(), performanceItem.getTotalChangePercent(), R.string.total_change_label));
        this.dayPerformance.setText(TextFormatUtils.getShortChangeText(this.getContext(),
                performanceItem.getDailyChange().getDollars(), R.string.day_change_label));

        boolean allowTrade = ((account.getId() != null) && (account.getStrategy()== Account.Strategy.NONE));
        this.tradeButton.setVisibility(allowTrade?VISIBLE:GONE);

        this.openOrders.setVisibility((openOrderCount > 0) ? VISIBLE : GONE);
        if (openOrderCount > 0) {
            this.openOrders.setText(String.format(getResources().getString(R.string.account_item_open_orders_format), openOrderCount));
        }
    }


    public Account getAccount() {
        return this.account;
    }

    public void setAccountItemViewListener(AccountItemViewListener accountItemViewListener) {
        this.accountItemViewListener = accountItemViewListener;
    }
}


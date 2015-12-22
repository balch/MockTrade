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

package com.balch.mocktrade.investment;

import android.content.res.Configuration;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.utils.TextFormatUtils;


public class InvestmentItemView extends RecyclerView.ViewHolder {

    protected TextView mSymbol;
    protected TextView mDescription;
    protected TextView mPrice;
    protected TextView mPerformance;
    protected TextView mValue;
    protected TextView mValueChange;
    protected LinearLayout mValueLayout;
    protected LinearLayout mPriceLayout;
    protected Investment mInvestment;

    public InvestmentItemView(ViewGroup parent) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.investment_item_view, parent, false));
        mSymbol = (TextView) itemView.findViewById(R.id.investment_item_symbol);
        mDescription = (TextView) itemView.findViewById(R.id.investment_item_description);
        mPrice = (TextView) itemView.findViewById(R.id.investment_item_price);
        mPerformance = (TextView) itemView.findViewById(R.id.investment_item_perf);

        mValue = (TextView) itemView.findViewById(R.id.investment_item_value);
        mValueChange = (TextView) itemView.findViewById(R.id.investment_item_value_change);
        mValueLayout = (LinearLayout) itemView.findViewById(R.id.investment_item_value_layout);
        mPriceLayout = (LinearLayout) itemView.findViewById(R.id.investment_item_price_layout);

        boolean showValueLayout = (itemView.getResources().getBoolean(R.bool.isTablet) ||
                (itemView.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE));
        mValueLayout.setVisibility(showValueLayout ? View.VISIBLE : View.GONE);
    }

    public void bind(Investment investment) {
        mInvestment = investment;

        mSymbol.setText(TextUtils.isEmpty(investment.getExchange()) ?
                investment.getSymbol() :
                investment.getExchange() + ": " + investment.getSymbol());
        mDescription.setText(investment.getDescription());

        String price = investment.getPrice().getFormatted();
        if (!investment.isPriceCurrent()) {
            price = price + " **";
        }

        mPrice.setText(price);

        mValue.setText(investment.getValue().getFormatted());

        Money delta = Money.subtract(investment.getPrice(), investment.getPrevDayClose());
        float percent = (investment.getPrice().getMicroCents() != 0) ?
                delta.getMicroCents() / (float) investment.getPrevDayClose().getMicroCents() :
                1.0f;

        mPerformance.setText(TextFormatUtils.getShortChangePercentText(delta.getDollars(), percent*100));

        Money deltaValue = Money.subtract(investment.getValue(), investment.getPrevDayValue());
        mValueChange.setText(TextFormatUtils.getShortChangeText(deltaValue.getDollars()));
    }

    public Investment getInvestment() {
        return mInvestment;
    }

}


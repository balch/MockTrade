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
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.ViewProvider;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.shared.utils.TextFormatUtils;


public class InvestmentViewHolder extends RecyclerView.ViewHolder {

    private TextView mSymbol;
    private TextView mDescription;
    private TextView mPrice;
    private TextView mPerformance;
    private TextView mValue;
    private TextView mValueChange;
    private Investment mInvestment;

    public InvestmentViewHolder(ViewGroup parent, ViewProvider viewProvider) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.portfolio_view_holder_investment, parent, false));
        mSymbol = (TextView) itemView.findViewById(R.id.investment_item_symbol);
        mDescription = (TextView) itemView.findViewById(R.id.investment_item_description);
        mPrice = (TextView) itemView.findViewById(R.id.investment_item_price);
        mPerformance = (TextView) itemView.findViewById(R.id.investment_item_perf);

        mValue = (TextView) itemView.findViewById(R.id.investment_item_value);
        mValueChange = (TextView) itemView.findViewById(R.id.investment_item_value_change);
        LinearLayout valueLayout = (LinearLayout) itemView.findViewById(R.id.investment_item_value_layout);

        Context context = itemView.getContext();
        boolean showValueLayout = (viewProvider.isTablet(context) || viewProvider.isLandscape(context));
        valueLayout.setVisibility(showValueLayout ? View.VISIBLE : View.GONE);
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

        mPerformance.setText(TextFormatUtils.getShortChangePercentText(delta.getDollars(), investment.getTodayChangePercent()));

        Money deltaValue = Money.subtract(investment.getValue(), investment.getPrevDayValue());
        mValueChange.setText(TextFormatUtils.getShortChangeText(deltaValue.getDollars()));
    }

    public Investment getInvestment() {
        return mInvestment;
    }

}


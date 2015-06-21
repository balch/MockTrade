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

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.utils.TextFormatUtils;


public class InvestmentItemView extends LinearLayout {

    protected TextView symbol;
    protected TextView description;
    protected TextView price;
    protected TextView perf;
    protected TextView value;
    protected TextView valueChange;
    protected LinearLayout valueLayout;
    protected LinearLayout priceLayout;
    protected Investment investment;

    public InvestmentItemView(Context context) {
        super(context);
        initialize();
    }

    public InvestmentItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public InvestmentItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        inflate(getContext(), R.layout.investment_item_view, this);
        this.symbol = (TextView) findViewById(R.id.investment_item_symbol);
        this.description = (TextView) findViewById(R.id.investment_item_description);
        this.price = (TextView) findViewById(R.id.investment_item_price);
        this.perf = (TextView) findViewById(R.id.investment_item_perf);

        this.value = (TextView) findViewById(R.id.investment_item_value);
        this.valueChange = (TextView) findViewById(R.id.investment_item_value_change);
        this.valueLayout = (LinearLayout) findViewById(R.id.investment_item_value_layout);
        this.priceLayout = (LinearLayout) findViewById(R.id.investment_item_price_layout);

        boolean showValueLayout = (getResources().getBoolean(R.bool.isTablet) ||
                (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE));
        this.valueLayout.setVisibility(showValueLayout ? VISIBLE : GONE);
    }

    public void bind(Investment investment) {
        this.investment = investment;

        this.symbol.setText(TextUtils.isEmpty(investment.getExchange()) ?
                investment.getSymbol() :
                investment.getExchange() + ": " + investment.getSymbol());
        this.description.setText(investment.getDescription());

        String price = investment.getPrice().getFormatted();
        if (!investment.isPriceCurrent()) {
            price = price + " **";
        }

        this.price.setText(price);

        this.value.setText(investment.getValue().getFormatted());

        Money delta = Money.subtract(investment.getPrice(), investment.getPrevDayClose());
        float percent = (investment.getPrice().getMicroCents() != 0) ?
                delta.getMicroCents() / (float) investment.getPrevDayClose().getMicroCents() :
                1.0f;

        this.perf.setText(TextFormatUtils.getShortChangePercentText(delta.getDollars(), percent*100));

        Money deltaValue = Money.subtract(investment.getValue(), investment.getPrevDayValue());
        this.valueChange.setText(TextFormatUtils.getShortChangeText(deltaValue.getDollars()));
    }

    public Investment getInvestment() {
        return this.investment;
    }

}


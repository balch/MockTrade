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
import android.content.res.Resources;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.mocktrade.R;
import com.balch.mocktrade.shared.PerformanceItem;
import com.balch.mocktrade.shared.utils.TextFormatUtils;


public class SummaryTotalsView extends LinearLayout {

    private TextView mCurrentBalance;
    private TextView mDayPerformance;
    private TextView mTotalPerformance;

    public SummaryTotalsView(Context context) {
        super(context);
        initialize();
    }

    public SummaryTotalsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public SummaryTotalsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        Resources resources = getResources();
        ContextCompat.getColor(getContext(), R.color.summary_totals_background);

        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientation(HORIZONTAL);
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.summary_totals_background));

        int padding = resources.getDimensionPixelSize(R.dimen.summary_total_padding);
        setPadding(padding, padding, padding, padding);

        inflate(getContext(), R.layout.portfolio_view_summary_totals, this);
        mCurrentBalance = (TextView) findViewById(R.id.summary_item_current_balance);
        mDayPerformance = (TextView) findViewById(R.id.summary_item_day_performance);
        mTotalPerformance = (TextView) findViewById(R.id.summary_item_total_performance);
    }

    public void bind(PerformanceItem performanceItem) {
        mCurrentBalance.setText(performanceItem.getValue().getFormatted());
        mTotalPerformance.setText(TextFormatUtils.getLongChangePercentText(getContext(),
                performanceItem.getTotalChange().getDollars(), performanceItem.getTotalChangePercent(), R.string.total_change_label));
        mDayPerformance.setText(TextFormatUtils.getShortChangeText(getContext(),
                performanceItem.getTodayChange().getDollars(), R.string.day_change_label));

    }

}


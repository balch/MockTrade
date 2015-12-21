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

import com.balch.android.app.framework.types.Money;

public class PerformanceItem {
    protected final Money mCostBasis;
    protected final Money mValue;
    protected final Money mTodayChange;

    public PerformanceItem(Money costBasis, Money value, Money todayChange) {
        this.mCostBasis = costBasis;
        this.mValue = value;
        this.mTodayChange = todayChange;
    }

    public Money getCostBasis() {
        return mCostBasis;
    }

    public Money getValue() {
        return mValue;
    }

    public Money getmTodayChange() {
        return mTodayChange;
    }

    public void aggregate(PerformanceItem performanceItem) {
        this.mCostBasis.add(performanceItem.getCostBasis());
        this.mTodayChange.add(performanceItem.getmTodayChange());
        this.mValue.add(performanceItem.getValue());
    }

    public Money getDailyChange() {
        return this.mTodayChange;
    }


    public Money getTotalChange() {
        return Money.subtract(this.mValue, this.mCostBasis);
    }

    public float getTotalChangePercent() {
        float percent = (this.mCostBasis.getMicroCents() != 0) ?
                Money.subtract(this.mValue, this.mCostBasis).getMicroCents() / (float) this.mCostBasis.getMicroCents() :
                1.0f;

        return percent * 100.0f;
    }


}

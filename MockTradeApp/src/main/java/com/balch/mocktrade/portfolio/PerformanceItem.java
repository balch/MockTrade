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
    protected final Money costBasis;
    protected final Money value;
    protected final Money prevDayValue;

    public PerformanceItem(Money costBasis, Money value, Money prevDayValue) {
        this.costBasis = costBasis;
        this.value = value;
        this.prevDayValue = prevDayValue;
    }

    public Money getCostBasis() {
        return costBasis;
    }

    public Money getValue() {
        return value;
    }

    public Money getPrevDayValue() {
        return prevDayValue;
    }

    public void aggregate(PerformanceItem performanceItem) {
        this.costBasis.add(performanceItem.getCostBasis());
        this.prevDayValue.add(performanceItem.getPrevDayValue());
        this.value.add(performanceItem.getValue());
    }

    public Money getDailyChange() {
        return Money.subtract(this.value, this.prevDayValue);
    }

    public float getDailyChangePercent() {
        float percent = (this.prevDayValue.getMicroCents() != 0) ?
                Money.subtract(this.value, this.prevDayValue).getMicroCents() / (float) this.prevDayValue.getMicroCents() :
                1.0f;

        return percent * 100.0f;
    }

    public Money getTotalChange() {
        return Money.subtract(this.value, this.costBasis);
    }

    public float getTotalChangePercent() {
        float percent = (this.costBasis.getMicroCents() != 0) ?
                Money.subtract(this.value, this.costBasis).getMicroCents() / (float) this.costBasis.getMicroCents() :
                1.0f;

        return percent * 100.0f;
    }


}

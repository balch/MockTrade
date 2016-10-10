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

package com.balch.mocktrade.order;

import com.balch.android.app.framework.types.Money;

public class OrderResult {
    private final boolean mSuccess;
    private final Money price;
    private final Money mCost;
    private final Money mProfit;
    private final long mConfirmationId;

    public OrderResult(boolean success, Money price, Money cost, Money profit, long confirmationId) {
        this.mSuccess = success;
        this.price = price;
        this.mCost = cost;
        this.mProfit = profit;
        this.mConfirmationId = confirmationId;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public Money getPrice() {
        return price;
    }

    public Money getCost() {
        return mCost;
    }

    public Money getValue() {
        return Money.multiply(mCost, -1);
    }

    public Money getProfit() {
        return mProfit;
    }

    public long getConfirmationId() {
        return mConfirmationId;
    }
}



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
    protected final boolean success;
    protected final Money price;
    protected final Money cost;
    protected final Money profit;
    protected final long confirmationId;

    public OrderResult(boolean success, Money price, Money cost, Money profit, long confirmationId) {
        this.success = success;
        this.price = price;
        this.cost = cost;
        this.profit = profit;
        this.confirmationId = confirmationId;
    }

    public boolean isSuccess() {
        return success;
    }

    public Money getPrice() {
        return price;
    }

    public Money getCost() {
        return cost;
    }

    public Money getValue() {
        return Money.multiply(cost, -1);
    }

    public Money getProfit() {
        return profit;
    }

    public long getConfirmationId() {
        return confirmationId;
    }
}



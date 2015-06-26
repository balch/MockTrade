/*
 * Author: Balch
 * Created: 3/17/15 7:14 PM
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
 * Copyright (C) 2015
 */

package com.balch.mocktrade.order;

public class OrderExecutionException extends Exception {
    private static final String TAG = OrderExecutionException.class.getSimpleName();

    public OrderExecutionException() {
        super();
    }

    public OrderExecutionException(Exception ex) {
        super(ex);
    }

}

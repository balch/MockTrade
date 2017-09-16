/*
 * Author: Balch
 * Created: 9/11/17 9:54 PM
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
 * Copyright (C) 2017
 *
 */

package com.balch.mocktrade.finance;

import java.util.List;

public class QuoteResult {
    private List<Quote> quotes;
    private String errorMessage;
    private boolean success;

    QuoteResult(boolean success, List<Quote> quotes, String errorMessage) {
        this.quotes = quotes;
        this.errorMessage = errorMessage;
        this.success = success;
    }

    public List<Quote> getQuotes() {
        return quotes;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }
}

/*
 * Author: Balch
 * Created: 9/11/17 8:54 PM
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

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YahooFinanceApi {

    /**
     * Get quotes from Yahoo download service
     * See http://www.jarloo.com/yahoo_finance/
     *
     * s = Symbol
     * x = Exchange
     * l1 = Last Trade
     * p = Previous Close
     * d1 = Last Trade Date
     * t1 = Last Trade Time (EST)
     * d = Dividend Per Share
     * n = Name
     *
     * @param symbols
     * @return
     */
    @GET("d/quotes.csv?f=sxl1pd1t1dn")
    Observable<String> getQuotes(@Query("s") String symbols);
}

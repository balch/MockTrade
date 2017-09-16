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

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BarChartFinanceApi {

    @GET("getQuote.json?mode=R&fields=symbol,name,exchange,lastPrice,tradeTimestamp,close,dividendYieldAnnual,mode")
    Single<QuoteResult> getQuotes(@Query("symbols") String symbols,
                                  @Query("apikey") String appKey);
}

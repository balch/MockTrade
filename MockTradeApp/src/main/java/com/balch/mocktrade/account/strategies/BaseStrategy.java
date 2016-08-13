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

package com.balch.mocktrade.account.strategies;

import android.content.Context;

import com.balch.mocktrade.ModelProvider;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.GoogleFinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.portfolio.PortfolioModel;
import com.balch.mocktrade.portfolio.PortfolioSqliteModel;

import java.util.List;
import java.util.Map;

public abstract class BaseStrategy {
    protected FinanceModel financeModel;
    protected PortfolioModel portfolioModel;
    protected Context context;

    public abstract void initialize(Account account);

    private void init( ModelProvider modelProvider) {
        this.financeModel = new GoogleFinanceModel(modelProvider);
        this.portfolioModel = new PortfolioSqliteModel(modelProvider);
        this.context = modelProvider.getContext();
    }

    // NOTE: No Guarantees!!! This could be called more than once a day or not called at all
    // handlers should be written to accommodate this behavior.  Main purpose of this function is that
    // it gets called less frequently than poll update, so could do more work
    public void dailyUpdate(Account account, List<Investment> investments,
                                     Map<String, Quote> quoteMap) {
    }

    public void pollUpdate(Account account, List<Investment> investments,
                            Map<String, Quote> quoteMap) {

    }

    static public BaseStrategy createStrategy(Class<? extends BaseStrategy> clazz,
              ModelProvider modelProvider) throws IllegalAccessException, InstantiationException {
        BaseStrategy baseStrategy = clazz.newInstance();
        baseStrategy.init(modelProvider);

        return baseStrategy;
    }

}

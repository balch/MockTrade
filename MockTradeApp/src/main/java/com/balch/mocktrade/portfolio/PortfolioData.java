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

import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.investment.Investment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortfolioData {
    protected List<Account> mAccounts;
    protected Map<Long, List<Investment>> mAccountToInvestmentMap;
    protected Map<Long, Integer> mAccountToOpenOrderCountMap;
    protected Date mLastSyncTime;
    protected Date mLastQuoteTime;

    public PortfolioData() {
        mAccounts = new ArrayList<>();
        mAccountToInvestmentMap = new HashMap<>();
        mAccountToOpenOrderCountMap = new HashMap<>();
    }

    public List<Account> getAccounts() {
        return mAccounts;
    }

    public List<Investment> getInvestments(Long accountId) {
        List<Investment> investments = mAccountToInvestmentMap.get(accountId);
        if (investments == null) {
            investments = new ArrayList<>();
        }
        return investments;
    }

    public void addAccount(Account account) {
        this.mAccounts.add(account);
    }

    public void addAccounts(List<Account> accounts) {
        this.mAccounts.addAll(accounts);
    }

    public void addInvestment(Investment investment) {
        Long key = investment.getAccount().getId();
        List<Investment> investments = this.mAccountToInvestmentMap.get(key);
        if (investments == null) {
            investments = new ArrayList<>();
            this.mAccountToInvestmentMap.put(key, investments);
        }
        investments.add(investment);
    }

    public void addInvestments(List<Investment> investments) {
        for (Investment i :investments) {
            this.addInvestment(i);
        }
    }

    public void addToOpenOrderCount(Long accountId) {
        this.mAccountToOpenOrderCountMap.put(accountId, getOpenOrderCount(accountId) + 1);
    }

    public int getOpenOrderCount(Long accountId) {
        return this.mAccountToOpenOrderCountMap.containsKey(accountId) ?
                this.mAccountToOpenOrderCountMap.get(accountId) : 0;
    }

    public Date getLastSyncTime() {
        return mLastSyncTime;
    }

    public void setLastSyncTime(Date lastSyncTime) {
        mLastSyncTime = lastSyncTime;
    }

    public Date getLastQuoteTime() {
        return mLastQuoteTime;
    }

    public void setLastQuoteTime(Date lastQuoteTime) {
        mLastQuoteTime = lastQuoteTime;
    }

}

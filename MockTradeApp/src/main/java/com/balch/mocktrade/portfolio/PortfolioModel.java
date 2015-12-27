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

import android.content.Context;

import com.balch.android.app.framework.model.BaseModel;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.order.OrderExecutionException;
import com.balch.mocktrade.order.OrderResult;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface PortfolioModel extends BaseModel {

    List<Account> getAccounts(boolean allAccounts);

    Account getAccount(long accountID);

    void createAccount(Account account);

    void deleteAccount(Account account);

    List<Investment> getAllInvestments();

    void createOrder(Order order);

    List<Order> getOpenOrders();

    OrderResult attemptExecuteOrder(final Order order, Quote quote) throws IllegalAccessException, OrderExecutionException;

    boolean updateInvestment(Investment investment);

    void processOrders(Context context, boolean forceExecution);

    void scheduleOrderServiceAlarm();

    void scheduleOrderServiceAlarmIfNeeded();

    void createSnapshotTotals(List<Account> accounts, Map<Long, List<Investment>> accountToInvestmentMap);

    Date getLastQuoteTime();
}


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
import android.database.sqlite.SQLiteDatabase;
import android.util.LongSparseArray;

import com.balch.android.app.framework.sql.SqlConnection;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountSqliteModel;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.GoogleFinanceApi;
import com.balch.mocktrade.finance.GoogleFinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.investment.InvestmentSqliteModel;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.order.OrderExecutionException;
import com.balch.mocktrade.order.OrderResult;
import com.balch.mocktrade.order.OrderSqliteModel;
import com.balch.mocktrade.services.OrderService;
import com.balch.mocktrade.settings.Settings;
import com.balch.mocktrade.shared.PerformanceItem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PortfolioSqliteModel implements PortfolioModel {

    private final AccountSqliteModel accountModel;
    private final InvestmentSqliteModel investmentModel;
    private final OrderSqliteModel orderModel;
    private final FinanceModel financeModel;
    private final SnapshotTotalsSqliteModel snapshotTotalsModel;
    private final SqlConnection sqlConnection;

    public PortfolioSqliteModel(Context context, SqlConnection sqlConnection,
                                GoogleFinanceApi googleFinanceApi,
                                Settings settings) {
        this.sqlConnection = sqlConnection;
        this.accountModel = new AccountSqliteModel(context, googleFinanceApi, sqlConnection, settings);
        this.investmentModel = new InvestmentSqliteModel(sqlConnection);
        this.orderModel = new OrderSqliteModel(context, googleFinanceApi, sqlConnection, settings);
        this.snapshotTotalsModel = new SnapshotTotalsSqliteModel(sqlConnection, settings);
        this.financeModel = new GoogleFinanceModel(context, googleFinanceApi, settings);
    }

    @Override
    public List<Account> getAccounts(boolean allAccounts) {
        return accountModel.getAccounts(allAccounts);
    }

    @Override
    public Account getAccount(long accountID) {
        return accountModel.getAccount(accountID);
    }

    @Override
    public void createAccount(Account account) {
        accountModel.createAccount(account);
    }

    @Override
    public void deleteAccount(Account account) {
        accountModel.deleteAccount(account);
    }

    @Override
    public List<Investment> getAllInvestments() {
        return investmentModel.getAllInvestments();
    }

    @Override
    public List<Investment> getInvestments(Long accountId) {
        return investmentModel.getInvestments(accountId);
    }

    @Override
    public void createOrder(Order order) {
        orderModel.createOrder(order);
    }

    public List<Order> getOpenOrders() {
        return orderModel.getOpenOrders();
    }

    @Override
    public boolean updateInvestment(Investment investment) {
        return investmentModel.updateInvestment(investment);
    }

    @Override
    public void processOrders(Context context, boolean forceExecution) {
        if (forceExecution || this.financeModel.isMarketOpen()) {
            context.startService(OrderService.getIntent(context));
        } else {
            this.orderModel.scheduleOrderServiceAlarm(this.financeModel.isMarketOpen());
        }
    }

    @Override
    public void scheduleOrderServiceAlarm() {
        this.orderModel.scheduleOrderServiceAlarm(this.financeModel.isMarketOpen());
    }

    @Override
    public void scheduleOrderServiceAlarmIfNeeded() {
        List<Order> openOrders = this.getOpenOrders();
        if (openOrders.size() > 0) {
            this.scheduleOrderServiceAlarm();
        }
    }

    @Override
    public int purgeSnapshots(int days) {
        return snapshotTotalsModel.purgeSnapshotTable(days);
    }

    @Override
    public void createSnapshotTotals(List<Account> accounts,
                         LongSparseArray<List<Investment>> accountToInvestmentMap) {

        Date now = new Date();

        // the accounts need to be added as an atomic bundle for the sums to add up
        // this loop will set the isChanged to true if any account has totals that
        // are different from last snapshot
        List<PerformanceItem> performanceItems = new ArrayList<>(accounts.size());
        boolean isChanged = false;
        for (Account account : accounts) {

            List<Investment> investments = accountToInvestmentMap.get(account.getId());
            if ((investments != null) && (investments.size() > 0)) {
                PerformanceItem performanceItem = account.getPerformanceItem(investments, now);
                performanceItems.add(performanceItem);

                if (!isChanged) {
                    PerformanceItem lastPerformanceItem = snapshotTotalsModel.getLastSnapshot(account.getId());
                    if ((lastPerformanceItem == null) ||
                            !lastPerformanceItem.getValue().equals(performanceItem.getValue()) ||
                            !lastPerformanceItem.getCostBasis().equals(performanceItem.getCostBasis()) ||
                            !lastPerformanceItem.getTodayChange().equals(performanceItem.getTodayChange())) {
                        isChanged = true;
                    }
                }
            }
        }

        // if there is any change all accounts have to be inserted with this timestamp
        if (isChanged) {
            SQLiteDatabase db = sqlConnection.getWritableDatabase();
            db.beginTransaction();
            try {

                SnapshotMapper snapshotMapper = new SnapshotMapper(true);
                for (PerformanceItem performanceItem : performanceItems) {
                    sqlConnection.insert(snapshotMapper, performanceItem, db);
                }
                db.setTransactionSuccessful();

            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                db.endTransaction();
            }
        }
    }

    @Override
    public Date getLastQuoteTime() {
        return investmentModel.getLastTradeTime();
    }

    @Override
    public List<PerformanceItem> getCurrentSnapshot() {
        return snapshotTotalsModel.getCurrentSnapshot();
    }

    @Override
    public List<PerformanceItem> getCurrentSnapshot(long accountId) {
        return snapshotTotalsModel.getCurrentSnapshot(accountId);
    }

    @Override
    public List<PerformanceItem> getCurrentDailySnapshot(int days) {
        return snapshotTotalsModel.getCurrentDailySnapshot(days);
    }

    @Override
    public List<PerformanceItem> getCurrentDailySnapshot(long accountId, int days) {
        return snapshotTotalsModel.getCurrentDailySnapshot(accountId, days);
    }

    @Override
    public OrderResult attemptExecuteOrder(Order order, Quote quote) throws OrderExecutionException {
        return orderModel.attemptExecuteOrder(order, quote);
    }


}

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

import com.balch.android.app.framework.sql.SqlConnection;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountSqliteModel;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.investment.InvestmentSqliteModel;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.SqliteModel;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.order.OrderExecutionException;
import com.balch.mocktrade.order.OrderResult;
import com.balch.mocktrade.order.OrderSqliteModel;
import com.balch.mocktrade.services.OrderService;
import com.balch.mocktrade.shared.PerformanceItem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PortfolioSqliteModel extends SqliteModel implements PortfolioModel {

    protected AccountSqliteModel accounteModel;
    protected InvestmentSqliteModel investmentModel;
    protected OrderSqliteModel orderModel;
    protected FinanceModel financeModel;
    protected SnapshotTotalsSqliteModel snapshotTotalsModel;

    @Override
    public void initialize(ModelProvider modelProvider) {
        super.initialize(modelProvider);
        this.accounteModel = new AccountSqliteModel(modelProvider);
        this.investmentModel = new InvestmentSqliteModel(modelProvider);
        this.orderModel = new OrderSqliteModel(modelProvider);
        this.snapshotTotalsModel = new SnapshotTotalsSqliteModel(modelProvider);

        // this mPortfolioModel is more generic then the sqlite models above and is
        // registered with the mPortfolioModel factory
        this.financeModel = this.getModelFactory().getModel(FinanceModel.class);
    }

    @Override
    public List<Account> getAccounts(boolean allAccounts) {
        return accounteModel.getAccounts(allAccounts);
    }

    @Override
    public Account getAccount(long accountID) {
        return accounteModel.getAccount(accountID);
    }

    @Override
    public void createAccount(Account account) {
        accounteModel.createAccount(account);
    }

    @Override
    public void deleteAccount(Account account) {
        accounteModel.deleteAccount(account);
    }

    @Override
    public List<Investment> getAllInvestments() {
        return investmentModel.getAllInvestments();
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
            Map<Long, List<Investment>> accountToInvestmentMap) {

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
            SqlConnection sqlConnection = getSqlConnection();
            SQLiteDatabase db = sqlConnection.getWritableDatabase();
            db.beginTransaction();
            try {
                for (PerformanceItem performanceItem : performanceItems) {
                    sqlConnection.insert(snapshotTotalsModel, performanceItem, db);
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
    public OrderResult attemptExecuteOrder(Order order, Quote quote) throws OrderExecutionException {
        return orderModel.attemptExecuteOrder(order, quote);
    }

}

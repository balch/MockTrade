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
import com.balch.mocktrade.investment.SummaryItemMapper;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.SqliteModel;
import com.balch.mocktrade.order.Order;
import com.balch.mocktrade.order.OrderExecutionException;
import com.balch.mocktrade.order.OrderResult;
import com.balch.mocktrade.order.OrderSqliteModel;
import com.balch.mocktrade.services.OrderService;

import java.sql.SQLException;
import java.util.List;

public class PortfolioSqliteModel extends SqliteModel implements PortfolioModel {

    protected AccountSqliteModel accounteModel;
    protected InvestmentSqliteModel investmentModel;
    protected OrderSqliteModel orderModel;
    protected FinanceModel financeModel;

    @Override
    public void initialize(ModelProvider modelProvider) {
        super.initialize(modelProvider);
        this.accounteModel = new AccountSqliteModel(modelProvider);
        this.investmentModel = new InvestmentSqliteModel(modelProvider);
        this.orderModel = new OrderSqliteModel(modelProvider);

        // this mPortfolioModel is more generic then the sqlite models above and is
        // registered with the mPortfolioModel factory
        this.financeModel = this.getModelFactory().getModel(FinanceModel.class);
    }

    @Override
    public List<Account> getAllAccounts() {
        return accounteModel.getAllAccounts();
    }

    @Override
    public Account getAccount(long accountID) {
        return null;
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
    public void createSummaryItem(List<Investment> investments) {

        SqlConnection sqlConnection = getSqlConnection();
        SQLiteDatabase db = sqlConnection.getWritableDatabase();
        db.beginTransaction();
        try {
            SummaryItemMapper mapper = new SummaryItemMapper();
            for (Investment investment : investments) {
                mapper.deleteSummaryItemByTradeTime(investment, db);
                sqlConnection.insert(mapper, investment, db);
            }

            db.setTransactionSuccessful();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public OrderResult attemptExecuteOrder(Order order, Quote quote) throws OrderExecutionException {
        return orderModel.attemptExecuteOrder(order, quote);
    }

}

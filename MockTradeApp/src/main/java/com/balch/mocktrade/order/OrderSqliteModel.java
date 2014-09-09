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

package com.balch.mocktrade.order;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.Transaction;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.investment.InvestmentSqliteModel;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.SqliteModel;

import java.util.ArrayList;
import java.util.List;

public class OrderSqliteModel extends SqliteModel implements OrderModel, OrderManager.OrderManagerListener {
    private static final String TAG = OrderSqliteModel.class.getName();

    protected InvestmentSqliteModel investmentModel;
    protected OrderManager orderManager;

    public OrderSqliteModel() {
    }

    public OrderSqliteModel(ModelProvider modelProvider) {
        super(modelProvider);
        initialize(modelProvider);
    }

    @Override
    public void initialize(ModelProvider modelProvider) {
        super.initialize(modelProvider);
        this.investmentModel = new InvestmentSqliteModel(modelProvider);
        this.orderManager = new OrderManager(modelProvider.getContext(),
                (FinanceModel)getModelFactory().getModel(FinanceModel.class),
                getSettings(), this);
    }

    public List<Order> getOpenOrders() {
        return getOpenOrders(null);
    }

    public List<Order> getOpenOrders(Long accountId) {
        try {
            StringBuilder where = new StringBuilder(Order.SQL_STATUS+"=?");
            List<String> whereArgs = new ArrayList<String>();
            whereArgs.add(Order.OrderStatus.OPEN.name());

            if (accountId != null) {
                where.append(" AND ").append(Order.SQL_ACCOUNT_ID).append("=?");
                whereArgs.add(String.valueOf(accountId));
            }

            return this.getSqlConnection().query(Order.class, where.toString(),
                    whereArgs.toArray(new String[whereArgs.size()]), null);
        } catch (Exception e) {
            Log.e(TAG, "Error in getOpenOrders", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cancelOrder(Order order) {
        try {
            order.setStatus(Order.OrderStatus.CANCELED);
            if (!this.getSqlConnection().update(order)) {
                throw new Exception("Order was not updated");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error in cancelOrder", ex);
            throw new RuntimeException(ex);
        }
    }

    public void createOrder(Order order) {
        try {
            this.getSqlConnection().insert(order);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public OrderResult attemptExecuteOrder(Order order, Quote quote) throws Exception {
        try {
            return orderManager.attemptExecuteOrder(order, quote);
        } catch (Exception ex) {
            if (order != null) {
                order.setStatus(Order.OrderStatus.ERROR);
                getSqlConnection().update(order);
            }
            throw ex;
        }
    }

    public OrderResult executeOrder(Order order, Quote quote, Money price) throws Exception {
        SQLiteDatabase db = getSqlConnection().getWritableDatabase();
        db.beginTransaction();
        try {
            Money cost = order.getCost(price);

            Account account = getSqlConnection().queryById(Account.class, order.getAccount().getId());

            Money transactionCost;
            Transaction.TransactionType transactionType;
            if (order.getAction() == Order.OrderAction.BUY) {
                if (account.getAvailableFunds().getDollars() < cost.getDollars()) {
                    throw new Exception("Insufficient funds");
                }
                transactionType = Transaction.TransactionType.WITHDRAWAL;
            } else {
                transactionType = Transaction.TransactionType.DEPOSIT;
            }
            transactionCost = Money.multiply(cost, -1);
            Transaction transaction = new Transaction(account, transactionCost, transactionType, "Order Id=" + order.getId());
            long transactionId = getSqlConnection().insert(transaction, db);

            account.getAvailableFunds().add(transactionCost);
            if (!getSqlConnection().update(account, db)) {
                throw new Exception("Error updating account");
            }

            Investment investment = investmentModel.getInvestmentBySymbol(order.getSymbol(), order.getAccount().getId());
            if (investment == null) {
                if (order.getAction() == Order.OrderAction.SELL) {
                    throw new Exception("Can't sell and investment you don't own");
                }
                investment = new Investment(account, quote.getSymbol(),
                        Investment.InvestmentStatus.OPEN, quote.getName(), quote.getExchange(),
                        cost, price, order.getQuantity());
                getSqlConnection().insert(investment, db);
            } else {
                if (order.getAction() == Order.OrderAction.SELL) {
                    if (order.getQuantity() > investment.getQuantity()) {
                        throw new Exception("Selling too many shares");
                    }
                }

                investment.aggregateOrder(order, price);
                if (investment.getQuantity() > 0) {
                    if (!getSqlConnection().update(investment, db)) {
                        throw new Exception("Error updating investment");
                    }
                } else {
                    // delete the investment if we sold everything
                    if (!getSqlConnection().delete(investment, db)) {
                        throw new Exception("Error updating investment");
                    }
                }
            }

            order.setStatus(Order.OrderStatus.FULFILLED);
            if (!getSqlConnection().update(order, db)) {
                throw new Exception("Error updating order");
            }

            db.setTransactionSuccessful();

            return new OrderResult(true, price, cost, transactionId);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public Investment getInvestmentBySymbol(String symbol, Long accountId) {
        return investmentModel.getInvestmentBySymbol(symbol, accountId);
    }

    @Override
    public boolean updateOrder(Order order) throws Exception {
        return getSqlConnection().update(order);
    }

    public void scheduleOrderServiceAlarm(boolean isMarketOpen){
        orderManager.scheduleOrderServiceAlarm(isMarketOpen);
    }


    @Override
    public void destroy() {

    }
}

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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.balch.android.app.framework.sql.SqlMapper;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.account.AccountSqliteModel;
import com.balch.mocktrade.account.Transaction;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.investment.Investment;
import com.balch.mocktrade.investment.InvestmentSqliteModel;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.SqliteModel;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OrderSqliteModel extends SqliteModel implements SqlMapper<Order>, OrderModel, OrderManager.OrderManagerListener {
    private static final String TAG = OrderSqliteModel.class.getName();

    public static final String TABLE_NAME = "[order]";

    public static final String COLUMN_ACCOUNT_ID = "account_id";
    public static final String COLUMN_SYMBOL = "symbol";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_ACTION = "action";
    public static final String COLUMN_STRATEGY = "strategy";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_LIMIT_PRICE = "limit_price";
    public static final String COLUMN_STOP_PRICE = "stop_price";
    public static final String COLUMN_STOP_PERCENT = "stop_percent";
    public static final String COLUMN_QUANTITY = "quantity";
    public static final String COLUMN_HIGHEST_PRICE = "highest_price";

    protected InvestmentSqliteModel investmentModel;
    protected AccountSqliteModel accountModel;
    protected OrderManager orderManager;

    public OrderSqliteModel() {
    }

    public OrderSqliteModel(ModelProvider modelProvider) {
        super(modelProvider);
        initialize(modelProvider);
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }


    @Override
    public void initialize(ModelProvider modelProvider) {
        super.initialize(modelProvider);
        this.investmentModel = new InvestmentSqliteModel(modelProvider);
        this.accountModel = new AccountSqliteModel(modelProvider);
        this.orderManager = new OrderManager(modelProvider.getContext(),
                (FinanceModel)getModelFactory().getModel(FinanceModel.class),
                getSettings(), this);
    }

    public List<Order> getOpenOrders() {
        return getOpenOrders(null);
    }

    public List<Order> getOpenOrders(Long accountId) {
        try {
            StringBuilder where = new StringBuilder(COLUMN_STATUS+"=?");
            List<String> whereArgs = new ArrayList<String>();
            whereArgs.add(Order.OrderStatus.OPEN.name());

            if (accountId != null) {
                where.append(" AND ").append(COLUMN_ACCOUNT_ID).append("=?");
                whereArgs.add(String.valueOf(accountId));
            }

            return this.getSqlConnection().query(this, Order.class, where.toString(),
                    whereArgs.toArray(new String[whereArgs.size()]), null);
        } catch (Exception e) {
            Log.e(TAG, "Error in getOpenOrders", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cancelOrder(Order order) throws OrderCancelException {
        try {
            order.setStatus(Order.OrderStatus.CANCELED);

            // only update the order if the status is still open
            // still may need some locking to make sure the order
            // is not executed after is has been canceled
            String where = " AND "+COLUMN_STATUS+"=?";
            String [] whereArgs = new String[]{Order.OrderStatus.OPEN.name()};

            if (!this.getSqlConnection().update(this, order, where, whereArgs,
                    this.getSqlConnection().getWritableDatabase())) {
                throw new OrderCancelException("Order cannot be canceled");
            }
        } catch (OrderCancelException ex) {
            throw ex;
        } catch (Exception ex) {
            Log.e(TAG, "Error in cancelOrder", ex);
            throw new RuntimeException(ex);
        }
    }

    public void createOrder(Order order) {
        try {
            this.getSqlConnection().insert(this, order);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public OrderResult attemptExecuteOrder(Order order, Quote quote) throws OrderExecutionException {
        try {
            return orderManager.attemptExecuteOrder(order, quote);
        } catch (Exception ex) {
            if (order != null) {

                try {
                    order.setStatus(Order.OrderStatus.ERROR);
                    getSqlConnection().update(this, order);
                } catch (IllegalAccessException e) {
                    throw new OrderExecutionException(ex);
                }
            }
            throw new OrderExecutionException(ex);
        }
    }

    public OrderResult executeOrder(Order order, Quote quote, Money price) throws SQLException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = getSqlConnection().getWritableDatabase();
        db.beginTransaction();
        try {
            Money cost = order.getCost(price);
            Money profit = new Money(0);

            Account account = getSqlConnection().queryById(accountModel, Account.class, order.getAccount().getId());

            Money transactionCost;
            Transaction.TransactionType transactionType;
            if (order.getAction() == Order.OrderAction.BUY) {
                if (account.getAvailableFunds().getDollars() < cost.getDollars()) {
                    throw new IllegalAccessException("Insufficient funds");
                }
                transactionType = Transaction.TransactionType.WITHDRAWAL;
            } else {
                transactionType = Transaction.TransactionType.DEPOSIT;
            }
            transactionCost = Money.multiply(cost, -1);
            Transaction transaction = new Transaction(account, transactionCost, transactionType, "Order Id=" + order.getId());
            long transactionId = getSqlConnection().insert(transaction, transaction, db);

            account.getAvailableFunds().add(transactionCost);
            if (!getSqlConnection().update(accountModel, account, db)) {
                throw new IllegalAccessException("Error updating account");
            }

            Investment investment = investmentModel.getInvestmentBySymbol(order.getSymbol(), order.getAccount().getId());
            if (investment == null) {
                if (order.getAction() == Order.OrderAction.SELL) {
                    throw new IllegalAccessException("Can't sell and investment you don't own");
                }
                investment = new Investment(account, quote.getSymbol(),
                        Investment.InvestmentStatus.OPEN, quote.getName(), quote.getExchange(),
                        cost, price, new Date(0), order.getQuantity());
                getSqlConnection().insert(investmentModel, investment, db);
            } else {
                if (order.getAction() == Order.OrderAction.SELL) {
                    if (order.getQuantity() > investment.getQuantity()) {
                        throw new IllegalAccessException("Selling too many shares");
                    }

                    profit = Money.subtract(transactionCost, investment.getCostBasis());
                }

                investment.aggregateOrder(order, price);
                if (investment.getQuantity() > 0) {
                    if (!getSqlConnection().update(investmentModel, investment, db)) {
                        throw new IllegalAccessException("Error updating investment");
                    }
                } else {
                    // delete the investment if we sold everything
                    if (!getSqlConnection().delete(investmentModel, investment, db)) {
                        throw new IllegalAccessException("Error updating investment");
                    }
                }
            }

            order.setStatus(Order.OrderStatus.FULFILLED);
            if (!getSqlConnection().update(this, order, db)) {
                throw new IllegalAccessException("Error updating order");
            }

            db.setTransactionSuccessful();

            return new OrderResult(true, price, cost, profit, transactionId);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public Investment getInvestmentBySymbol(String symbol, Long accountId) {
        return investmentModel.getInvestmentBySymbol(symbol, accountId);
    }

    @Override
    public boolean updateOrder(Order order) throws IllegalAccessException {
        return getSqlConnection().update(this, order);
    }

    public void scheduleOrderServiceAlarm(boolean isMarketOpen){
        orderManager.scheduleOrderServiceAlarm(isMarketOpen);
    }


    @Override
    public void destroy() {

    }

    @Override
    public ContentValues getContentValues(Order order) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_ACCOUNT_ID, order.account.getId());
        values.put(COLUMN_SYMBOL, order.symbol);
        values.put(COLUMN_STATUS, order.status.name());
        values.put(COLUMN_ACTION, order.action.name());
        values.put(COLUMN_STRATEGY, order.strategy.name());
        values.put(COLUMN_DURATION, order.duration.name());
        values.put(COLUMN_LIMIT_PRICE, order.limitPrice.getMicroCents());
        values.put(COLUMN_STOP_PRICE, order.stopPrice.getMicroCents());
        values.put(COLUMN_STOP_PERCENT, order.stopPercent);
        values.put(COLUMN_QUANTITY, order.quantity);
        values.put(COLUMN_HIGHEST_PRICE, order.highestPrice.getMicroCents());

        return values;
    }

    @Override
    public void populate(Order order, Cursor cursor, Map<String, Integer> columnMap) {
        order.setId(cursor.getLong(columnMap.get(COLUMN_ID)));
        order.account = new Account();
        order.account.setId(cursor.getLong(columnMap.get(COLUMN_ACCOUNT_ID)));
        order.symbol = cursor.getString(columnMap.get(COLUMN_SYMBOL));
        order.status = Order.OrderStatus.valueOf(cursor.getString(columnMap.get(COLUMN_STATUS)));
        order.action = Order.OrderAction.valueOf(cursor.getString(columnMap.get(COLUMN_ACTION)));
        order.strategy = Order.OrderStrategy.valueOf(cursor.getString(columnMap.get(COLUMN_STRATEGY)));
        order.duration = Order.OrderDuration.valueOf(cursor.getString(columnMap.get(COLUMN_DURATION)));
        order.limitPrice = new Money(cursor.getLong(columnMap.get(COLUMN_LIMIT_PRICE)));
        order.stopPrice = new Money(cursor.getLong(columnMap.get(COLUMN_STOP_PRICE)));
        order.stopPercent = cursor.getDouble(columnMap.get(COLUMN_STOP_PERCENT));
        order.quantity = cursor.getLong(columnMap.get(COLUMN_QUANTITY));
        order.highestPrice = new Money(cursor.getLong(columnMap.get(COLUMN_HIGHEST_PRICE)));
    }

}

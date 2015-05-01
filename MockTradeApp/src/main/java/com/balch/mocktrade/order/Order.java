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

import com.balch.android.app.framework.MetadataUtils;
import com.balch.android.app.framework.bean.BaseBean;
import com.balch.android.app.framework.bean.BeanEditState;
import com.balch.android.app.framework.bean.annotations.BeanColumnEdit;
import com.balch.android.app.framework.bean.annotations.BeanColumnNew;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;

import java.io.Serializable;
import java.util.Map;

public class Order  extends BaseBean implements Serializable {
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

    public static final String FLD_LIMIT_PRICE = "limitPrice";
    public static final String FLD_STOP_PRICE = "stopPrice";
    public static final String FLD_STOP_PERCENT = "stopPercent";

    protected Account account;

    @BeanColumnEdit(order = 1, state = BeanEditState.READONLY, labelResId = R.string.order_symbol_label, hints = {"MAX_CHARS=32","NOT_EMPTY=true"})
    @BeanColumnNew(order = 1, customControl = StockSymbolControl.class, labelResId = R.string.order_symbol_label, hints = {"MAX_CHARS=32","NOT_EMPTY=true"})
    protected String symbol;

    protected OrderStatus status;

    @BeanColumnEdit(order = 2, state = BeanEditState.READONLY, labelResId = R.string.order_action_label)
    @BeanColumnNew(order = 2, state = BeanEditState.READONLY, labelResId = R.string.order_action_label)
    protected OrderAction action;

    @BeanColumnEdit(order = 3, state = BeanEditState.READONLY, labelResId = R.string.order_strategy_label)
    @BeanColumnNew(order = 3, labelResId = R.string.order_strategy_label)
    protected OrderStrategy strategy;

    protected OrderDuration duration;

    @BeanColumnEdit(order = 4, labelResId = R.string.order_limit_price_label)
    @BeanColumnNew(order = 4, labelResId = R.string.order_limit_price_label, hints = {"INIT_EMPTY=true"})
    protected Money limitPrice;

    @BeanColumnEdit(order = 5, labelResId = R.string.order_stop_price_label)
    @BeanColumnNew(order = 5, labelResId = R.string.order_stop_price_label, hints = {"INIT_EMPTY=true"})
    protected Money stopPrice;

    @BeanColumnEdit(order = 6, labelResId = R.string.order_stop_percent_label, hints = {"PERCENT=true"})
    @BeanColumnNew(order = 6, labelResId = R.string.order_stop_percent_label, hints = {"PERCENT=true","INIT_EMPTY=true"})
    protected Double stopPercent;

    @BeanColumnEdit(order = 7, labelResId = R.string.order_quantity_label, customControl = QuantityPriceControl.class)
    @BeanColumnNew(order = 7, labelResId = R.string.order_quantity_label, customControl = QuantityPriceControl.class)
    protected Long quantity;

    protected Money highestPrice;

    public Order() {
        this.symbol = "";
        this.status = OrderStatus.OPEN;
        this.action = OrderAction.BUY;
        this.strategy = OrderStrategy.MARKET;
        this.duration = OrderDuration.GOOD_TIL_CANCELED;
        this.quantity = new Long(0);
        this.limitPrice = new Money(0);
        this.stopPrice = new Money(0);
        this.stopPercent = new Double(0.0);
        this.highestPrice = new Money(0);
    }

    @Override
    public String getTableName() {
        return Order.TABLE_NAME;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderAction getAction() {
        return action;
    }

    public void setAction(OrderAction action) {
        this.action = action;
    }

    public OrderStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(OrderStrategy strategy) {
        this.strategy = strategy;
    }

    public OrderDuration getDuration() {
        return duration;
    }

    public void setDuration(OrderDuration duration) {
        this.duration = duration;
    }

    public Long getQuantity() {
        return quantity;
    }

    public Long getQuantityDelta() {
        return (this.action==OrderAction.BUY) ?
                this.quantity :
                -this.quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Money getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(Money limitPrice) {
        this.limitPrice = limitPrice;
    }

    public Double getStopPercent() {
        return stopPercent;
    }

    public void setStopPercent(Double stopPercent) {
        this.stopPercent = stopPercent;
    }

    public Money getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(Money stopPrice) {
        this.stopPrice = stopPrice;
    }

    public Money getHighestPrice() {
        return highestPrice;
    }

    public void setHighestPrice(Money highestPrice) {
        this.highestPrice = highestPrice;
    }

    public Money getCost(Money price) {
        return Money.multiply(price, this.getQuantityDelta());
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(COLUMN_ACCOUNT_ID, this.account.getId());
        values.put(COLUMN_SYMBOL, this.symbol);
        values.put(COLUMN_STATUS, this.status.name());
        values.put(COLUMN_ACTION, this.action.name());
        values.put(COLUMN_STRATEGY, this.strategy.name());
        values.put(COLUMN_DURATION, this.duration.name());
        values.put(COLUMN_LIMIT_PRICE, this.limitPrice.getMicroCents());
        values.put(COLUMN_STOP_PRICE, this.stopPrice.getMicroCents());
        values.put(COLUMN_STOP_PERCENT, this.stopPercent);
        values.put(COLUMN_QUANTITY, this.quantity);
        values.put(COLUMN_HIGHEST_PRICE, this.highestPrice.getMicroCents());

        return values;
    }

    @Override
    public void populate(Cursor cursor, Map<String, Integer> columnMap) {
        this.id = cursor.getLong(columnMap.get(COLUMN_ID));
        this.account = new Account();
        this.account.setId(cursor.getLong(columnMap.get(COLUMN_ACCOUNT_ID)));
        this.symbol = cursor.getString(columnMap.get(COLUMN_SYMBOL));
        this.status = OrderStatus.valueOf(cursor.getString(columnMap.get(COLUMN_STATUS)));
        this.action = OrderAction.valueOf(cursor.getString(columnMap.get(COLUMN_ACTION)));
        this.strategy = OrderStrategy.valueOf(cursor.getString(columnMap.get(COLUMN_STRATEGY)));
        this.duration = OrderDuration.valueOf(cursor.getString(columnMap.get(COLUMN_DURATION)));
        this.limitPrice = new Money(cursor.getLong(columnMap.get(COLUMN_LIMIT_PRICE)));
        this.stopPrice = new Money(cursor.getLong(columnMap.get(COLUMN_STOP_PRICE)));
        this.stopPercent = cursor.getDouble(columnMap.get(COLUMN_STOP_PERCENT));
        this.quantity = cursor.getLong(columnMap.get(COLUMN_QUANTITY));
        this.highestPrice = new Money(cursor.getLong(columnMap.get(COLUMN_HIGHEST_PRICE)));
    }


    public enum OrderStatus implements MetadataUtils.EnumResource {
        OPEN,
        FULFILLED,
        ERROR,
        CANCELED;

        @Override
        public int getListResId() {
            return R.array.order_status_display_values;
        }
    }

    public enum OrderAction implements MetadataUtils.EnumResource {
        BUY,
        SELL;

        @Override
        public int getListResId() {
            return R.array.order_type_display_values;
        }
    }

    private static final int FLAG_BUY = (1<<0);
    private static final int FLAG_SELL = (1<<1);
    public enum OrderStrategy implements MetadataUtils.EnumResource {
        MARKET(FLAG_BUY | FLAG_SELL),
        MANUAL(FLAG_BUY | FLAG_SELL),
        LIMIT(FLAG_BUY | FLAG_SELL),
        STOP_LOSS(FLAG_SELL),
        TRAILING_STOP_AMOUNT_CHANGE(FLAG_SELL),
        TRAILING_STOP_PERCENT_CHANGE(FLAG_SELL);


        private int supportedActions;

        private OrderStrategy(int flags) {
            this.supportedActions = flags;
        }

        @Override
        public int getListResId() {
            return R.array.order_strategy_display_values;
        }

        public boolean isBuySuported() {
            return ((this.supportedActions & FLAG_BUY) != 0);
        }

        public boolean isSellSuported() {
            return ((this.supportedActions & FLAG_SELL) != 0);
        }

    }

    public enum OrderDuration implements MetadataUtils.EnumResource {
        GOOD_TIL_CANCELED,
        DAY;

        @Override
        public int getListResId() {
            return R.array.order_duration_display_values;
        }
    }

}




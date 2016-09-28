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

import android.os.Parcel;
import android.os.Parcelable;

import com.balch.android.app.framework.domain.MetadataUtils;
import com.balch.android.app.framework.domain.DomainObject;
import com.balch.android.app.framework.domain.EditState;
import com.balch.android.app.framework.domain.annotations.ColumnEdit;
import com.balch.android.app.framework.domain.annotations.ColumnNew;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;

public class Order extends DomainObject implements Parcelable {

    static final String FLD_LIMIT_PRICE = "limitPrice";
    static final String FLD_STOP_PRICE = "stopPrice";
    static final String FLD_STOP_PERCENT = "stopPercent";
    static final String FLD_SYMBOL = "symbol";
    static final String FLD_ACTION = "action";
    static final String FLD_STRATEGY = "strategy";
    static final String FLD_QUANTITY = "quantity";

    protected Account account;

    @ColumnEdit(order = 1, state = EditState.READONLY, labelResId = R.string.order_symbol_label, hints = {"MAX_CHARS=32","NOT_EMPTY=true"})
    @ColumnNew(order = 1, customControl = StockSymbolLayout.class, labelResId = R.string.order_symbol_label, hints = {"MAX_CHARS=32","NOT_EMPTY=true"})
    protected String symbol;

    protected OrderStatus status;

    @ColumnEdit(order = 2, state = EditState.READONLY, labelResId = R.string.order_action_label)
    @ColumnNew(order = 2, state = EditState.READONLY, labelResId = R.string.order_action_label)
    protected OrderAction action;

    @ColumnEdit(order = 3, state = EditState.READONLY, labelResId = R.string.order_strategy_label)
    @ColumnNew(order = 3, labelResId = R.string.order_strategy_label)
    protected OrderStrategy strategy;

    protected OrderDuration duration;

    @ColumnEdit(order = 4, labelResId = R.string.order_limit_price_label)
    @ColumnNew(order = 4, labelResId = R.string.order_limit_price_label, hints = {"INIT_EMPTY=true"})
    protected Money limitPrice;

    @ColumnEdit(order = 5, labelResId = R.string.order_stop_price_label)
    @ColumnNew(order = 5, labelResId = R.string.order_stop_price_label, hints = {"INIT_EMPTY=true"})
    protected Money stopPrice;

    @ColumnEdit(order = 6, labelResId = R.string.order_stop_percent_label, hints = {"PERCENT=true"})
    @ColumnNew(order = 6, labelResId = R.string.order_stop_percent_label, hints = {"PERCENT=true","INIT_EMPTY=true"})
    protected Double stopPercent;

    @ColumnEdit(order = 7, labelResId = R.string.order_quantity_label, customControl = QuantityPriceLayout.class)
    @ColumnNew(order = 7, labelResId = R.string.order_quantity_label, customControl = QuantityPriceLayout.class)
    protected Long quantity;

    protected Money highestPrice;

    public Order() {
        this.symbol = "";
        this.status = OrderStatus.OPEN;
        this.action = OrderAction.BUY;
        this.strategy = OrderStrategy.MARKET;
        this.duration = OrderDuration.GOOD_TIL_CANCELED;
        this.quantity = 0L;
        this.limitPrice = new Money(0);
        this.stopPrice = new Money(0);
        this.stopPercent = 0.0;
        this.highestPrice = new Money(0);
    }

    protected Order(Parcel in) {
        super(in);
        account = in.readParcelable(Account.class.getClassLoader());
        symbol = in.readString();
        status = (OrderStatus)in.readSerializable();
        action = (OrderAction) in.readSerializable();
        strategy = (OrderStrategy) in.readSerializable();
        duration = (OrderDuration) in.readSerializable();
        limitPrice = in.readParcelable(Money.class.getClassLoader());
        stopPrice = in.readParcelable(Money.class.getClassLoader());
        stopPercent = (Double)in.readSerializable();
        quantity = (Long)in.readSerializable();
        highestPrice = in.readParcelable(Money.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(account, flags);
        dest.writeString(symbol);
        dest.writeSerializable(status);
        dest.writeSerializable(action);
        dest.writeSerializable(strategy);
        dest.writeSerializable(duration);
        dest.writeParcelable(limitPrice, flags);
        dest.writeParcelable(stopPrice, flags);
        dest.writeSerializable(stopPercent);
        dest.writeSerializable(quantity);
        dest.writeParcelable(highestPrice, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Order> CREATOR = new Creator<Order>() {
        @Override
        public Order createFromParcel(Parcel in) {
            return new Order(in);
        }

        @Override
        public Order[] newArray(int size) {
            return new Order[size];
        }
    };

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

    private static final int FLAG_BUY = (1);
    private static final int FLAG_SELL = (1<<1);
    public enum OrderStrategy implements MetadataUtils.EnumResource {
        MARKET(FLAG_BUY | FLAG_SELL),
        MANUAL(FLAG_BUY | FLAG_SELL),
        LIMIT(FLAG_BUY | FLAG_SELL),
        STOP_LOSS(FLAG_SELL),
        TRAILING_STOP_AMOUNT_CHANGE(FLAG_SELL),
        TRAILING_STOP_PERCENT_CHANGE(FLAG_SELL);


        private int supportedActions;

        OrderStrategy(int flags) {
            this.supportedActions = flags;
        }

        @Override
        public int getListResId() {
            return R.array.order_strategy_display_values;
        }

        public boolean isBuySupported() {
            return ((this.supportedActions & FLAG_BUY) != 0);
        }

        public boolean isSellSupported() {
            return ((this.supportedActions & FLAG_SELL) != 0);
        }

    }

    enum OrderDuration implements MetadataUtils.EnumResource {
        GOOD_TIL_CANCELED,
        DAY;

        @Override
        public int getListResId() {
            return R.array.order_duration_display_values;
        }
    }

}




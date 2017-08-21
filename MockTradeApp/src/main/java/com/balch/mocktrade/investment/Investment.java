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

package com.balch.mocktrade.investment;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import com.balch.android.app.framework.core.MetadataUtils;
import com.balch.android.app.framework.core.DomainObject;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;
import com.balch.mocktrade.order.Order;

import java.util.Date;

public class Investment extends DomainObject implements Parcelable {
    public static final String TAG = Investment.class.getSimpleName();

    private Account account;
    private String symbol;
    private InvestmentStatus status;
    private String description;
    private String exchange;
    private Money costBasis;
    private Money price;
    private Date lastTradeTime;
    private Money prevDayClose;
    private long quantity;

    public Investment() {
    }

    public Investment(Account account, String symbol,
                      InvestmentStatus status, String description, String exchange,
                      Money costBasis, Money price, Date lastTradeTime, long quantity) {
        this.account = account;
        this.symbol = symbol;
        this.status = status;
        this.description = description;
        this.exchange = exchange;
        this.costBasis = costBasis;
        this.price = price;
        this.prevDayClose = price.clone();
        this.quantity = quantity;
        this.lastTradeTime = lastTradeTime;
    }

    protected Investment(Parcel in) {
        super(in);
        account = in.readParcelable(Account.class.getClassLoader());
        symbol = in.readString();
        status = InvestmentStatus.valueOf(in.readString());
        description = in.readString();
        exchange = in.readString();
        costBasis = in.readParcelable(Money.class.getClassLoader());
        price = in.readParcelable(Money.class.getClassLoader());
        lastTradeTime = new Date(in.readLong());
        prevDayClose = in.readParcelable(Money.class.getClassLoader());
        quantity = in.readLong();

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(account, flags);
        dest.writeString(symbol);
        dest.writeString(description);
        dest.writeString(exchange);
        dest.writeParcelable(costBasis, flags);
        dest.writeParcelable(price, flags);
        dest.writeLong(lastTradeTime.getTime());
        dest.writeParcelable(prevDayClose, flags);
        dest.writeLong(quantity);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Investment> CREATOR = new Creator<Investment>() {
        @Override
        public Investment createFromParcel(Parcel in) {
            return new Investment(in);
        }

        @Override
        public Investment[] newArray(int size) {
            return new Investment[size];
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

    public InvestmentStatus getStatus() {
        return status;
    }

    public void setStatus(InvestmentStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public Money getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(Money costBasis) {
        this.costBasis = costBasis;
    }

    public Money getPrice() {
        return price;
    }

    public void setPrice(Money price, Date lastTradeTime) {
        this.price = price;
        this.lastTradeTime = lastTradeTime;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public Money getValue() {
        return Money.multiply(this.price, this.quantity);
    }

    public Money getPrevDayValue() {
        return Money.multiply(this.prevDayClose, this.quantity);
    }

    public Money getPrevDayClose() {
        return prevDayClose;
    }

    public void setPrevDayClose(Money prevDayClose) {
        this.prevDayClose = prevDayClose;
    }

    public Date getLastTradeTime() {
        return lastTradeTime;
    }

    public Money getTodayChange() {
        return Money.subtract(getValue(), getPrevDayValue());
    }

    public Money getTotalChange() {
        return Money.subtract(getValue(), this.costBasis);
    }

    public float getTotalChangePercent() {
        float percent = (this.costBasis.getMicroCents() != 0) ?
                Money.subtract(this.getValue(), this.costBasis).getMicroCents() / (float) this.costBasis.getMicroCents() :
                1.0f;

        return percent * 100.0f;
    }

    public float getTodayChangePercent() {
        Money delta = Money.subtract(getPrice(), getPrevDayClose());
        return (getPrevDayClose().getMicroCents() != 0) ?
                delta.getMicroCents() * 100 / (float) getPrevDayClose().getMicroCents() :
                100.0f;
    }

    public void aggregateOrder(Order order, Money price) {
        // note: this work with sell orders!!!
        this.costBasis.add(order.getCost(price));
        this.quantity += order.getQuantityDelta();
    }

    public boolean isPriceCurrent() {
        return DateUtils.isToday(this.lastTradeTime.getTime());
    }

    public enum InvestmentStatus implements MetadataUtils.EnumResource {
        OPEN,
        CLOSED;

        @Override
        public int getListResId() {
            return R.array.order_strategy_display_values;
        }
    }
}
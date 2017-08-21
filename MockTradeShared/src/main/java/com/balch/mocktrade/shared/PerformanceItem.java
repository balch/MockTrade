/*
 * Author: Balch
 * Created: 7/28/16 8:23 AM
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
 * Copyright (C) 2016
 *
 */

package com.balch.mocktrade.shared;

import android.os.Parcel;
import android.os.Parcelable;

import com.balch.android.app.framework.core.DomainObject;
import com.balch.android.app.framework.types.Money;
import com.google.android.gms.wearable.DataMap;

import java.util.Date;

public class PerformanceItem extends DomainObject implements Parcelable {

    private static final String DATA_ACCOUNT_ID = "accountId";
    private static final String DATA_TIMESTAMP = "timestamp";
    private static final String DATA_COST_BASIS = "costBasis";
    private static final String DATA_VALUE = "value";
    private static final String DATA_TODAY_CHANGE = "todayChange";

    private long mAccountId;
    private Date mTimestamp;
    private Money mCostBasis;
    private Money mValue;
    private Money mTodayChange;

    public PerformanceItem() {
    }

    public PerformanceItem(long accountId, Date timestamp,
                           Money costBasis, Money value, Money todayChange) {
        this.mAccountId = accountId;
        this.mTimestamp = timestamp;
        this.mCostBasis = costBasis;
        this.mValue = value;
        this.mTodayChange = todayChange;
    }

    public PerformanceItem(DataMap map) {
        this(map.getLong(DATA_ACCOUNT_ID), new Date(map.getLong(DATA_TIMESTAMP)),
                new Money(map.getLong(DATA_COST_BASIS)), new Money(map.getLong(DATA_VALUE)),
                new Money(map.getLong(DATA_TODAY_CHANGE)));
    }

    protected PerformanceItem(Parcel in) {
        super(in);
        mAccountId = in.readLong();
        mTimestamp = readDate(in);
        mCostBasis = in.readParcelable(Money.class.getClassLoader());
        mValue = in.readParcelable(Money.class.getClassLoader());
        mTodayChange = in.readParcelable(Money.class.getClassLoader());
}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mAccountId);
        writeDate(dest, mTimestamp);
        dest.writeParcelable(mCostBasis, flags);
        dest.writeParcelable(mValue, flags);
        dest.writeParcelable(mTodayChange, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PerformanceItem> CREATOR = new Creator<PerformanceItem>() {
        @Override
        public PerformanceItem createFromParcel(Parcel in) {
            return new PerformanceItem(in);
        }

        @Override
        public PerformanceItem[] newArray(int size) {
            return new PerformanceItem[size];
        }
    };

    public DataMap toDataMap() {
        DataMap map = new DataMap();
        map.putLong(DATA_ACCOUNT_ID, mAccountId);
        map.putLong(DATA_TIMESTAMP, mTimestamp.getTime());
        map.putLong(DATA_COST_BASIS, mCostBasis.getMicroCents());
        map.putLong(DATA_VALUE, mValue.getMicroCents());
        map.putLong(DATA_TODAY_CHANGE, mTodayChange.getMicroCents());
        return map;
    }

    public Money getCostBasis() {
        return mCostBasis;
    }

    public Money getValue() {
        return mValue;
    }

    public Money getTodayChange() {
        return mTodayChange;
    }

    public void aggregate(PerformanceItem performanceItem) {
        this.mCostBasis.add(performanceItem.getCostBasis());
        this.mTodayChange.add(performanceItem.getTodayChange());
        this.mValue.add(performanceItem.getValue());
    }

    public Money getTotalChange() {
        return Money.subtract(this.mValue, this.mCostBasis);
    }

    public float getTotalChangePercent() {
        float percent = (this.mCostBasis.getMicroCents() != 0) ?
                Money.subtract(this.mValue, this.mCostBasis).getMicroCents() / (float) this.mCostBasis.getMicroCents() :
                1.0f;

        return percent * 100.0f;
    }

    public long getAccountId() {
        return mAccountId;
    }

    public Date getTimestamp() {
        return mTimestamp;
    }

    public void setAccountId(long accountId) {
        this.mAccountId = accountId;
    }

    public void setTimestamp(Date timestamp) {
        this.mTimestamp = timestamp;
    }

    public void setCostBasis(Money costBasis) {
        this.mCostBasis = costBasis;
    }

    public void setValue(Money value) {
        this.mValue = value;
    }

    public void setTodayChange(Money todayChange) {
        this.mTodayChange = todayChange;
    }
}

/*
 * Author: Balch
 * Created: 7/31/16 3:30 AM
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

import com.balch.android.app.framework.domain.DomainObject;
import com.balch.android.app.framework.types.Money;
import com.google.android.gms.wearable.DataMap;

public class HighlightItem extends DomainObject implements Parcelable {

    public enum HighlightType {
        TOTAL_OVERALL,
        TOTAL_ACCOUNT,
        PERFORMER_BEST_DAY,
        PERFORMER_WORST_DAY,
        PERFORMER_BEST_TOTAL,
        PERFORMER_WORST_TOTAL
    }

    private static final String DATA_TYPE = "highlightType";
    private static final String DATA_DESC = "description";
    private static final String DATA_SYMBOL = "symbol";
    private static final String DATA_COST_BASIS = "costBasis";
    private static final String DATA_VALUE = "value";
    private static final String DATA_TODAY_CHANGE = "todayChange";
    private static final String DATA_TODAY_CHANGE_PERCENT = "todayChangePercent";
    private static final String DATA_ACCOUNT_ID = "accountId";

    private HighlightType mHighlightType;
    private String mDescription;
    private String mSymbol;
    private Money mCostBasis;
    private Money mValue;
    private Money mTodayChange;
    private float mTodayChangePercent;
    private long mAccountId;

    public HighlightItem(HighlightType highlightType, String description,
                         String symbol, Money costBasis, Money value, Money todayChange,
                        float todayChangePercent, long accountId) {
        this.mHighlightType = highlightType;
        this.mDescription = description;
        this.mSymbol = symbol;
        this.mCostBasis = costBasis;
        this.mValue = value;
        this.mTodayChange = todayChange;
        this.mTodayChangePercent = todayChangePercent;
        this.mAccountId = accountId;
    }

    public HighlightItem(DataMap map) {
        this(HighlightType.valueOf(map.getString(DATA_TYPE )),
                map.getString(DATA_DESC), map.getString(DATA_SYMBOL),
                new Money(map.getLong(DATA_COST_BASIS)), new Money(map.getLong(DATA_VALUE)),
                new Money(map.getLong(DATA_TODAY_CHANGE)), map.getFloat(DATA_TODAY_CHANGE_PERCENT),
                map.getLong(DATA_ACCOUNT_ID, -1));
    }

    private HighlightItem(Parcel in) {
        super(in);
        mHighlightType = HighlightType.valueOf(in.readString());
        mDescription = in.readString();
        mSymbol = in.readString();
        mCostBasis = in.readParcelable(Money.class.getClassLoader());
        mValue = in.readParcelable(Money.class.getClassLoader());
        mTodayChange  = in.readParcelable(Money.class.getClassLoader());
        mTodayChangePercent = in.readFloat();
        mAccountId = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mHighlightType.name());
        dest.writeString(mDescription);
        dest.writeString(mSymbol);
        dest.writeParcelable(mCostBasis, flags);
        dest.writeParcelable(mValue, flags);
        dest.writeParcelable(mTodayChange, flags);
        dest.writeFloat(mTodayChangePercent);
        dest.writeLong(mAccountId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HighlightItem> CREATOR = new Creator<HighlightItem>() {
        @Override
        public HighlightItem createFromParcel(Parcel in) {
            return new HighlightItem(in);
        }

        @Override
        public HighlightItem[] newArray(int size) {
            return new HighlightItem[size];
        }
    };

    public DataMap toDataMap() {
        DataMap map = new DataMap();
        map.putString(DATA_TYPE, mHighlightType.name());
        map.putString(DATA_DESC,  mDescription);
        map.putString(DATA_SYMBOL,  mSymbol);
        map.putLong(DATA_COST_BASIS, mCostBasis.getMicroCents());
        map.putLong(DATA_VALUE, mValue.getMicroCents());
        map.putLong(DATA_TODAY_CHANGE, mTodayChange.getMicroCents());
        map.putFloat(DATA_TODAY_CHANGE_PERCENT, mTodayChangePercent);
        map.putLong(DATA_ACCOUNT_ID, mAccountId);
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

    public Money getTotalChange() {
        return Money.subtract(this.mValue, this.mCostBasis);
    }

    public float getTotalChangePercent() {
        float percent = (this.mCostBasis.getMicroCents() != 0) ?
                Money.subtract(this.mValue, this.mCostBasis).getMicroCents() / (float) this.mCostBasis.getMicroCents() :
                1.0f;

        return percent * 100.0f;
    }

    public float getTodayChangePercent() {
        return mTodayChangePercent;
    }

    public boolean isDayChangeType() {
        return ((mHighlightType == HighlightType.PERFORMER_BEST_DAY) ||
                (mHighlightType == HighlightType.PERFORMER_WORST_DAY));
    }

    public boolean isTotalType() {
        return ((mHighlightType == HighlightType.TOTAL_ACCOUNT) ||
                (mHighlightType == HighlightType.TOTAL_OVERALL));
    }

    public HighlightType getHighlightType() {
        return mHighlightType;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getSymbol() {
        return mSymbol;
    }

    public long getAccountId() {
        return mAccountId;
    }
}

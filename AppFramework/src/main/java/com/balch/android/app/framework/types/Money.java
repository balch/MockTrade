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

package com.balch.android.app.framework.types;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Locale;

public class Money implements Cloneable, Parcelable, Comparable<Money> {
    private static final String TAG = Money.class.getSimpleName();

    protected static final int DOLLAR_TO_MICRO_CENT = 10000;

    // $1 = 10000mc
    private long microCents;
    private Currency currency = Currency.getInstance("USD");

    public Money() {
        this(0L);
    }

    public Money(long microCents) {
        this.setMicroCents(microCents);
    }

    public Money(double dollars) {
        this.setDollars(dollars);
    }

    public Money(String dollars) {
        this.setDollars(dollars);
    }

    protected Money(Parcel in) {
        microCents = in.readLong();
        currency = Currency.getInstance(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(microCents);
        dest.writeString(currency.getCurrencyCode());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Money> CREATOR = new Creator<Money>() {
        @Override
        public Money createFromParcel(Parcel in) {
            return new Money(in);
        }

        @Override
        public Money[] newArray(int size) {
            return new Money[size];
        }
    };

    public long getMicroCents() {
        return microCents;
    }

    public void setMicroCents(long microCents) {
        this.microCents = microCents;
    }

    public double getDollars() {
        return microCents/(double)DOLLAR_TO_MICRO_CENT;
    }

    public void setDollars(double dollars) {
        this.microCents = (long)(dollars * DOLLAR_TO_MICRO_CENT);
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public void setDollars(String dollars) {
        Double val = 0.0;

        if (!TextUtils.isEmpty(dollars)) {
            String symbol = getSymbol();
            if (dollars.startsWith(symbol)) {
                dollars = dollars.substring(symbol.length());
            }

            val = Double.valueOf(dollars.replace(",",""));
        }
        this.setDollars(val);
    }

    public String getFormatted() {
        return getFormatted(2);
    }

    public Spannable getFormattedWithColor() {
        String sign = (microCents >= 0) ? "+" : "-";
        ForegroundColorSpan spanColor = new ForegroundColorSpan((microCents >= 0)? Color.GREEN:Color.RED);

        String val = String.format(Locale.getDefault(), "%s%s%.02f", sign, getSymbol(), Math.abs(microCents)/1000f);
        SpannableStringBuilder spanString = new SpannableStringBuilder(val);
        spanString.setSpan(spanColor, 0, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }


    public String getFormatted(int decimalPlaces) {
        double dollars = getDollars();

        StringBuilder patternBuilder = new StringBuilder(getSymbol());
        patternBuilder.append("#,##0");

        if (decimalPlaces == 1) {
            patternBuilder.append(".0");
        } else if (decimalPlaces >= 2) {
            patternBuilder.append(".00");
            for (int x = 0 ; x < decimalPlaces - 2; x++) {
                patternBuilder.append("#");
            }
        }

        String pattern = patternBuilder.toString();
        DecimalFormat format = new DecimalFormat(pattern + ";-" + pattern);
        return format.format(dollars);
    }

    public String getSymbol() {
        return currency.getSymbol();
    }

    public String getCurrencyNoGroupSep(int decimalPlaces) {
        double dollars = getDollars();
        return String.format("%1$.0"+decimalPlaces+"f", dollars);
    }

    public void multiply(long value) {
        this.microCents *= value;
    }

    public void add(Money money) {
        this.microCents += money.microCents;
    }

    public void subtract(Money money) {
        this.microCents -= money.microCents;
    }

    public static Money multiply(Money money, long quantity) {
        return new Money(money.microCents * quantity);
    }

    public static Money add(Money money1, Money money2) {
        return new Money(money1.microCents + money2.microCents);
    }

    public static Money subtract(Money money1, Money money2) {
        return new Money(money1.microCents - money2.microCents);
    }

    public Money clone() {
        Money clone = null;
        try {
            clone = (Money)super.clone();
        } catch (CloneNotSupportedException e) {
            Log.wtf(TAG, e);
        }
        return clone;
    }

    @Override
    public String toString() {
        return "Money{" +
                "microCents=" + microCents +
                ", currency=" + getFormatted() +
                '}';
    }

    @Override
    public int compareTo(Money another) {
        int compare = 0;
        if (this.microCents != another.microCents) {
            compare = (this.microCents < another.microCents) ? -1 : 1;
        }
        return compare;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Money money = (Money) o;

        if (microCents != money.microCents) return false;
        return !(currency != null ? !currency.equals(money.currency) : money.currency != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (microCents ^ (microCents >>> 32));
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        return result;
    }
}

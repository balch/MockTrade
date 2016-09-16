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

package com.balch.mocktrade.shared.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class TextFormatUtils {
    private final static NumberFormat MONEY_DECIMAL_FORMAT = new DecimalFormat("'$'#,##0.00");
    private final static NumberFormat MONEY_INTEGER_FORMAT = new DecimalFormat("'$'#,###");
    private final static NumberFormat PERCENT_DECIMAL_FORMAT = new DecimalFormat("0.00");
    private final static NumberFormat PERCENT_INTEGER_FORMAT = new DecimalFormat("#,##0");

    public static Spannable getLongChangePercentText(Context context, double dollars, float percent, int labelId) {
        ForegroundColorSpan spanColor = new ForegroundColorSpan((dollars >= 0)? Color.GREEN:Color.RED);

        String label = context.getResources().getString(labelId);

        String val = label + ": " + getDollarString(dollars) + " (" + getPercentString(percent) +")";
        SpannableStringBuilder spanString = new SpannableStringBuilder(val);
        spanString.setSpan(spanColor, label.length()+1, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }

    public static Spannable getShortChangePercentText(double dollars, float percent) {
        ForegroundColorSpan spanColor = new ForegroundColorSpan((dollars >= 0)? Color.GREEN:Color.RED);

        String val = getDollarString(dollars) + "  " + getPercentString(percent);
        SpannableStringBuilder spanString = new SpannableStringBuilder(val);
        spanString.setSpan(spanColor, 0, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        spanString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }

    public static Spannable getShortChangeText(Context context, double dollars, int labelId) {
        ForegroundColorSpan spanColor = new ForegroundColorSpan((dollars >= 0)? Color.GREEN:Color.RED);

        String label = context.getResources().getString(labelId);

        String val = label + ": " + getDollarString(dollars);
        SpannableStringBuilder spanString = new SpannableStringBuilder(val);
        spanString.setSpan(spanColor, label.length()+1, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }

    public static Spannable getShortChangeText(double dollars) {
        ForegroundColorSpan spanColor = new ForegroundColorSpan((dollars >= 0)? Color.GREEN:Color.RED);
        String val = getDollarString(dollars);
        SpannableStringBuilder spanString = new SpannableStringBuilder(val);
        spanString.setSpan(spanColor, 0, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }

    public static String getDollarString(double dollars) {
        NumberFormat format = Math.abs(dollars) < 10000 ? MONEY_DECIMAL_FORMAT : MONEY_INTEGER_FORMAT;
        return format.format(dollars);
    }

    public static String getPercentString(float percent) {
        NumberFormat format = Math.abs(percent) < 100 ? PERCENT_DECIMAL_FORMAT : PERCENT_INTEGER_FORMAT;
        return format.format(percent) + "%";
    }
}

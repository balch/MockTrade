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

package com.balch.mocktrade.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

public class TextFormatUtils {
    static public Spannable getLongChangePercentText(Context context, double dollars, float percent, int labelId) {
        String sign = (dollars >= 0) ? "" : "-";
        ForegroundColorSpan spanColor = new ForegroundColorSpan((dollars >= 0)? Color.GREEN:Color.RED);

        String label = context.getResources().getString(labelId);

        String val = String.format("%s: %s$%,.02f (%s%.2f%%)", label, sign, Math.abs(dollars), sign, Math.abs(percent));
        SpannableStringBuilder spanString = new SpannableStringBuilder(val);
        spanString.setSpan(spanColor, label.length()+1, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }

    static public Spannable getShortChangePercentText(double dollars, float percent) {
        String sign = (dollars >= 0) ? "" : "-";
        ForegroundColorSpan spanColor = new ForegroundColorSpan((dollars >= 0)? Color.GREEN:Color.RED);

        String val = String.format("%s%.2f%%  %s$%,.02f", sign, Math.abs(percent), sign, Math.abs(dollars));
        SpannableStringBuilder spanString = new SpannableStringBuilder(val);
        spanString.setSpan(spanColor, 0, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        spanString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }

    static public Spannable getShotChangeText(Context context, double dollars, int labelId) {
        String sign = (dollars >= 0) ? "" : "-";
        ForegroundColorSpan spanColor = new ForegroundColorSpan((dollars >= 0)? Color.GREEN:Color.RED);

        String label = context.getResources().getString(labelId);

        String val = String.format("%s: %s$%,.02f", label, sign, Math.abs(dollars));
        SpannableStringBuilder spanString = new SpannableStringBuilder(val);
        spanString.setSpan(spanColor, label.length()+1, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }

    static public Spannable getShotChangeText(double dollars) {
        String sign = (dollars >= 0) ? "" : "-";
        ForegroundColorSpan spanColor = new ForegroundColorSpan((dollars >= 0)? Color.GREEN:Color.RED);

        String val = String.format("%s$%,.02f", sign, Math.abs(dollars));
        SpannableStringBuilder spanString = new SpannableStringBuilder(val);
        spanString.setSpan(spanColor, 0, val.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }
}

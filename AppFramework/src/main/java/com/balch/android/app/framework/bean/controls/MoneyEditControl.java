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

package com.balch.android.app.framework.bean.controls;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;

import com.balch.android.app.framework.bean.BeanColumnDescriptor;
import com.balch.android.app.framework.bean.BeanViewHint;
import com.balch.android.app.framework.types.Money;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MoneyEditControl extends StringEditControl {
    protected boolean hideCents = false;

    public MoneyEditControl(Context context) {
        super(context);
    }

    public MoneyEditControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MoneyEditControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void bind(BeanColumnDescriptor descriptor) {
        for (BeanViewHint hint : descriptor.getHints()) {
            if (hint.getHint() == BeanViewHint.Hint.HIDE_CENTS) {
                this.hideCents = hint.getBoolValue();
            }
        }
        super.bind(descriptor);
    }

    @Override
    protected String getValueAsString(Object obj) {
        String value = "";
        if (obj != null) {
            value = ((Money) obj).getCurrencyNoGroupSep(hideCents?0:2);
        }
        return value;
    }

    @Override
    protected List<InputFilter> getInputFilters() {
        List<InputFilter> filters = super.getInputFilters();
        filters.add(new CurrencyFormatInputFilter());
        return filters;
    }

    // adapted from http://stackoverflow.com/questions/7627148/edittext-with-currency-format
    public static class CurrencyFormatInputFilter implements InputFilter {

        protected static Pattern mPattern = Pattern.compile("(0|[1-9]+[0-9]*)?(\\.[0-9]{0,2})?");

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {

            String result = dest.subSequence(0, dstart) +
                            source.toString() +
                            dest.subSequence(dend, dest.length());

            Matcher matcher = mPattern.matcher(result);

            return (!matcher.matches()) ?
                    dest.subSequence(dstart, dend) :
                    null;
        }
    }

    @Override
    public Object getValue() {
        return new Money(super.getValue().toString());
    }
}

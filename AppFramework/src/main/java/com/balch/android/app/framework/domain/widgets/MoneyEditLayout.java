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

package com.balch.android.app.framework.domain.widgets;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;

import com.balch.android.app.framework.R;
import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.ViewHint;
import com.balch.android.app.framework.types.Money;

public class MoneyEditLayout extends StringEditLayout {
    protected boolean hideCents = false;

    public MoneyEditLayout(Context context) {
        super(context);
    }

    public MoneyEditLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MoneyEditLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void initialize() {
        super.initialize();
        this.value.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        this.value.setHint(R.string.control_money_hint);
    }

    @Override
    public void bind(ColumnDescriptor descriptor) {
        for (ViewHint hint : descriptor.getHints()) {
            if (hint.getHint() == ViewHint.Hint.HIDE_CENTS) {
                this.hideCents = hint.getBoolValue();
            }
        }
        super.bind(descriptor);
        this.allowEmpty = false;
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
    public Object getValue() {
        return new Money(super.getValue().toString());
    }
}

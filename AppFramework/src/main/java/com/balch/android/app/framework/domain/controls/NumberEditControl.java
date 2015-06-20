/*
 * Author: Balch
 * Created: 9/9/14 9:57 AM
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

package com.balch.android.app.framework.domain.controls;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.balch.android.app.framework.R;
import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.ViewHint;

public class NumberEditControl extends StringEditControl {

    public NumberEditControl(Context context) {
        super(context);
    }

    public NumberEditControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberEditControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void initialize() {
        super.initialize();
        this.value.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    }

    @Override
    public void bind(ColumnDescriptor descriptor) {
        for (ViewHint hint : descriptor.getHints()) {
            if (hint.getHint() == ViewHint.Hint.PERCENT) {
                this.value.setHint(hint.getBoolValue() ?
                        R.string.control_percent_hint :
                        R.string.control_number_hint);
            }
        }
        super.bind(descriptor);
        this.allowEmpty = false;
    }

    @Override
    protected String getValueAsString(Object obj) {
        String value = "";
        if (obj != null) {
            value = obj.toString();
        }
        return value;
    }

    @Override
    public Object getValue() {
        String val = super.getValue().toString();
        if (TextUtils.isEmpty(val)) {
            val = "0";
        }
        return Double.valueOf(val);
    }
}

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
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.R;
import com.balch.android.app.framework.bean.BeanColumnDescriptor;
import com.balch.android.app.framework.bean.BeanValidatorException;

public class UnsupportedEditControl extends LinearLayout implements BeanEditControl {

    protected BeanColumnDescriptor descriptor;
    protected TextView label;
    protected TextView value;

    public UnsupportedEditControl(Context context) {
        super(context);
        initialize();
    }

    public UnsupportedEditControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public UnsupportedEditControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        inflate(getContext(), R.layout.bean_edit_control_unsupported, this);
        this.label = (TextView)findViewById(R.id.unsupported_edit_control_label);
        this.value = (TextView)findViewById(R.id.unsupported_edit_control_value);
    }

    @Override
    public void bind(BeanColumnDescriptor descriptor) {
        this.descriptor = descriptor;
        this.label.setText(descriptor.getLabelResId());
        this.value.setText("Field type '"+descriptor.getField().getType().getSimpleName()+"' is unsupported");
    }

    @Override
    public BeanColumnDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public void validate() throws BeanValidatorException {

    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void setValue(Object value) {

    }

    @Override
    public void setBeanEditControlListener(BeanEditControlListener listener) {

    }

    @Override
    public void setBeanControlMapper(BeanControlMapper beanControlMapper) {

    }

}

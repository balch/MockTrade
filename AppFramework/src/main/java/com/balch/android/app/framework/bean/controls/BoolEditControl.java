/*
 * Author: Balch
 * Created: 9/16/14 7:35 AM
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
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.balch.android.app.framework.R;
import com.balch.android.app.framework.bean.BeanColumnDescriptor;
import com.balch.android.app.framework.bean.BeanEditState;
import com.balch.android.app.framework.bean.BeanValidatorException;

public class BoolEditControl extends LinearLayout implements BeanEditControl, View.OnClickListener {
    private static final String TAG = BoolEditControl.class.getName();

    protected CheckBox checkBox;

    protected BeanColumnDescriptor descriptor;
    protected BeanEditControlListener beanEditControlListener;
    protected BeanControlMapper beanControlMapper;

    public BoolEditControl(Context context) {
        super(context);
        initialize();
    }

    public BoolEditControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public BoolEditControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        inflate(getContext(), R.layout.bean_edit_control_bool, this);
        this.checkBox = (CheckBox) findViewById(R.id.bool_edit_control_value);

        this.checkBox.setOnClickListener(this);
    }

    @Override
    public void bind(BeanColumnDescriptor descriptor) {
        this.descriptor = descriptor;

        this.checkBox.setText(descriptor.getLabelResId());

        boolean enabled = (descriptor.getState() == BeanEditState.CHANGEABLE);

        try {
            Object val = descriptor.getField().get(descriptor.getItem());
            this.checkBox.setChecked((Boolean)val);
        } catch (IllegalAccessException e) {
            this.checkBox.setText("IllegalAccessException getting value");
            enabled = false;
        }
        this.checkBox.setEnabled(enabled);


    }

    @Override
    public void setBeanControlMapper(BeanControlMapper beanControlMapper) {
        this.beanControlMapper = beanControlMapper;
    }

    @Override
    public void validate() throws BeanValidatorException {
    }


    @Override
    public BeanColumnDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public Object getValue() {
        return new Boolean(this.checkBox.isChecked());
    }

    @Override
    public void setValue(Object value) {
        this.checkBox.setChecked((Boolean)value);
    }

    @Override
    public void setBeanEditControlListener(BeanEditControlListener listener) {
        this.beanEditControlListener = listener;
    }

    @Override
    public void onClick(View v) {
        boolean hasError = false;
        try {
            this.validate();
            this.checkBox.setError(null);
        } catch (BeanValidatorException e) {
            this.checkBox.setError(e.getMessage());
            hasError = true;
        }

        if (this.beanEditControlListener != null) {
            try {
                this.beanEditControlListener.onChanged(this.descriptor, this.getValue(), hasError);
            } catch (BeanValidatorException e) {
                this.checkBox.setError(e.getMessage());
            }
        }
    }
}

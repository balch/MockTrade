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

package com.balch.android.app.framework.domain.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.R;
import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.ValidatorException;

public class UnsupportedEditLayout extends LinearLayout implements EditLayout {

    protected ColumnDescriptor descriptor;
    protected TextView label;
    protected TextView value;

    public UnsupportedEditLayout(Context context) {
        super(context);
        initialize();
    }

    public UnsupportedEditLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public UnsupportedEditLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientation(VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.edit_control_padding);
        setPadding(0, padding, 0, padding);

        inflate(getContext(), R.layout.edit_control_unsupported, this);
        this.label = (TextView)findViewById(R.id.unsupported_edit_control_label);
        this.value = (TextView)findViewById(R.id.unsupported_edit_control_value);
    }

    @Override
    public void bind(ColumnDescriptor descriptor) {
        this.descriptor = descriptor;
        this.label.setText(descriptor.getLabelResId());
        this.value.setText("Field type '"+descriptor.getField().getType().getSimpleName()+"' is unsupported");
    }

    @Override
    public ColumnDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public void validate() throws ValidatorException {

    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void setValue(Object value) {

    }

    @Override
    public void setEditControlListener(EditLayoutListener listener) {

    }

    @Override
    public void setControlMapper(ControlMapper controlMapper) {

    }

}

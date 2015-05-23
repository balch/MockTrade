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

package com.balch.android.app.framework.domain.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.balch.android.app.framework.MetadataUtils;
import com.balch.android.app.framework.R;
import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.EditState;
import com.balch.android.app.framework.domain.ValidatorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnumEditControl extends LinearLayout implements EditControl {
    private static final String TAG = EnumEditControl.class.getName();

    protected TextView label;
    protected Spinner value;

    protected ColumnDescriptor descriptor;
    protected EditControlListener editControlListener;
    protected ControlMapper controlMapper;

    protected List<Object> enumValues;

    public EnumEditControl(Context context) {
        super(context);
        initialize();
    }

    public EnumEditControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public EnumEditControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        inflate(getContext(), R.layout.edit_control_enum, this);
        this.label = (TextView) findViewById(R.id.enum_edit_control_label);
        this.value = (Spinner) findViewById(R.id.enum_edit_control_value);
    }

    @Override
    public void bind(final ColumnDescriptor descriptor) {
        this.descriptor = descriptor;
        this.label.setText(descriptor.getLabelResId());

        this.value.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (editControlListener != null) {
                    try {
                        editControlListener.onChanged(descriptor, getValue(), false);
                    } catch (ValidatorException e) {
                        Log.e(TAG, "Error changing value", e);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        boolean enabled = (descriptor.getState() == EditState.CHANGEABLE);
        try {
            Object obj = descriptor.getField().get(descriptor.getItem());
            List<String> displayValues = new ArrayList<String>();
            Object[] enumValues =  ((Enum)obj).getDeclaringClass().getEnumConstants();
            if (obj instanceof MetadataUtils.EnumResource) {
                int resId = ((MetadataUtils.EnumResource) obj).getListResId();
                displayValues.addAll(Arrays.asList(this.getResources().getStringArray(resId)));
            } else {
                for (Object o : enumValues) {
                    displayValues.add(o.toString());
                }
            }

            setOptions(Arrays.asList(enumValues), displayValues, ((Enum)obj).ordinal());

        } catch (IllegalAccessException e) {
            Log.e(TAG, "Error Creating enum", e);
            enabled = false;
        }
        this.value.setEnabled(enabled);
    }

   public void setOptions(List<Object> enumValues, List<String> displayValues, int selectedIndex) {
       this.enumValues = enumValues;
       ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_item,
               displayValues);
       dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

       this.value.setAdapter(dataAdapter);
       this.value.setSelection(selectedIndex);
   }

    @Override
    public void setControlMapper(ControlMapper controlMapper) {
        this.controlMapper = controlMapper;
    }

    @Override
    public void validate() throws ValidatorException {
        int position = this.value.getSelectedItemPosition();
        // empty string validation
        if (position < 0) {
            throw new ValidatorException(getResources().getString(R.string.error_empty_string));
        }
    }

    @Override
    public ColumnDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public Object getValue() {
        Object obj = null;
        try {
            obj = descriptor.getField().get(descriptor.getItem());
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Error Creating enum", e);
            return null;
        }
        return this.enumValues.get(this.value.getSelectedItemPosition());
    }

    @Override
    public void setValue(Object value) {
        for (int x = 0; x < this.enumValues.size(); x++) {
            if (this.enumValues.get(x) == value) {
                this.value.setSelection(x);
                break;
            }
        }
    }

    @Override
    public void setEditControlListener(EditControlListener listener) {
        this.editControlListener = listener;
    }

}

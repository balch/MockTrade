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

package com.balch.android.app.framework.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import com.balch.android.app.framework.R;

public class TimePickerPreference extends DialogPreference {

    private int hour = 0;
    private int minute = 0;
    private TimePicker picker = null;

    public TimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(R.string.picker_preference_positive);
        setNegativeButtonText(R.string.picker_preference_negative);
    }

    @Override
    protected View onCreateDialogView() {
        this.picker = new TimePicker(getContext());

        return this.picker;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        this.picker.setCurrentHour(this.hour);
        this.picker.setCurrentMinute(this.minute);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            this.hour = this.picker.getCurrentHour();
            this.minute = this.picker.getCurrentMinute();

            String time = String.valueOf(this.hour) + ":" + String.valueOf(this.minute);

            if (callChangeListener(time)) {
                this.persistString(time);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time = null;

        if (restoreValue) {
            time = this.getPersistedString((defaultValue != null)?defaultValue.toString():"00:00");
        }  else {
            time = this.getDefaultValue(defaultValue);
        }

        String[] parts = time.split(":");

        this.hour = Integer.parseInt(parts[0]);
        this.minute = Integer.parseInt(parts[1]);
    }

    protected String getDefaultValue(Object defaultValue) {
        return (defaultValue != null) ? defaultValue.toString() : null;
    }

}

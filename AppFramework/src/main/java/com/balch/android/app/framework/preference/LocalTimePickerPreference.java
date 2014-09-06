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
import android.text.TextUtils;
import android.util.AttributeSet;

import com.balch.android.app.framework.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class LocalTimePickerPreference extends TimePickerPreference {

    protected  static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    protected TimeZone timeZone = TimeZone.getDefault();

    public LocalTimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray attributes = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.LocalTimePicker,
                    0, 0);

            try {
                String tz = attributes.getString(R.styleable.LocalTimePicker_valueTimeZone);

                if (!TextUtils.isEmpty(tz)) {
                    this.timeZone = TimeZone.getTimeZone(tz);
                }


            } finally {
                attributes.recycle();
            }
        }
    }

    @Override
    protected boolean persistString(String value) {
        return super.persistString(toValueTime(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        String value = super.getPersistedString(defaultReturnValue);
        if (value != null) {
            value = toLocalTime(value);
        }
        return value;
    }

    @Override
    protected String getDefaultValue(Object defaultValue) {
        return (defaultValue != null) ? toLocalTime(defaultValue.toString()) : null;
    }

    protected String toLocalTime(String pstTime) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));

        String timeParts[] = pstTime.split(":");
        cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(timeParts[0]));
        cal.set(Calendar.MINUTE, Integer.valueOf(timeParts[1]));

        return TIME_FORMAT.format(cal.getTime());
    }

    protected String toValueTime(String localTime) {
        Calendar cal = Calendar.getInstance();

        String timeParts[] = localTime.split(":");
        cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(timeParts[0]));
        cal.set(Calendar.MINUTE, Integer.valueOf(timeParts[1]));

        Calendar pst = Calendar.getInstance(this.timeZone);
        pst.setTime(cal.getTime());

        return TIME_FORMAT.format(pst.getTime());
    }

}

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

package com.balch.android.app.framework;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ISO8601DateTime {
    private static final String TAG = ISO8601DateTime.class.getSimpleName();

    private static final String ISO_8601_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String ISO_8601_DATE_TIME_FORMAT_1 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'00:00:00Z";

    private ISO8601DateTime() {
    }


    public static String toISO8601(Date date) {
        return toISO8601(date, false);
    }

    public static String toISO8601(Date date, boolean dateOnly) {
        TimeZone tz = dateOnly ? TimeZone.getDefault() : TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat(getDateFormat(dateOnly, null), Locale.getDefault());
        df.setTimeZone(tz);
        return df.format(date);
    }

    public static Date toDate(String iso8601string) throws ParseException {
        String s = iso8601string.replace("Z", "+00:00");
        return new SimpleDateFormat(getDateFormat(false, iso8601string), Locale.getDefault()).parse(s);
    }

    protected static String getDateFormat(boolean dataOnly, String iso8601String) {
        return dataOnly ? ISO_8601_DATE_FORMAT :
                ((iso8601String != null) && iso8601String.contains("."))
                        ? ISO_8601_DATE_TIME_FORMAT_1 : ISO_8601_DATE_TIME_FORMAT;
    }
}

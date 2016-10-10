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

package com.balch.mocktrade.finance;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.balch.mocktrade.receivers.QuoteReceiver;
import com.balch.mocktrade.settings.Settings;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This is package-private on purpose!!! It is intended to contain
 * shared functionality between FinanceModel implementations
 *
 * External classes should use the FinanceModel instead.
 */
class FinanceManager {

    private final Settings mSettings;
    private final Context mContext;

    public FinanceManager(Context context, Settings settings) {
        this.mContext = context.getApplicationContext();
        this.mSettings = settings;
    }

    public boolean isMarketOpen() {
        return (marketOpenCompareValue(false) == 0);
    }

    public boolean isInPollTime() {
        return (marketOpenCompareValue(true) == 0);
    }

    /**
     * Returns a value indicating if the market is open, or if it is before market open or after market close.
     * On Weekends, the returned value is considered after market close
     * @return  -1 - before market open
     *           0 - market is open
     *           1 - after market close
     */
    private int marketOpenCompareValue(boolean usePoll) {
        int val = 0; // assume open
        Calendar now = new GregorianCalendar(mSettings.getSavedSettingsTimeZone());
        if ((now.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) &&
            (now.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)) {

            Calendar startTime = usePoll ? getPollStartTime() : getMarketOpenTime();
            Calendar endTime = usePoll ? getPollEndTime() : getMarketCloseTime();

            if (now.before(startTime)) {
                val = -1;
            } else if (now.after(endTime)) {
                val = 1;
            }
        } else {
            val = 1;
        }

        return val;
    }

    public Date nextMarketOpen() {
        return this.nextMarketOpen(false);
    }

    public Date nextPollStart() {
        return this.nextMarketOpen(true);
    }

    private Date nextMarketOpen(boolean usePoll) {
        Date nextOpen;

        int comapreVal = marketOpenCompareValue(usePoll);
        if (comapreVal == 0) {
            nextOpen = new Date();
        } else {
            Calendar startTime = usePoll ? getPollStartTime() : getMarketOpenTime();
            if (startTime.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                startTime.add(Calendar.DATE, 1);
            }

            if (comapreVal == 1) {
                startTime.add(Calendar.DATE, 1);
            }

            nextOpen = startTime.getTime();

        }
        return nextOpen;
    }

    public void setQuoteServiceAlarm(){
        AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);

        Intent intent = QuoteReceiver.getIntent(mContext);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Date startTime = this.nextPollStart();
        if (startTime == null) {
            startTime = new Date();
        }

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                startTime.getTime(),
                this.mSettings.getPollInterval() * 1000,
                pendingIntent);
    }

    private Calendar getPollStartTime() {
        return getCalendarFromTime(mSettings.geMarketOpenTime(), -15);
    }

    private Calendar getMarketOpenTime() {
        return getCalendarFromTime(mSettings.geMarketOpenTime(), 0);
    }

    private Calendar getPollEndTime() {
        return getCalendarFromTime(mSettings.geMarketCloseTime(), 15);
    }

    private Calendar getMarketCloseTime() {
        return getCalendarFromTime(mSettings.geMarketCloseTime(), 0);
    }

    private Calendar getCalendarFromTime(String time, int offsetMinutes) {
        Calendar cal = new GregorianCalendar(mSettings.getSavedSettingsTimeZone());
        String [] parts = time.split(":");
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        cal.add(Calendar.MINUTE, offsetMinutes);

        return cal;
    }
}

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

    protected final Settings settings;
    protected final Context context;

    public FinanceManager(Context context, Settings settings) {
        this.context = context;
        this.settings = settings;
    }

    public boolean isMarketOpen() {
        return (this.marketOpenCompareValue(false) == 0);
    }

    public boolean isInPollTime() {
        return (this.marketOpenCompareValue(true) == 0);
    }

    /**
     * Returns a value indicating if the market is open, or if it is before market open or after market close.
     * On Weekends, the returned value is considered after market close
     * @return  -1 - before market open
     *           0 - market is open
     *           1 - after market close
     */
    protected int marketOpenCompareValue(boolean usePoll) {
        int val = 0; // assume open
        Calendar now = new GregorianCalendar(settings.getSavedSettingsTimeZone());
        if ((now.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) &&
            (now.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)) {

            Calendar startTime = usePoll ? this.getPollStartTime() : this.getMarketOpenTime();
            Calendar endTime = usePoll ? this.getPollEndTime() : this.getMarketCloseTime();

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

    protected Date nextMarketOpen(boolean usePoll) {
        Date nextOpen;

        int comapreVal = this.marketOpenCompareValue(usePoll);
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
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = QuoteReceiver.getIntent(context);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Date startTime = this.nextPollStart();
        if (startTime == null) {
            startTime = new Date();
        }

        alarmManager.setInexactRepeating(AlarmManager.RTC,
                startTime.getTime(),
                this.settings.getPollInterval() * 1000,
                pendingIntent);
    }

    protected Calendar getPollStartTime() {
        return this.getCalendarFromTime(settings.geMarketOpenTime(), -15);
    }

    protected Calendar getMarketOpenTime() {
        return this.getCalendarFromTime(settings.geMarketOpenTime(), 0);
    }

    protected Calendar getPollEndTime() {
        return this.getCalendarFromTime(settings.geMarketCloseTime(), 15);
    }

    protected Calendar getMarketCloseTime() {
        return this.getCalendarFromTime(settings.geMarketCloseTime(), 0);
    }

    protected Calendar getCalendarFromTime(String time, int offsetMinutes) {
        Calendar cal = new GregorianCalendar(settings.getSavedSettingsTimeZone());
        String [] parts = time.split(":");
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        cal.add(Calendar.MINUTE, offsetMinutes);

        return cal;
    }
}

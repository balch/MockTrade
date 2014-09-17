/*
 * Author: Balch
 * Created: 9/16/14 8:50 PM
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

public class StopWatch {
    protected long startTime = 0;
    protected long stopTime = 0;
    protected boolean running = false;

    public static StopWatch getInstance() {
        return StopWatch.getInstance(true);
    }

    public static StopWatch getInstance(boolean startRunning) {
        StopWatch sw = new StopWatch();
        if (startRunning) {
            sw.start();
        }
        return sw;
    }

    public StopWatch start() {
        this.startTime = System.currentTimeMillis();
        this.running = true;
        return this;
    }

    public long stop() {
        this.stopTime = System.currentTimeMillis();
        this.running = false;
        return this.getElapsedTime();
    }

    public long getElapsedTime() {
        long endTime = this.running ? System.currentTimeMillis() : this.stopTime;
        return endTime - this.startTime;
    }

    public double getElapsedTimeSecs() {
        return getElapsedTime() / 1000.0 ;
    }

}

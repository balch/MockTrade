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

package com.balch.mocktrade.portfolio;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.balch.android.app.framework.sql.SqlConnection;
import com.balch.android.app.framework.sql.SqlMapper;
import com.balch.mocktrade.ModelProvider;
import com.balch.mocktrade.shared.PerformanceItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SnapshotTotalsSqliteModel {
    public static final String TAG = SnapshotTotalsSqliteModel.class.getSimpleName();


    // create SQL to aggregate accounts we want to see in totals
    private static final String SQL_ACCOUNTS_INCLUDED_TOTALS =
            "SELECT -1 AS " + SnapshotMapper.COLUMN_ACCOUNT_ID + ", " +
                    "t1." + SqlMapper.COLUMN_ID + " AS " + SqlMapper.COLUMN_ID + "," +
                    "t1." + SqlMapper.COLUMN_CREATE_TIME + " AS " + SqlMapper.COLUMN_CREATE_TIME + "," +
                    "t1." + SqlMapper.COLUMN_UPDATE_TIME + " AS " + SqlMapper.COLUMN_UPDATE_TIME + "," +
                    "t1." + SnapshotMapper.COLUMN_SNAPSHOT_TIME + " AS " + SnapshotMapper.COLUMN_SNAPSHOT_TIME + "," +
                    " SUM(" + SnapshotMapper.COLUMN_TOTAL_VALUE + ") AS " + SnapshotMapper.COLUMN_TOTAL_VALUE + "," +
                    " SUM(" + SnapshotMapper.COLUMN_COST_BASIS + ") AS " + SnapshotMapper.COLUMN_COST_BASIS + "," +
                    " SUM(" + SnapshotMapper.COLUMN_TODAY_CHANGE + ") AS " + SnapshotMapper.COLUMN_TODAY_CHANGE + " " +
                    " FROM %s AS t1, account AS t2" +
                    " WHERE t1.account_id = t2._id AND t2.exclude_from_totals = 0" +
                    " AND " + SnapshotMapper.COLUMN_SNAPSHOT_TIME + " >= ?" +
                    " AND " + SnapshotMapper.COLUMN_SNAPSHOT_TIME + " < ?" +
                    " GROUP BY " + SnapshotMapper.COLUMN_SNAPSHOT_TIME +
                    " ORDER BY " + SnapshotMapper.COLUMN_SNAPSHOT_TIME + " ASC";

    private static final String SQL_LATEST_VALID_GRAPH_DATE =
            "SELECT MAX(" + SnapshotMapper.COLUMN_SNAPSHOT_TIME + ") AS " + SnapshotMapper.COLUMN_SNAPSHOT_TIME + ", " +
                    "DATE(" + SnapshotMapper.COLUMN_SNAPSHOT_TIME + "/1000, 'unixepoch') AS dt, " +
                    "COUNT(DISTINCT(" + SnapshotMapper.COLUMN_SNAPSHOT_TIME + ")) as readings " +
                    " FROM " + SnapshotMapper.TABLE_NAME +
                    " GROUP BY dt " +
                    " HAVING readings >= 3 " +
                    " ORDER BY dt DESC " +
                    " LIMIT 1";

    private static final String SQL_WHERE_SNAPSHOTS_BY_ACCOUNT_ID =
            SnapshotMapper.COLUMN_ACCOUNT_ID + "=? AND " +
                    SnapshotMapper.COLUMN_SNAPSHOT_TIME + " >= ? AND " +
                    SnapshotMapper.COLUMN_SNAPSHOT_TIME + " < ?";

    private final SqlConnection mSqlConnection;

    public SnapshotTotalsSqliteModel(ModelProvider modelProvider) {
        mSqlConnection = modelProvider.getSqlConnection();
    }

    public PerformanceItem getLastSnapshot(long accountId) {
        String where = SnapshotMapper.COLUMN_ACCOUNT_ID + "=?";
        String[] whereArgs = new String[]{String.valueOf(accountId)};

        PerformanceItem performanceItem = null;
        try {
            List<PerformanceItem> performanceItems =
                    mSqlConnection.query(new SnapshotMapper(true), PerformanceItem.class, where, whereArgs,
                            SnapshotMapper.COLUMN_SNAPSHOT_TIME + " DESC LIMIT 1");
            if ((performanceItems != null) && (performanceItems.size() > 0)) {
                performanceItem = performanceItems.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getLastSnapshot", e);
            throw new RuntimeException(e);
        }

        return performanceItem;
    }

    public List<PerformanceItem> getSnapshots(long accountId, long startTime, long endTimeExclusive) {

        if (accountId < 0) {
            return getSnapshots(startTime, endTimeExclusive);
        }

        String[] whereArgs = new String[]{
                String.valueOf(accountId),
                String.valueOf(startTime),
                String.valueOf(endTimeExclusive)
        };

        List<PerformanceItem> performanceItems;
        try {
            performanceItems =
                    mSqlConnection.query(new SnapshotMapper(true), PerformanceItem.class, SQL_WHERE_SNAPSHOTS_BY_ACCOUNT_ID,
                            whereArgs, SnapshotMapper.COLUMN_SNAPSHOT_TIME + " ASC");
        } catch (Exception e) {
            Log.e(TAG, "Error in getSnapshots(accountId)", e);
            throw new RuntimeException(e);
        }

        return performanceItems;
    }

    public List<PerformanceItem> getSnapshotsByDay(long accountId, long startTime, long endTimeExclusive) {

        if (accountId < 0) {
            return getSnapshotsByDay(startTime, endTimeExclusive);
        }

        String[] whereArgs = new String[]{
                String.valueOf(accountId),
                String.valueOf(startTime),
                String.valueOf(endTimeExclusive)
        };

        List<PerformanceItem> performanceItems;
        try {
            performanceItems =
                    mSqlConnection.query(new SnapshotMapper(false), PerformanceItem.class, SQL_WHERE_SNAPSHOTS_BY_ACCOUNT_ID,
                            whereArgs, SnapshotMapper.COLUMN_SNAPSHOT_TIME + " ASC");
        } catch (Exception e) {
            Log.e(TAG, "Error in getSnapshots(accountId)", e);
            throw new RuntimeException(e);
        }

        return performanceItems;
    }

    public List<PerformanceItem> getSnapshots(long startTime, long endTimeExclusive) {

        String[] whereArgs = new String[]{
                String.valueOf(startTime),
                String.valueOf(endTimeExclusive)
        };

        Cursor cursor = null;
        List<PerformanceItem> performanceItems = new ArrayList<>();
        try {

            cursor = mSqlConnection.getReadableDatabase().rawQuery(
                    String.format(SQL_ACCOUNTS_INCLUDED_TOTALS, SnapshotMapper.TABLE_NAME), whereArgs);
            mSqlConnection.processCursor(new SnapshotMapper(true), cursor, PerformanceItem.class, performanceItems);

        } catch (Exception e) {
            Log.e(TAG, "Error in getSnapshots()", e);
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return performanceItems;
    }

    public List<PerformanceItem> getSnapshotsByDay(long startTime, long endTimeExclusive) {

        String[] whereArgs = new String[]{
                String.valueOf(startTime),
                String.valueOf(endTimeExclusive)
        };

        Cursor cursor = null;
        List<PerformanceItem> performanceItems = new ArrayList<>();
        try {

            cursor = mSqlConnection.getReadableDatabase().rawQuery(
                    String.format(SQL_ACCOUNTS_INCLUDED_TOTALS, SnapshotMapper.TABLE_NAME_SNAPSHOT_DAILY), whereArgs);
            mSqlConnection.processCursor(new SnapshotMapper(false), cursor, PerformanceItem.class, performanceItems);

        } catch (Exception e) {
            Log.e(TAG, "Error in getSnapshotsByDay()", e);
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return performanceItems;
    }

    /**
     * Returns the latest timestamp that can be graphed. This is based on the timestamp
     * having at least 3 distinct readings for the day
     */
    public long getLatestGraphSnapshotTime() {

        Cursor cursor = null;
        long latestTimestamp = 0;
        try {

            cursor = mSqlConnection.getReadableDatabase().rawQuery(SQL_LATEST_VALID_GRAPH_DATE, new String[]{});
            if (cursor.moveToNext()) {
                latestTimestamp = cursor.getLong(0);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in getLatestGraphSnapshotTime()", e);
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return latestTimestamp;
    }

    public int purgeSnapshotTable(int days) {
        SQLiteDatabase db = mSqlConnection.getWritableDatabase();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        long timestamp = cal.getTimeInMillis();

        return db.delete(SnapshotMapper.TABLE_NAME, SnapshotMapper.COLUMN_SNAPSHOT_TIME + "<=?", new String[]{String.valueOf(timestamp)});
    }

    public List<PerformanceItem> getCurrentSnapshot() {
        return getCurrentSnapshot(-1);
    }

    public List<PerformanceItem> getCurrentSnapshot(long accountId) {
        List<PerformanceItem> snapshot = null;

        long latestTimestamp = getLatestGraphSnapshotTime();
        if (latestTimestamp > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(latestTimestamp);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            long startTime = cal.getTimeInMillis();

            cal.add(Calendar.DAY_OF_YEAR, 1);
            long endTime = cal.getTimeInMillis();

            snapshot = getSnapshots(accountId, startTime, endTime);
        }
        return snapshot;
    }


    public List<PerformanceItem> getCurrentDailySnapshot(int days) {
        return getCurrentDailySnapshot(-1, days);
    }

    public List<PerformanceItem> getCurrentDailySnapshot(long accountId, int days) {
        List<PerformanceItem> snapshot = null;

        long latestTimestamp = getLatestGraphSnapshotTime();
        if (latestTimestamp > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(latestTimestamp);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.DAY_OF_YEAR, -days);
            long startTime = cal.getTimeInMillis();

            cal.add(Calendar.DAY_OF_YEAR, days + 1);
            long endTime = cal.getTimeInMillis();

            snapshot = getSnapshotsByDay(accountId, startTime, endTime);
        }
        return snapshot;
    }

}

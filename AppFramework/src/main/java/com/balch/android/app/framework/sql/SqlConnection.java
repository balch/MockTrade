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

package com.balch.android.app.framework.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.balch.android.app.framework.StopWatch;
import com.balch.android.app.framework.domain.DomainObject;
import com.balch.android.app.framework.types.ISO8601DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class SqlConnection extends SQLiteOpenHelper {
    private static final String TAG = SqlConnection.class.getSimpleName();

    protected final Context context;
    protected final String createScript;
    protected final String updateScript;

    public SqlConnection(Context context, String databaseName, int version,
                         String createScript, String updateScript) {
        super(context, databaseName, null, version);
        this.context = context;
        this.createScript = createScript;
        this.updateScript = updateScript;
    }

    public <T extends DomainObject> T queryById(SqlMapper mapper, Class<T> clazz, Long id) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, SQLException {
        List<T> items = this.query(mapper, clazz, SqlMapper.COLUMN_ID+"=?", new String[]{String.valueOf(id)}, null);
        return (items.size() == 1) ? items.get(0) : null;
    }

    public <T extends DomainObject> List<T> query(SqlMapper mapper, Class<T> clazz, String where, String[] whereArgs, String orderBy) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, SQLException {

        StopWatch sw = null;
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            sw = StopWatch.getInstance();
        }

        List<T> results = new ArrayList<T>();

        Constructor<T> ctor = clazz.getConstructor();
        String table = mapper.getTableName();

        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase().query(table, null, where, whereArgs, null, null, orderBy);
            Map<String, Integer> columnMap = getColumnMap(cursor);
            while (cursor.moveToNext()) {
                T item = ctor.newInstance();
                ISO8601DateTime iso8601DateTime;
                try {
                    iso8601DateTime = new ISO8601DateTime(cursor.getString(columnMap.get(SqlMapper.COLUMN_CREATE_TIME)));
                } catch (ParseException ex) {
                    iso8601DateTime = new ISO8601DateTime();
                    Log.e(TAG, "Error reading CreateTime time", ex);
                }
                item.setCreateTime(iso8601DateTime);

                try {
                    iso8601DateTime = new ISO8601DateTime(cursor.getString(columnMap.get(SqlMapper.COLUMN_UPDATE_TIME)));
                } catch (ParseException ex) {
                    iso8601DateTime = new ISO8601DateTime();
                    Log.e(TAG, "Error reading UpdateTime time", ex);
                }
                item.setUpdateTime(iso8601DateTime);

                mapper.populate(item, cursor, columnMap);
                results.add(item);

            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (sw != null) {
            long elapsedMs = sw.stop();
            Log.d(TAG, String.format("SqlConnection.query on %s took %d ms to return %d rows", table, elapsedMs, results.size()));
        }

        return results;
    }

    public long insert(SqlMapper mapper, DomainObject item) throws SQLException {
        return insert(mapper, item, this.getWritableDatabase());
    }

    public long insert(SqlMapper mapper, DomainObject item, SQLiteDatabase db) throws SQLException {
        ContentValues values = mapper.getContentValues(item);

        ISO8601DateTime now = new ISO8601DateTime();
        values.put(SqlMapper.COLUMN_CREATE_TIME, now.toString());
        values.put(SqlMapper.COLUMN_UPDATE_TIME, now.toString());

        long id = db.insert(mapper.getTableName(), null, values);
        if (id == -1) {
            throw new SQLException("Error inserting record");
        }

        item.setId(id);
        return id;
    }

    public boolean update(SqlMapper mapper, DomainObject item)  {
        return update(mapper, item, null, null, this.getWritableDatabase());
    }

    public boolean update(SqlMapper mapper, DomainObject item, SQLiteDatabase db)  {
        return update(mapper, item, null, null, db);
    }

    public boolean update(SqlMapper mapper, DomainObject item, String extraWhere, String [] whereArgs, SQLiteDatabase db)  {

        ContentValues values = mapper.getContentValues(item);

        ISO8601DateTime now = new ISO8601DateTime();
        values.put(SqlMapper.COLUMN_UPDATE_TIME, now.toString());

        StringBuilder where = new StringBuilder("_id=?");
        List<String> whereArgList = new ArrayList<String>();
        whereArgList.add(item.getId().toString());

        if (!TextUtils.isEmpty(extraWhere)) {
            where.append(" ").append(extraWhere);
            if (whereArgs != null) {
                for (String s : whereArgs) {
                    whereArgList.add(s);
                }
            }
        }
        int count = db.update(mapper.getTableName(), values, where.toString(),
                whereArgList.toArray(new String[whereArgList.size()]));
        return (count == 1);
    }

    public boolean delete(SqlMapper mapper, DomainObject item)  {
        return delete(mapper, item, this.getWritableDatabase());
    }

    public boolean delete(SqlMapper mapper, DomainObject item, SQLiteDatabase db)  {
        return (db.delete(mapper.getTableName(), "_id=?", new String[]{item.getId().toString()}) == 1);
    }

/////////////////////////////////

    @Override
    public void onCreate(SQLiteDatabase db) {
        executeScript(this.createScript, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, String.format("onUpgrade(%d -> %d)", oldVersion, newVersion));
        for (int x = oldVersion+1; x <= newVersion; x++) {
            executeScript(String.format(this.updateScript, x), db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    protected void executeScript(String scriptName, SQLiteDatabase db) {
        String script = getScript(scriptName);
        if (script == null) {
            throw new IllegalArgumentException("Error loading " + scriptName);
        }

        String[] statements = script.split(";");
        for (String s : statements) {
            String sql = s.trim();

            if (!sql.isEmpty()) {
                Log.d(TAG, "Executing\n" + sql);
                db.execSQL(sql + ";");
            }
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db){
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    protected String getScript(String scriptName) {
        Log.d(TAG, "getting Script contents for " + scriptName);

        InputStream inputStream = null;
        String sql = null;
        try {
            inputStream = this.context.getAssets().open(scriptName);

            StringBuilder sqlBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                sqlBuilder.append(new String(buffer, 0, length));
            }

            sql = sqlBuilder.toString();
        } catch (Exception e) {
            Log.e(TAG, "getScript", e);
            throw new IllegalArgumentException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "getScript", e);
                }
            }
        }
        return sql;
    }

    private Map<String, Integer> getColumnMap(Cursor cursor) {
        Map<String, Integer> columnMap = new Hashtable<>(cursor.getColumnCount());

        for (int x = 0; x < cursor.getColumnCount(); x++) {
            columnMap.put(cursor.getColumnName(x), x);
        }

        return columnMap;
    }


}

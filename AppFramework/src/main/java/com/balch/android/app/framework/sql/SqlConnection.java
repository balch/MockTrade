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

import com.balch.android.app.framework.MetadataUtils;
import com.balch.android.app.framework.StopWatch;
import com.balch.android.app.framework.bean.BaseBean;
import com.balch.android.app.framework.sql.annotations.SqlColumn;
import com.balch.android.app.framework.types.ISO8601DateTime;
import com.balch.android.app.framework.types.Money;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlConnection extends SQLiteOpenHelper {
    private static final String TAG = SqlConnection.class.getSimpleName();

    protected Map<String, String> sqlColumnCache = new HashMap<String, String>();

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

    public <T extends BaseBean> T queryById(Class<T> clazz, Long id) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, SQLException {
        List<T> items = this.query(clazz, BaseBean._ID+"=?", new String[]{String.valueOf(id)}, null);
        return (items.size() == 1) ? items.get(0) : null;
    }

    public <T extends BaseBean> List<T> query(Class<T> clazz, String where, String[] whereArgs, String orderBy) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, SQLException {

        StopWatch sw = null;
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            sw = StopWatch.getInstance();
        }

        List<T> results = new ArrayList<T>();

        Constructor<T> ctor = clazz.getConstructor();
        String table = ctor.newInstance().getTableName();

        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase().query(table, null, where, whereArgs, null, null, orderBy);

            SetFieldFromCursorHandler setFieldFromCursorHandler = new SetFieldFromCursorHandler(cursor);

            List<Field> fields = MetadataUtils.getAllFields(clazz);
            while (cursor.moveToNext()) {
                T item = ctor.newInstance();

                for (final Field field : fields) {
                    setFieldFromCursor(field, item, setFieldFromCursorHandler);
                }

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

    public long insert(BaseBean bean) throws SQLException, IllegalAccessException {
        return insert(bean, this.getWritableDatabase());
    }

    public long insert(BaseBean bean, SQLiteDatabase db) throws SQLException, IllegalAccessException {
        ISO8601DateTime now = new ISO8601DateTime();
        bean.setCreateTime(now);
        bean.setUpdateTime(now);
        ContentValues values = getContentValues(bean);

        bean.setId(db.insert(bean.getTableName(), null, values));
        if (bean.getId() == -1) {
            throw new SQLException("Error inserting record");
        }

        return bean.getId();
    }

    public boolean update(BaseBean bean) throws IllegalAccessException {
        return update(bean, null, null, this.getWritableDatabase());
    }

    public boolean update(BaseBean bean, SQLiteDatabase db) throws IllegalAccessException {
        return update(bean, null, null, db);
    }

    public boolean update(BaseBean bean, String extraWhere, String [] whereArgs, SQLiteDatabase db) throws IllegalAccessException {

        ISO8601DateTime now = new ISO8601DateTime();
        bean.setUpdateTime(now);
        ContentValues values = getContentValues(bean);

        StringBuilder where = new StringBuilder("_id=?");
        List<String> whereArgList = new ArrayList<String>();
        whereArgList.add(bean.getId().toString());

        if (!TextUtils.isEmpty(extraWhere)) {
            where.append(" ").append(extraWhere);
            if (whereArgs != null) {
                for (String s : whereArgs) {
                    whereArgList.add(s);
                }
            }
        }
        int count = db.update(bean.getTableName(), values, where.toString(),
                whereArgList.toArray(new String[whereArgList.size()]));
        return (count == 1);
    }

    public boolean delete(BaseBean bean)  {
        return delete(bean, this.getWritableDatabase());
    }

    public boolean delete(BaseBean bean, SQLiteDatabase db)  {
        return (db.delete(bean.getTableName(), "_id=?", new String[]{bean.getId().toString()}) == 1);
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

    protected static class GetContentValuesHandler implements MetadataUtils.Handler {
        protected final ContentValues values;
        protected String column;
        protected Object value;

        public GetContentValuesHandler(ContentValues values) {
            this.values = values;
        }

        public GetContentValuesHandler bind(String column, Object value) {
            this.column = column;
            this.value = value;
            return this;
        }

        @Override
        public boolean handleMoney(Field field) {
            values.put(column, Long.valueOf(((Money) value).getMicroCents()));
            return true;
        }

        @Override
        public boolean handleEnum(Field field) {
            values.put(column, ((Enum) value).name());
            return true;
        }

        @Override
        public boolean handleISO8601DateTime(Field field) {
            values.put(column, ((ISO8601DateTime) value).toString());
            return true;
        }

        @Override
        public boolean handleDate(Field field) {
            values.put(column, ISO8601DateTime.toISO8601((Date) value));
            return true;
        }

        @Override
        public boolean handleBaseBean(Field field) {
            values.put(column, ((BaseBean) value).getId());
            return true;
        }

        @Override
        public boolean handleString(Field field) {
            values.put(column, (String) value);
            return true;
        }

        @Override
        public boolean handleBoolean(Field field) {
            values.put(column, (Boolean)value ? 1 : 0);
            return true;
        }

        @Override
        public boolean handleInteger(Field field) {
            values.put(column, (Integer) value);
            return true;
        }

        @Override
        public boolean handleLong(Field field) {
            values.put(column, (Long) value);
            return true;
        }

        @Override
        public boolean handleDouble(Field field) {
            values.put(column, (Double) value);
            return true;
        }

        @Override
        public boolean handleFloat(Field field) {
            values.put(column, (Float) value);
            return true;
        }

        @Override
        public boolean handleUnsupported(Field field) {
            throw new UnsupportedOperationException("Unsupported Type:" + field.getType().getName());
        }
    }

    protected ContentValues getContentValues(BaseBean bean) throws IllegalAccessException {
        final ContentValues values = new ContentValues();

        GetContentValuesHandler handler = new GetContentValuesHandler(values);

        List<Field> fields = MetadataUtils.getAllFields(((Object) bean).getClass());
        for (final Field field : fields) {
            final SqlColumn sqlColumn = field.getAnnotation(SqlColumn.class);
            if (sqlColumn != null) {
                final String column = (sqlColumn.name().equals("") ?
                        field.getName() :
                        sqlColumn.name());

                final Object value = field.get(bean);
                if (value != null) {
                    MetadataUtils.handleField(field, handler.bind(column, value));
                }
            }
        }
        return values;
    }

    protected static class SetFieldFromCursorHandler implements MetadataUtils.Handler {
        protected final Cursor cursor;
        protected int  idx;
        protected BaseBean item;

        public SetFieldFromCursorHandler(Cursor cursor) {
            this.cursor = cursor;
        }

        public SetFieldFromCursorHandler bind(int idx, BaseBean item) {
            this.idx = idx;
            this.item = item;
            return this;
        }

        public Cursor getCursor() {
            return cursor;
        }

        @Override
        public boolean handleMoney(Field field) {
            try {
                field.set(item, new Money(cursor.getLong(idx)));
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleEnum(Field field) {
            try {
                field.set(item, Enum.valueOf((Class<Enum>) field.getType(), cursor.getString(idx)));
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleISO8601DateTime(Field field) {
            try {
                field.set(item, new ISO8601DateTime(cursor.getString(idx)));
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            } catch (ParseException e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleDate(Field field) {
            try {
                field.set(item, ISO8601DateTime.toDate(cursor.getString(idx)));
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            } catch (ParseException e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleBaseBean(Field field) {
            try {
                BaseBean bean = (BaseBean)field.getType().newInstance();
                bean.setId(cursor.getLong(idx));
                field.set(item, bean);
            } catch (Exception e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleString(Field field) {
            try {
                field.set(item, cursor.getString(idx));
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleBoolean(Field field) {
            try {
                field.set(item, (cursor.getInt(idx) == 1));
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleInteger(Field field) {
            try {
                field.set(item, Integer.valueOf(cursor.getInt(idx)));
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleLong(Field field) {
            try {
                field.set(item, Long.valueOf(cursor.getLong(idx)));
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleDouble(Field field) {
            try {
                field.set(item, Double.valueOf(cursor.getDouble(idx)));
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleFloat(Field field) {
            try {
                field.set(item, Float.valueOf(cursor.getFloat(idx)));
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
            return true;
        }

        @Override
        public boolean handleUnsupported(Field field) {
            throw new UnsupportedOperationException("Unsupported Type:" + field.getType().getName());
        }
    }

    protected <T extends BaseBean> void setFieldFromCursor(final Field field, final T item,
                                                           SetFieldFromCursorHandler setFieldFromCursorHandler)
            throws SQLException {

        // getAnnotation is SLOW
        // Caching column names gives a 2x speedup
        String key = field.getDeclaringClass().getSimpleName() + "." + field.getName();
        String columnName = sqlColumnCache.get(key);
        if (columnName == null) {
            final SqlColumn sqlColumn = field.getAnnotation(SqlColumn.class);
            if (sqlColumn != null) {
                columnName = (sqlColumn.name().equals("") ?
                        field.getName() :
                        sqlColumn.name());
            } else {
                // annotation does not exits, use an empty name
                columnName = "";
            }
            sqlColumnCache.put(key, columnName);
        }

        if (!TextUtils.isEmpty(columnName)) {
            Cursor cursor = setFieldFromCursorHandler.getCursor();
            final int idx = cursor.getColumnIndex(columnName);

            if (idx >= 0) {
                if (!cursor.isNull(idx)) {
                    MetadataUtils.handleField(field, setFieldFromCursorHandler.bind(idx, item));
                }
            } else {
                String msg = "Cannot find Column:"+columnName+" in this list of columns:"+
                        TextUtils.join(",", cursor.getColumnNames());
                Log.e(TAG, msg);
                throw new SQLException(msg);
            }
        }
    }
}

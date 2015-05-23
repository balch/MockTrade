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

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.balch.android.app.framework.sql.SqlBean;
import com.balch.android.app.framework.bean.BeanColumnDescriptor;
import com.balch.android.app.framework.bean.BeanEditState;
import com.balch.android.app.framework.bean.BeanViewHint;
import com.balch.android.app.framework.bean.annotations.BeanColumnEdit;
import com.balch.android.app.framework.bean.annotations.BeanColumnNew;
import com.balch.android.app.framework.bean.controls.BeanEditControl;
import com.balch.android.app.framework.bean.controls.BoolEditControl;
import com.balch.android.app.framework.bean.controls.EnumEditControl;
import com.balch.android.app.framework.bean.controls.MoneyEditControl;
import com.balch.android.app.framework.bean.controls.NumberEditControl;
import com.balch.android.app.framework.bean.controls.StringEditControl;
import com.balch.android.app.framework.bean.controls.UnsupportedEditControl;
import com.balch.android.app.framework.types.ISO8601DateTime;
import com.balch.android.app.framework.types.Money;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataUtils {
    private static final String TAG = MetadataUtils.class.getName();

    protected static Map<String, List<Field>> fieldCache = new HashMap<String, List<Field>>();

    // TODO: more EditControl classes
    public enum FrameworkType {
        MONEY(Money.class, MoneyEditControl.class),
        ENUM(Enum.class, EnumEditControl.class),
        ISO8601DATETIME(ISO8601DateTime.class, null),
        DATE(Date.class, null),
        BASEBEAN(SqlBean.class, null),
        STRING(String.class, StringEditControl.class),
        BOOLEAN(Boolean.class, BoolEditControl.class),
        INTEGER(Integer.class, null),
        LONG(Long.class, null),
        DOUBLE(Double.class, NumberEditControl.class),
        FLOAT(Float.class, NumberEditControl.class),
        UNSUPPORTED(null, UnsupportedEditControl.class);

        protected final Class<?> clazz;
        protected final Class<? extends ViewGroup> editViewClazz;

        FrameworkType(Class<?> clazz, Class<? extends ViewGroup> editViewClazz) {
            this.clazz = clazz;
            this.editViewClazz = editViewClazz;
        }
    }

    public interface Handler {

        boolean handleMoney(Field field);

        boolean handleEnum(Field field);

        boolean handleISO8601DateTime(Field field);

        boolean handleDate(Field field);

        boolean handleBaseBean(Field field);

        boolean handleString(Field field);

        boolean handleBoolean(Field field);

        boolean handleInteger(Field field);

        boolean handleLong(Field field);

        boolean handleDouble(Field field);

        boolean handleFloat(Field field);

        boolean handleUnsupported(Field field);
    }

    /**
     * function requiring caller to supply a
     * handler for all types supported by the framework.
     */
    static public boolean handleField(Field field, Handler handler) {

        FrameworkType frameworkType = getFrameworkTypeByField(field);

        boolean handled;

        if (frameworkType == FrameworkType.MONEY) {
            handled = handler.handleMoney(field);
        } else if (frameworkType == FrameworkType.ENUM) {
            handled = handler.handleEnum(field);
        } else if (frameworkType == FrameworkType.ISO8601DATETIME) {
            handled = handler.handleISO8601DateTime(field);
        } else if (frameworkType == FrameworkType.DATE) {
            handled = handler.handleDate(field);
        } else if (frameworkType == FrameworkType.BASEBEAN) {
            handled = handler.handleBaseBean(field);
        } else if (frameworkType == FrameworkType.STRING) {
            handled = handler.handleString(field);
        } else if (frameworkType == FrameworkType.BOOLEAN) {
            handled = handler.handleBoolean(field);
        } else if (frameworkType == FrameworkType.INTEGER) {
            handled = handler.handleInteger(field);
        } else if (frameworkType == FrameworkType.LONG) {
            handled = handler.handleLong(field);
        } else if (frameworkType == FrameworkType.DOUBLE) {
            handled = handler.handleDouble(field);
        } else if (frameworkType == FrameworkType.FLOAT) {
            handled = handler.handleFloat(field);
        } else {
            handled = handler.handleUnsupported(field);
        }

        return handled;
    }

    static public List<BeanColumnDescriptor> getBeanColumnDescriptors(SqlBean item, boolean isNew) {

        List<BeanColumnDescriptor> descriptors = new ArrayList<BeanColumnDescriptor>();

        List<Field> fields = getAllFields(((Object)item).getClass());
        for (Field field : fields) {
            if (isNew) {
                final BeanColumnNew newable = field.getAnnotation(BeanColumnNew.class);
                if ((newable != null) && (newable.state() != BeanEditState.HIDDEN)) {
                    Class<? extends BeanEditControl> customControl = newable.customControl();
                    if (BeanEditControl.class.equals(customControl)) {
                        customControl = null;
                    }

                    descriptors.add(new BeanColumnDescriptor(item, field, newable.labelResId(),
                            newable.state(), BeanViewHint.parse(newable.hints()), newable.order(),
                            customControl));
                }

            } else {
                final BeanColumnEdit editable = field.getAnnotation(BeanColumnEdit.class);
                if ((editable != null) && (editable.state() != BeanEditState.HIDDEN)) {
                    Class<? extends BeanEditControl> customControl = editable.customControl();
                    if (BeanEditControl.class.equals(customControl)) {
                        customControl = null;
                    }

                    descriptors.add(new BeanColumnDescriptor(item, field, editable.labelResId(),
                            editable.state(), BeanViewHint.parse(editable.hints()), editable.order(),
                            customControl));
                }
            }
        }

        Collections.sort(descriptors);

        return descriptors;
    }

    static public View getEditView(BeanColumnDescriptor columnDescriptor,
                                   FrameworkType frameworkType, Context context) {
        View view = null;
        Class<? extends ViewGroup> editViewClazz = frameworkType.editViewClazz;
        if (columnDescriptor.getCustomControl() != null) {
            editViewClazz = (Class<? extends ViewGroup>) columnDescriptor.getCustomControl();
        }

        if (editViewClazz != null) {
            try {
                view =  editViewClazz.getConstructor(Context.class).newInstance(context);
            } catch (Exception e) {
                Log.e(TAG, "Error getting EditView from Framework Tyoe", e);
            }
        }
        return view;
    }

    static public FrameworkType getFrameworkTypeByField(Field field) {
        FrameworkType frameworkType = FrameworkType.UNSUPPORTED;

        if (field.getType().isEnum()) {
            frameworkType = FrameworkType.ENUM;
        } else {
            for (FrameworkType f : FrameworkType.values()) {
                if (f.clazz != null) {
                    if (f.clazz.isAssignableFrom(field.getType())) {
                        frameworkType = f;
                        break;
                    }
                }
            }
        }
        return frameworkType;
    }

    public static List<Field> getAllFields(Class<?> type) {

        String key = type.getName();
        List<Field> fields = fieldCache.get(key);
        if (fields == null) {
            fields = getAllFields(new ArrayList<Field>(), type);
            fieldCache.put(key, fields);
        }

        return fields;
    }

    protected static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);
            fields.add(field);
        }

        if (type.getSuperclass() != null) {
            fields = getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    public interface EnumResource {
        int getListResId();
    }

}

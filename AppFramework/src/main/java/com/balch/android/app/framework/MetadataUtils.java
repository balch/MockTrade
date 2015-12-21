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

import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.EditState;
import com.balch.android.app.framework.domain.ViewHint;
import com.balch.android.app.framework.domain.annotations.ColumnEdit;
import com.balch.android.app.framework.domain.annotations.ColumnNew;
import com.balch.android.app.framework.domain.widgets.EditLayout;
import com.balch.android.app.framework.domain.widgets.BoolEditLayout;
import com.balch.android.app.framework.domain.widgets.EnumEditLayout;
import com.balch.android.app.framework.domain.widgets.MoneyEditLayout;
import com.balch.android.app.framework.domain.widgets.NumberEditLayout;
import com.balch.android.app.framework.domain.widgets.StringEditLayout;
import com.balch.android.app.framework.domain.widgets.UnsupportedEditLayout;
import com.balch.android.app.framework.domain.DomainObject;
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
    private static final String TAG = MetadataUtils.class.getSimpleName();

    protected static Map<String, List<Field>> fieldCache = new HashMap<>();

    // TODO: more EditControl classes
    public enum FrameworkType {
        MONEY(Money.class, MoneyEditLayout.class),
        ENUM(Enum.class, EnumEditLayout.class),
        ISO8601DATETIME(ISO8601DateTime.class, null),
        DATE(Date.class, null),
        DOMAINOBJECT(DomainObject.class, null),
        STRING(String.class, StringEditLayout.class),
        BOOLEAN(Boolean.class, BoolEditLayout.class),
        INTEGER(Integer.class, null),
        LONG(Long.class, null),
        DOUBLE(Double.class, NumberEditLayout.class),
        FLOAT(Float.class, NumberEditLayout.class),
        UNSUPPORTED(null, UnsupportedEditLayout.class);

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

        boolean handleDomainObject(Field field);

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
        } else if (frameworkType == FrameworkType.DOMAINOBJECT) {
            handled = handler.handleDomainObject(field);
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

    static public List<ColumnDescriptor> getColumnDescriptors(DomainObject domainObject, boolean isNew) {

        List<ColumnDescriptor> descriptors = new ArrayList<>();

        List<Field> fields = getAllFields(((Object)domainObject).getClass());
        for (Field field : fields) {
            if (isNew) {
                final ColumnNew newable = field.getAnnotation(ColumnNew.class);
                if ((newable != null) && (newable.state() != EditState.HIDDEN)) {
                    Class<? extends EditLayout> customControl = newable.customControl();
                    if (EditLayout.class.equals(customControl)) {
                        customControl = null;
                    }

                    descriptors.add(new ColumnDescriptor(domainObject, field, newable.labelResId(),
                            newable.state(), ViewHint.parse(newable.hints()), newable.order(),
                            customControl));
                }

            } else {
                final ColumnEdit editable = field.getAnnotation(ColumnEdit.class);
                if ((editable != null) && (editable.state() != EditState.HIDDEN)) {
                    Class<? extends EditLayout> customControl = editable.customControl();
                    if (EditLayout.class.equals(customControl)) {
                        customControl = null;
                    }

                    descriptors.add(new ColumnDescriptor(domainObject, field, editable.labelResId(),
                            editable.state(), ViewHint.parse(editable.hints()), editable.order(),
                            customControl));
                }
            }
        }

        Collections.sort(descriptors);

        return descriptors;
    }

    static public View getEditView(ColumnDescriptor columnDescriptor,
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

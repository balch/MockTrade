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

package com.balch.android.app.framework.core;

import androidx.annotation.NonNull;

import com.balch.android.app.framework.core.widget.EditLayout;

import java.lang.reflect.Field;

public class ColumnDescriptor implements Comparable<ColumnDescriptor> {
    protected final DomainObject item;
    protected final Field field;
    protected final int labelResId;
    protected final EditState state;
    protected final ViewHint[] hints;
    protected final int order;
    protected final Class<? extends EditLayout> customControl;


    public ColumnDescriptor(DomainObject item, Field field, int labelResId,
                            EditState state, ViewHint[] hints, int order,
                            Class<? extends EditLayout> customControl) {
        this.item = item;
        this.field = field;
        this.labelResId = labelResId;
        this.state = state;
        this.hints = hints;
        this.order = order;
        this.customControl = customControl;
    }

    public int getLabelResId() {
        return labelResId;
    }

    public EditState getState() {
        return state;
    }

    public ViewHint[] getHints() {
        return hints;
    }

    public int getOrder() {
        return order;
    }

    public Field getField() {
        return field;
    }

    public DomainObject getItem() {
        return item;
    }

    public Class<? extends EditLayout> getCustomControl() {
        return customControl;
    }

    @Override
    public int compareTo(@NonNull ColumnDescriptor another) {
        return (this.order < another.order) ? -1 : ((this.order == another.order) ? 0 : 1);
    }
}

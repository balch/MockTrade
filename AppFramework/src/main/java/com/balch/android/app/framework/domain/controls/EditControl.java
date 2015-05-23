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

package com.balch.android.app.framework.domain.controls;

import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.ValidatorException;

public interface EditControl {
    interface EditControlListener {
        void onChanged(ColumnDescriptor descriptor, Object value, boolean hasError) throws ValidatorException;
        void onError(ColumnDescriptor descriptor, Object value, String errorMsg);
    }

    void bind(ColumnDescriptor descriptor);
    ColumnDescriptor getDescriptor();
    void validate() throws ValidatorException;
    Object getValue();
    void setValue(Object value);
    void setEditControlListener(EditControlListener listener);
    void setControlMapper(ControlMapper controlMapper);
    void setEnabled(boolean enabled);
}

/*
 * Author: Balch
 * Created: 8/19/17 7:47 AM
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
 * Copyright (C) 2017
 *
 */

package com.balch.android.app.framework.core;

import com.balch.android.app.framework.BasePresenter;

import java.util.List;

public class EditPresenter extends BasePresenter<EditView> {
    public EditPresenter(EditView view) {
        super(view);
    }

    public void initialize(DomainObject item, boolean isNew, ExternalController validator,
                           int okButtonResId, int cancelButtonResId, List<Integer> columnViewIDs,
                           EditView.EditViewListener listener) {
        view.bind(item, isNew, validator, okButtonResId, cancelButtonResId, columnViewIDs);
        view.setEditViewListener(listener);
    }

}

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

package com.balch.android.app.framework.domain;

import android.app.Application;
import android.os.Bundle;

import com.balch.android.app.framework.BasePresenter;

public class EditPresenter extends BasePresenter<Application> {

    protected final EditView view;
    protected final ExternalController validator;
    protected final DomainObject item;
    protected final boolean isNew;
    protected final int okButtonResId;
    protected final int cancelButtonResId;

    public EditPresenter(EditView view, boolean isNew, DomainObject item, ExternalController validator,
                         int okButtonResId, int cancelButtonResId,
                         EditView.EditViewListener editViewListener) {
        this.view = view;
        this.isNew = isNew;
        this.item = item;
        this.validator = validator;
        this.okButtonResId = okButtonResId;
        this.cancelButtonResId = cancelButtonResId;
        this.view.setEditViewListener(editViewListener);
    }

    @Override
    public void initialize(Bundle savedInstanceState) {
        this.view.bind(this.item, this.isNew, this.validator,
                this.okButtonResId, this.cancelButtonResId);
    }
}

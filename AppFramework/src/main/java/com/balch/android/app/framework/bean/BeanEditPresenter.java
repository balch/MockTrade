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

package com.balch.android.app.framework.bean;

import android.app.Application;
import android.os.Bundle;

import com.balch.android.app.framework.BasePresenter;
import com.balch.android.app.framework.sql.SqlBean;

public class BeanEditPresenter extends BasePresenter<Application> {

    protected final BeanEditView view;
    protected final BeanExternalController validator;
    protected final SqlBean item;
    protected final boolean isNew;
    protected final int okButtonResId;
    protected final int cancelButtonResId;

    public BeanEditPresenter(BeanEditView view, boolean isNew, SqlBean item, BeanExternalController validator,
                             int okButtonResId, int cancelButtonResId,
                             BeanEditView.BeanEditViewListener beanEditViewListener) {
        this.view = view;
        this.isNew = isNew;
        this.item = item;
        this.validator = validator;
        this.okButtonResId = okButtonResId;
        this.cancelButtonResId = cancelButtonResId;
        this.view.setBeanEditViewListener(beanEditViewListener);
    }

    @Override
    public void initialize(Bundle savedInstanceState) {
        this.view.bind(this.item, this.isNew, this.validator,
                this.okButtonResId, this.cancelButtonResId);
    }
}

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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.balch.android.app.framework.ModelProvider;
import com.balch.android.app.framework.PresenterActivity;
import com.balch.android.app.framework.R;

import java.util.ArrayList;

public class EditActivity extends PresenterActivity<EditView, ModelProvider> {
    protected static final String EXTRA_ISNEW = "isNew";
    protected static final String EXTRA_ITEM = "item";
    protected static final String EXTRA_VALIDATOR = "validator";
    protected static final String EXTRA_TITLE_RESID = "titleResId";
    protected static final String EXTRA_OK_BUTTON_RESID = "okButtonResId";
    protected static final String EXTRA_CANCEL_BUTTON_RESID = "cancelButtonResId";
    protected static final String EXTRA_RESULT = "EditActivityResult";

    protected static final String STATE_COLUMN_VIEW_IDS = "col_view_ids";

    protected EditView view;
    protected ArrayList<Integer> columnViewIDs = new ArrayList<>();

    protected ExternalController validator;
    protected DomainObject item;
    protected boolean isNew;
    protected int okButtonResId;
    protected int cancelButtonResId;

    @Override
    public void onCreateBase(Bundle savedInstanceState) {
        Intent intent = this.getIntent();
        int titleResId = intent.getIntExtra(EXTRA_TITLE_RESID, 0);
        if (titleResId != 0) {
            this.setTitle(titleResId);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.edit_view_toolbar);
        setSupportActionBar(toolbar);

        this.validator = intent.getParcelableExtra(EXTRA_VALIDATOR);
        this.item = intent.getParcelableExtra(EXTRA_ITEM);
        this.isNew = intent.getBooleanExtra(EXTRA_ISNEW, false);
        this.okButtonResId = intent.getIntExtra(EXTRA_OK_BUTTON_RESID, 0);
        this.cancelButtonResId = intent.getIntExtra(EXTRA_CANCEL_BUTTON_RESID, 0);

        this.view.setEditViewListener(new EditView.EditViewListener() {
            @Override
            public void onSave(DomainObject item) {
                Intent intent = getIntent();
                intent.putExtra(EXTRA_RESULT, item);
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void onCancel() {
                Intent intent = getIntent();
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        });

        if (savedInstanceState != null) {
            this.columnViewIDs = savedInstanceState.getIntegerArrayList(STATE_COLUMN_VIEW_IDS);
        }

        this.view.bind(this.item, this.isNew, this.validator, this.okButtonResId, this.cancelButtonResId, this.columnViewIDs);
    }

    @Override
    public void onSaveInstanceStateBase(Bundle outState) {
        outState.putIntegerArrayList(STATE_COLUMN_VIEW_IDS, this.columnViewIDs);
    }

    @Override
    public EditView createView() {
        this.view = new EditView(this);
        return this.view;
    }

    @Override
    protected void createModel(ModelProvider modelProvider) {

    }

    public static Intent getIntent(Context context, int titleResId, DomainObject domainObject, ExternalController externalController,
                                   int okButtonResId, int cancelButtonResId) {
        Intent intent = new Intent(context, EditActivity.class);

        intent.putExtra(EXTRA_ISNEW, (domainObject.getId() == null));
        intent.putExtra(EXTRA_ITEM, domainObject);
        intent.putExtra(EXTRA_VALIDATOR, externalController);
        intent.putExtra(EXTRA_TITLE_RESID, titleResId);
        intent.putExtra(EXTRA_OK_BUTTON_RESID, okButtonResId);
        intent.putExtra(EXTRA_CANCEL_BUTTON_RESID, cancelButtonResId);

        return intent;
    }

    public static <T extends DomainObject> T getResult(Intent intent) {
        return (T) intent.getParcelableExtra(EXTRA_RESULT);
    }
}

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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.balch.android.app.framework.BaseActivity;
import com.balch.android.app.framework.BasePresenter;
import com.balch.android.app.framework.sql.SqlBean;

import java.util.Arrays;
import java.util.List;

public class BeanEditActivity extends BaseActivity<BeanEditView> {
    protected static final String EXTRA_ISNEW = "isNew";
    protected static final String EXTRA_ITEM = "item";
    protected static final String EXTRA_VALIDATOR = "validator";
    protected static final String EXTRA_TITLE_RESID = "titleResId";
    protected static final String EXTRA_OK_BUTTON_RESID = "okButtonResId";
    protected static final String EXTRA_CANCEL_BUTTON_RESID = "cancelButtonResId";
    protected static final String EXTRA_RESULT = "BeanEditActivityResult";


    @Override
    protected void initialize(Bundle savedInstanceState) {
    }

    @Override
    protected List<BasePresenter> createPresenters(BeanEditView view) {

        Intent intent = this.getIntent();
        int titleResId = intent.getIntExtra(EXTRA_TITLE_RESID, 0);
        if (titleResId != 0) {
            this.setTitle(titleResId);
        }

        BeanExternalController validator = (BeanExternalController) intent.getSerializableExtra(EXTRA_VALIDATOR);
        SqlBean item = (SqlBean) intent.getSerializableExtra(EXTRA_ITEM);
        boolean  isNew = intent.getBooleanExtra(EXTRA_ISNEW, false);
        int okButtonResId = intent.getIntExtra(EXTRA_OK_BUTTON_RESID, 0);
        int cancelButtonResId = intent.getIntExtra(EXTRA_CANCEL_BUTTON_RESID, 0);

        return Arrays.asList((BasePresenter) new BeanEditPresenter(view, isNew, item, validator,
                okButtonResId, cancelButtonResId,
                new BeanEditView.BeanEditViewListener() {
                    @Override
                    public void onSave(SqlBean item) {
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
                }));
    }

    @Override
    protected BeanEditView createView() {
        return new BeanEditView(this);
    }

    public static Intent getIntent(Context context, int titleResId, SqlBean item, BeanExternalController beanExternalController,
                                   int okButtonResId, int cancelButtonResId) {
        Intent intent = new Intent(context, BeanEditActivity.class);

        intent.putExtra(EXTRA_ISNEW, (item.getId() == null));
        intent.putExtra(EXTRA_ITEM, item);
        intent.putExtra(EXTRA_VALIDATOR, beanExternalController);
        intent.putExtra(EXTRA_TITLE_RESID, titleResId);
        intent.putExtra(EXTRA_OK_BUTTON_RESID, okButtonResId);
        intent.putExtra(EXTRA_CANCEL_BUTTON_RESID, cancelButtonResId);

        return intent;
    }

    public static <T extends SqlBean> T getResult(Intent intent) {
        return (T) intent.getSerializableExtra(EXTRA_RESULT);
    }
}

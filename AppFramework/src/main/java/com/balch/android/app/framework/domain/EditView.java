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
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.balch.android.app.framework.MetadataUtils;
import com.balch.android.app.framework.R;
import com.balch.android.app.framework.domain.widget.ControlMap;
import com.balch.android.app.framework.domain.widget.ControlMapper;
import com.balch.android.app.framework.domain.widget.EditLayout;
import com.balch.android.app.framework.domain.widget.UnsupportedEditLayout;
import com.balch.android.app.framework.view.BaseView;

import java.lang.reflect.Field;
import java.util.List;

public class EditView extends LinearLayout implements BaseView, ControlMapper {
    private static final String TAG = EditView.class.getSimpleName();

    public interface EditViewListener {
        void onSave(DomainObject domainObject);
        void onCancel();
    }

    protected EditViewListener mEditViewListener;
    protected List<ColumnDescriptor> mColumnDescriptorList;
    protected DomainObject mDomainObject;

    protected LinearLayout mEditControlLayout;
    protected Button mOkButton;
    protected Button mCancelButton;
    protected ExternalController mExternalController;
    protected ControlMap mControlMap = new ControlMap();

    public EditView(Context context) {
        super(context);
    }

    public EditView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void initializeLayout() {
        inflate(getContext(), R.layout.edit_view, this);
        this.mEditControlLayout = (LinearLayout)findViewById(R.id.edit_layout);
        this.mOkButton = (Button)findViewById(R.id.edit_ok_button);
        this.mCancelButton = (Button)findViewById(R.id.edit_cancel_button);

        this.mOkButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditView.this.save();
            }
        });

        this.mCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditView.this.cancel();
            }
        });

    }

    @Override
    public ControlMap getControlMap() {
        return this.mControlMap;
    }

    public void bind(DomainObject domainObject, boolean isNew, ExternalController controller,
                     int okButtonResId, int cancelButtonResId) {
        this.mDomainObject = domainObject;
        this.mExternalController = controller;
        this.mColumnDescriptorList = MetadataUtils.getColumnDescriptors(mDomainObject, isNew);

        this.mCancelButton.setText((cancelButtonResId != 0) ? cancelButtonResId : R.string.edit_view_button_cancel);
        this.mOkButton.setText((okButtonResId != 0) ? okButtonResId : isNew ? R.string.edit_view_ok_button_new : R.string.edit_view_ok_button_edit);

        this.mControlMap.clear();
        for (ColumnDescriptor descriptor : this.mColumnDescriptorList) {
            MetadataUtils.FrameworkType frameworkType = MetadataUtils.getFrameworkTypeByField(descriptor.getField());

            View view = MetadataUtils.getEditView(descriptor, frameworkType, this.getContext());
            if (view == null) {
                view = new UnsupportedEditLayout(this.getContext());
            }

            if (view instanceof EditLayout) {

                EditLayout control = (EditLayout)view;
                this.mControlMap.put(descriptor.getField().getName(), control);

                control.setControlMapper(this);
                control.bind(descriptor);
                control.setEditControlListener(new EditLayout.EditLayoutListener() {

                    @Override
                    public void onChanged(ColumnDescriptor descriptor, Object value, boolean hasError) throws ValidatorException {
                        if (!hasError) {
                            valueChanged(descriptor, value);
                            validate();
                        } else {
                            setErrorState(true);
                        }
                    }

                    @Override
                    public void onError(ColumnDescriptor descriptor, Object value, String errorMsg) {
                        setErrorState(true);
                    }
                });
            }

            this.mEditControlLayout.addView(view);
        }

        if (this.mExternalController != null) {
            this.mExternalController.initialize(this.getContext(), mDomainObject, this.mControlMap);
        }

        validate();
    }

    protected void save() {
        populateFromView(this.mEditControlLayout, this.mDomainObject);

        if (this.mEditViewListener != null) {
            this.mEditViewListener.onSave(this.mDomainObject);
        }
    }

    protected DomainObject getPopulatedCopy() {
        DomainObject domObj = createEmptyObject();
        Field[] fields = ((Object)this.mDomainObject).getClass().getDeclaredFields();
        for (Field field : fields) {
            if (DomainObject.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                try {
                    field.set(domObj, field.get(this.mDomainObject));
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Error Setting DomainObject", e);
                }
            }
        }


        populateFromView(this.mEditControlLayout, domObj);
        return  domObj;
    }

    protected DomainObject createEmptyObject() {
        DomainObject domObj = null;
        try {
            domObj = (DomainObject)((Object) mDomainObject).getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Log.e(TAG, "Create DomainObject object", e);
            setErrorState(true);
        }
        return domObj;
    }

    protected static void populateFromView(LinearLayout editControlLayout, DomainObject domainObject) {
        try {
            int cnt = editControlLayout.getChildCount();
            for (int x = 0; x < cnt; x++) {
                View view = editControlLayout.getChildAt(x);
                if ( (view instanceof EditLayout) &&
                    !(view instanceof UnsupportedEditLayout) ) {
                    EditLayout control = (EditLayout) view;
                    control.getDescriptor().getField().set(domainObject, control.getValue());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error Populating control", e);
            Toast.makeText(editControlLayout.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    protected void cancel() {
        if (this.mEditViewListener != null) {
            this.mEditViewListener.onCancel();
        }
    }

    protected void validate()  {
        boolean hasError = false;

        int cnt = this.mEditControlLayout.getChildCount();
        for (int x = 0; x < cnt; x++) {
            View view = this.mEditControlLayout.getChildAt(x);
            if (view.getVisibility() == VISIBLE) {
                if (view instanceof EditLayout) {
                    EditLayout control = (EditLayout) view;
                    try {
                        control.validate();
                    } catch (ValidatorException e) {
                        hasError = true;
                        break;
                    }
                }
            }
        }

        if (this.mExternalController != null) {
            try {
                mExternalController.validate(this.getContext(), getPopulatedCopy(), this.mControlMap);
            } catch (ValidatorException e) {
                hasError = true;
            }
        }

        setErrorState(hasError);
    }

    protected void valueChanged(ColumnDescriptor descriptor, Object value) throws ValidatorException {
        if ( this.mExternalController != null) {
            try {
                mExternalController.onChanged(this.getContext(), descriptor, value, this.mControlMap);
            } catch (ValidatorException e) {
                setErrorState(true);
                throw e;
            }
        }
    }

    protected void setErrorState(boolean hasError) {
        this.mOkButton.setEnabled(!hasError);
    }

    public void setEditViewListener(EditViewListener editViewListener) {
        this.mEditViewListener = editViewListener;
    }
}

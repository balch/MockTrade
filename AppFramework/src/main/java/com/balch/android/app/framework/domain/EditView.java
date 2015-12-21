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
import com.balch.android.app.framework.domain.controls.ControlMap;
import com.balch.android.app.framework.domain.controls.ControlMapper;
import com.balch.android.app.framework.domain.controls.EditControl;
import com.balch.android.app.framework.domain.controls.UnsupportedEditControl;
import com.balch.android.app.framework.view.BaseView;

import java.lang.reflect.Field;
import java.util.List;

public class EditView extends LinearLayout implements BaseView, ControlMapper {
    private static final String TAG = EditView.class.getSimpleName();

    public interface EditViewListener {
        void onSave(DomainObject domainObject);
        void onCancel();
    }

    protected EditViewListener editViewListener;
    protected List<ColumnDescriptor> columns;
    protected DomainObject item;

    protected LinearLayout editControlLayout;
    protected Button okButton;
    protected Button cancelButton;
    protected ExternalController controller;
    protected ControlMap controlMap = new ControlMap();

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
        this.editControlLayout = (LinearLayout)findViewById(R.id.edit_layout);
        this.okButton = (Button)findViewById(R.id.edit_ok_button);
        this.cancelButton = (Button)findViewById(R.id.edit_cancel_button);

        this.okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditView.this.save();
            }
        });

        this.cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditView.this.cancel();
            }
        });

    }

    @Override
    public ControlMap getControlMap() {
        return this.controlMap;
    }

    public void bind(DomainObject domainObject, boolean isNew, ExternalController controller,
                     int okButtonResId, int cancelButtonResId) {
        this.item = domainObject;
        this.controller = controller;
        this.columns = MetadataUtils.getColumnDescriptors(item, isNew);

        this.cancelButton.setText((cancelButtonResId != 0) ? cancelButtonResId : R.string.edit_view_button_cancel);
        this.okButton.setText((okButtonResId != 0) ? okButtonResId : isNew ? R.string.edit_view_ok_button_new : R.string.edit_view_ok_button_edit);

        this.controlMap.clear();
        for (ColumnDescriptor descriptor : this.columns) {
            MetadataUtils.FrameworkType frameworkType = MetadataUtils.getFrameworkTypeByField(descriptor.getField());

            View view = MetadataUtils.getEditView(descriptor, frameworkType, this.getContext());
            if (view == null) {
                view = new UnsupportedEditControl(this.getContext());
            }

            if (view instanceof EditControl) {

                EditControl control = (EditControl)view;
                this.controlMap.put(descriptor.getField().getName(), control);

                control.setControlMapper(this);
                control.bind(descriptor);
                control.setEditControlListener(new EditControl.EditControlListener() {

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

            this.editControlLayout.addView(view);
        }

        if (this.controller != null) {
            this.controller.initialize(this.getContext(), item, this.controlMap);
        }

        validate();
    }

    protected void save() {
        populateFromView(this.editControlLayout, this.item);

        if (this.editViewListener != null) {
            this.editViewListener.onSave(this.item);
        }
    }

    protected DomainObject getPopulatedCopy() {
        DomainObject domObj = createEmptyObject();
        Field[] fields = ((Object)this.item).getClass().getDeclaredFields();
        for (Field field : fields) {
            if (DomainObject.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                try {
                    field.set(domObj, field.get(this.item));
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Error Setting DomainObject", e);
                }
            }
        }


        populateFromView(this.editControlLayout, domObj);
        return  domObj;
    }

    protected DomainObject createEmptyObject() {
        DomainObject domObj = null;
        try {
            domObj = (DomainObject)((Object)item).getClass().newInstance();
        } catch (InstantiationException e) {
            Log.e(TAG, "Create DomainObject object", e);
            setErrorState(true);
        } catch (IllegalAccessException e) {
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
                if ( (view instanceof EditControl) &&
                    !(view instanceof UnsupportedEditControl) ) {
                    EditControl control = (EditControl) view;
                    control.getDescriptor().getField().set(domainObject, control.getValue());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error Populating control", e);
            Toast.makeText(editControlLayout.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    protected void cancel() {
        if (this.editViewListener != null) {
            this.editViewListener.onCancel();
        }
    }

    protected void validate()  {
        boolean hasError = false;

        int cnt = this.editControlLayout.getChildCount();
        for (int x = 0; x < cnt; x++) {
            View view = this.editControlLayout.getChildAt(x);
            if (view.getVisibility() == VISIBLE) {
                if (view instanceof EditControl) {
                    EditControl control = (EditControl) view;
                    try {
                        control.validate();
                    } catch (ValidatorException e) {
                        hasError = true;
                        break;
                    }
                }
            }
        }

        if (this.controller != null) {
            try {
                controller.validate(this.getContext(), getPopulatedCopy(), this.controlMap);
            } catch (ValidatorException e) {
                hasError = true;
            }
        }

        setErrorState(hasError);
    }

    protected void valueChanged(ColumnDescriptor descriptor, Object value) throws ValidatorException {
        if ( this.controller != null) {
            try {
                controller.onChanged(this.getContext(), descriptor, value, this.controlMap);
            } catch (ValidatorException e) {
                setErrorState(true);
                throw e;
            }
        }
    }

    protected void setErrorState(boolean hasError) {
        this.okButton.setEnabled(!hasError);
    }

    public void setEditViewListener(EditViewListener editViewListener) {
        this.editViewListener = editViewListener;
    }
}

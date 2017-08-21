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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.balch.android.app.framework.R;
import com.balch.android.app.framework.core.widget.ControlMap;
import com.balch.android.app.framework.core.widget.ControlMapper;
import com.balch.android.app.framework.core.widget.EditLayout;
import com.balch.android.app.framework.core.widget.UnsupportedEditLayout;
import com.balch.android.app.framework.BaseView;

import java.lang.reflect.Field;
import java.util.List;

public class EditView extends LinearLayout implements BaseView, ControlMapper {
    private static final String TAG = EditView.class.getSimpleName();

    public interface EditViewListener {
        void onSave(DomainObject domainObject);
        void onCancel();
    }

    protected EditViewListener listener;
    protected List<ColumnDescriptor> columnDescriptorList;
    protected DomainObject domainObject;

    protected LinearLayout editControlLayout;
    protected Button okButton;
    protected Button cancelButton;
    protected ExternalController externalController;
    protected ControlMap controlMap = new ControlMap();

    public EditView(Context context) {
        super(context);
        initializeLayout();
    }

    public EditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeLayout();
    }

    public EditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeLayout();
    }

    private void initializeLayout() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setOrientation(VERTICAL);

        inflate(getContext(), R.layout.edit_view, this);
        editControlLayout = (LinearLayout)findViewById(R.id.edit_layout);
        okButton = (Button)findViewById(R.id.edit_ok_button);
        cancelButton = (Button)findViewById(R.id.edit_cancel_button);

        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditView.this.save();
            }
        });

        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditView.this.cancel();
            }
        });

    }

    @Override
    public void cleanup() {
        listener = null;
    }

    @Override
    public ControlMap getControlMap() {
        return controlMap;
    }

    @SuppressWarnings("unchecked")
    public void bind(DomainObject domainObject, boolean isNew, ExternalController controller,
                     int okButtonResId, int cancelButtonResId, List<Integer> columnViewIDs) {
        this.domainObject = domainObject;
        externalController = controller;
        columnDescriptorList = MetadataUtils.getColumnDescriptors(this.domainObject, isNew);

        cancelButton.setText((cancelButtonResId != 0) ? cancelButtonResId : R.string.edit_view_button_cancel);
        okButton.setText((okButtonResId != 0) ? okButtonResId : isNew ? R.string.edit_view_ok_button_new : R.string.edit_view_ok_button_edit);

        controlMap.clear();
        int cnt = 0;
        for (ColumnDescriptor descriptor : columnDescriptorList) {
            MetadataUtils.FrameworkType frameworkType = MetadataUtils.getFrameworkTypeByField(descriptor.getField());

            View view = MetadataUtils.getEditView(descriptor, frameworkType, this.getContext());
            if (view == null) {
                view = new UnsupportedEditLayout(getContext());
            }

            if (cnt >= columnViewIDs.size()) {
                columnViewIDs.add(View.generateViewId());
            }
            view.setId(columnViewIDs.get(cnt));
            cnt++;

            if (view instanceof EditLayout) {

                EditLayout control = (EditLayout)view;
                controlMap.put(descriptor.getField().getName(), control);

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

            editControlLayout.addView(view);
        }

        if (externalController != null) {
            externalController.initialize(getContext(), this.domainObject, this.controlMap);
        }

        validate();
    }

    protected void save() {
        populateFromView(this.editControlLayout, this.domainObject);

        if (this.listener != null) {
            this.listener.onSave(this.domainObject);
        }
    }

    protected DomainObject getPopulatedCopy() {
        DomainObject domObj = createEmptyObject();
        Field[] fields = ((Object)this.domainObject).getClass().getDeclaredFields();
        for (Field field : fields) {
            if (DomainObject.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                try {
                    field.set(domObj, field.get(this.domainObject));
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
            domObj = (DomainObject)((Object) domainObject).getClass().newInstance();
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
        if (listener != null) {
            listener.onCancel();
        }
    }

    @SuppressWarnings("unchecked")
    protected void validate()  {
        boolean hasError = false;

        int cnt = editControlLayout.getChildCount();
        for (int x = 0; x < cnt; x++) {
            View view = editControlLayout.getChildAt(x);
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

        if (externalController != null) {
            try {
                externalController.validate(this.getContext(), getPopulatedCopy(), controlMap);
            } catch (ValidatorException e) {
                hasError = true;
            }
        }

        setErrorState(hasError);
    }

    protected void valueChanged(ColumnDescriptor descriptor, Object value) throws ValidatorException {
        if ( externalController != null) {
            try {
                externalController.onChanged(this.getContext(), descriptor, value, controlMap);
            } catch (ValidatorException e) {
                setErrorState(true);
                throw e;
            }
        }
    }

    protected void setErrorState(boolean hasError) {
        okButton.setEnabled(!hasError);
    }

    public void setEditViewListener(EditViewListener listener) {
        this.listener = listener;
    }

}

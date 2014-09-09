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
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.balch.android.app.framework.MetadataUtils;
import com.balch.android.app.framework.R;
import com.balch.android.app.framework.bean.controls.BeanControlMap;
import com.balch.android.app.framework.bean.controls.BeanControlMapper;
import com.balch.android.app.framework.bean.controls.BeanEditControl;
import com.balch.android.app.framework.bean.controls.UnsupportedEditControl;
import com.balch.android.app.framework.view.BaseView;

import java.lang.reflect.Field;
import java.util.List;

public class BeanEditView extends ScrollView implements BaseView, BeanControlMapper {
    private static final String TAG = BeanEditView.class.getName();

    public interface BeanEditViewListener {
        void onSave(BaseBean item);
        void onCancel();
    }

    protected BeanEditViewListener beanEditViewListener;
    protected List<BeanColumnDescriptor> columns;
    protected BaseBean item;

    protected LinearLayout editControlLayout;
    protected Button okButton;
    protected Button cancelButton;
    protected BeanExternalController controller;
    protected BeanControlMap beanControlMap = new BeanControlMap();

    public BeanEditView(Context context) {
        super(context);
    }

    public BeanEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BeanEditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void initializeLayout() {
        inflate(getContext(), R.layout.bean_edit_view, this);
        this.editControlLayout = (LinearLayout)findViewById(R.id.bean_edit_layout);
        this.okButton = (Button)findViewById(R.id.bean_edit_ok_button);
        this.cancelButton = (Button)findViewById(R.id.bean_edit_cancel_button);

        this.okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                BeanEditView.this.save();
            }
        });

        this.cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                BeanEditView.this.cancel();
            }
        });

    }

    @Override
    public BeanControlMap getBeanControlMap() {
        return this.beanControlMap;
    }

    @Override
    public void destroy() {

    }

    public void bind(BaseBean item, boolean isNew, BeanExternalController controller,
                     int okButtonResId, int cancelButtonResId) {
        this.item = item;
        this.controller = controller;
        this.columns = MetadataUtils.getBeanColumnDescriptors(item, isNew);

        this.cancelButton.setText((cancelButtonResId != 0) ? cancelButtonResId : R.string.bean_edit_view_button_cancel);
        this.okButton.setText((okButtonResId != 0) ? okButtonResId : isNew ? R.string.bean_edit_view_ok_button_new : R.string.bean_edit_view_ok_button_edit);

        this.beanControlMap.clear();
        for (BeanColumnDescriptor descriptor : this.columns) {
            MetadataUtils.FrameworkType frameworkType = MetadataUtils.getFrameworkTypeByField(descriptor.getField());

            View view = MetadataUtils.getEditView(descriptor, frameworkType, this.getContext());
            if (view == null) {
                view = new UnsupportedEditControl(this.getContext());
            }

            if (view instanceof BeanEditControl) {

                BeanEditControl control = (BeanEditControl)view;
                this.beanControlMap.put(descriptor.getField().getName(), control);

                control.setBeanControlMapper(this);
                control.bind(descriptor);
                control.setBeanEditControlListener(new BeanEditControl.BeanEditControlListener() {

                    @Override
                    public void onChanged(BeanColumnDescriptor descriptor, Object value, boolean hasError) throws BeanValidatorException {
                        if (!hasError) {
                            valueChanged(descriptor, value);
                            validate();
                        } else {
                            setErrorState(true);
                        }
                    }

                    @Override
                    public void onError(BeanColumnDescriptor descriptor, Object value, String errorMsg) {
                        setErrorState(true);
                    }
                });
            }

            this.editControlLayout.addView(view);
        }

        if (this.controller != null) {
            this.controller.initialize(this.getContext(), item, this.beanControlMap);
        }

        validate();
    }

    protected void save() {
        populateBeanFromControls(this.item);

        if (this.beanEditViewListener != null) {
            this.beanEditViewListener.onSave(this.item);
        }
    }

    protected BaseBean getPopulatedCopy() {
        BaseBean bean = createEmptyBean();
        Field[] fields = ((Object)this.item).getClass().getDeclaredFields();
        for (Field field : fields) {
            if (BaseBean.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                try {
                    field.set(bean, field.get(this.item));
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Error Setting BaseBean", e);
                }
            }
        }


        populateBeanFromControls(bean);
        return  bean;
    }

    protected BaseBean createEmptyBean() {
        BaseBean bean = null;
        try {
            bean = (BaseBean)((Object)item).getClass().newInstance();
        } catch (InstantiationException e) {
            Log.e(TAG, "Create basebean object", e);
            setErrorState(true);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Create basebean object", e);
            setErrorState(true);
        }
        return bean;
    }

    protected void populateBeanFromControls(BaseBean bean) {
        try {
            int cnt = this.editControlLayout.getChildCount();
            for (int x = 0; x < cnt; x++) {
                View view = this.editControlLayout.getChildAt(x);
                if ( (view instanceof BeanEditControl) &&
                    !(view instanceof UnsupportedEditControl) ) {
                    BeanEditControl control = (BeanEditControl) view;
                    control.getDescriptor().getField().set(bean, control.getValue());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error Populating control", e);
            Toast.makeText(this.getContext(), e.getMessage(), Toast.LENGTH_LONG);
        }
    }


    protected void cancel() {
        if (this.beanEditViewListener != null) {
            this.beanEditViewListener.onCancel();
        }
    }

    protected void validate()  {
        boolean hasError = false;

        int cnt = this.editControlLayout.getChildCount();
        for (int x = 0; x < cnt; x++) {
            View view = this.editControlLayout.getChildAt(x);
            if (view.getVisibility() == VISIBLE) {
                if (view instanceof BeanEditControl) {
                    BeanEditControl control = (BeanEditControl) view;
                    try {
                        control.validate();
                    } catch (BeanValidatorException e) {
                        hasError = true;
                        break;
                    }
                }
            }
        }

        if (this.controller != null) {
            try {
                controller.validate(this.getContext(), getPopulatedCopy(), this.beanControlMap);
            } catch (BeanValidatorException e) {
                hasError = true;
            }
        }

        setErrorState(hasError);
    }

    protected void valueChanged(BeanColumnDescriptor descriptor, Object value) throws BeanValidatorException {
        if ( this.controller != null) {
            try {
                controller.onChanged(this.getContext(), descriptor, value, this.beanControlMap);
            } catch (BeanValidatorException e) {
                setErrorState(true);
                throw e;
            }
        }
    }

    protected void setErrorState(boolean hasError) {
        this.okButton.setEnabled(!hasError);
    }

    public void setBeanEditViewListener(BeanEditViewListener beanEditViewListener) {
        this.beanEditViewListener = beanEditViewListener;
    }
}

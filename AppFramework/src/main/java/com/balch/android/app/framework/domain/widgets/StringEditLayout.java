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

package com.balch.android.app.framework.domain.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.R;
import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.EditState;
import com.balch.android.app.framework.domain.ValidatorException;
import com.balch.android.app.framework.domain.ViewHint;

import java.util.ArrayList;
import java.util.List;

public class StringEditLayout extends LinearLayout implements EditLayout, TextWatcher {
    private static final String TAG = StringEditLayout.class.getSimpleName();
    protected static final int TEXT_CHANGE_DELAY_MS = 500;

    protected TextView label;
    protected EditText value;

    protected ColumnDescriptor descriptor;
    protected EditLayoutListener editLayoutListener;
    protected ControlMapper controlMapper;

    protected boolean allowEmpty = true;

    protected Handler textChangeHandler = new Handler();
    protected Runnable txtChangeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                doTextChanged();
            } catch (Exception ex) {
                Log.e(TAG, "Exception on TextChanged Runnable", ex);
            }

        }
    };

    public StringEditLayout(Context context) {
        super(context);
        initialize();
    }

    public StringEditLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public StringEditLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        inflate(getContext(), R.layout.edit_control_string, this);
        this.label = (TextView) findViewById(R.id.string_edit_control_label);
        this.value = (EditText) findViewById(R.id.string_edit_control_value);

        // manage saving the state from this class
        // the controls had the same id value which was
        // causing problems with the auto restore
        // see http://stackoverflow.com/a/9444190
        this.value.setSaveEnabled(false);
    }

    @Override
    public void bind(ColumnDescriptor descriptor) {
        this.descriptor = descriptor;
        this.label.setText(descriptor.getLabelResId());

        this.value.removeTextChangedListener(this);
        this.value.setLines(1);

        boolean enabled = (descriptor.getState() == EditState.CHANGEABLE);
        this.allowEmpty = true;
        List<InputFilter> filters = getInputFilters();
        try {
            Object obj = descriptor.getField().get(descriptor.getItem());
            this.value.setText(this.getValueAsString(obj));

            // check the hints associated with this field
            for (ViewHint hint : descriptor.getHints()) {
                if (hint.getHint() == ViewHint.Hint.MAX_CHARS) {
                    filters.add(new InputFilter.LengthFilter(hint.getIntValue()));
                } else if (hint.getHint() == ViewHint.Hint.DISPLAY_LINES) {
                    this.value.setLines(hint.getIntValue());
                } else if (hint.getHint() == ViewHint.Hint.NOT_EMPTY) {
                    this.allowEmpty = !hint.getBoolValue();
                } else if (hint.getHint() == ViewHint.Hint.INIT_EMPTY) {
                    if (hint.getBoolValue()) {
                        this.value.setText("");
                    }
                }
            }
        } catch (IllegalAccessException e) {
            this.value.setText("IllegalAccessException getting value");
            enabled = false;
        }
        this.value.setEnabled(enabled);
        this.value.setFilters(filters.toArray(new InputFilter[filters.size()]));

        this.value.addTextChangedListener(this);
    }

    @Override
    public void setControlMapper(ControlMapper controlMapper) {
        this.controlMapper = controlMapper;
    }

    @Override
    public void validate() throws ValidatorException {
        String val = this.value.getText().toString();
        // empty string validation
        if (!this.allowEmpty) {
            if (TextUtils.isEmpty(val)) {
                throw new ValidatorException(getResources().getString(R.string.error_empty_string));
            }
        }

    }

    protected List<InputFilter> getInputFilters() {
        return new ArrayList<>();
    }

    protected String getValueAsString(Object obj) {
        String value = "";
        if (obj != null) {
            value = obj.toString();
        }
        return value;
    }

    @Override
    public ColumnDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public Object getValue() {
        return this.value.getText().toString();
    }

    @Override
    public void setValue(Object value) {
        this.value.setText(this.getValueAsString(value));
    }

    @Override
    public void setEditControlListener(EditLayoutListener listener) {
        this.editLayoutListener = listener;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        this.textChangeHandler.removeCallbacks(this.txtChangeRunnable);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        this.textChangeHandler.postDelayed(this.txtChangeRunnable, TEXT_CHANGE_DELAY_MS);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.value = this.value.getText().toString();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.value.setText(ss.value);
    }

    protected void doTextChanged() {

        boolean hasError = false;
        try {
            this.validate();
            value.setError(null);
        } catch (ValidatorException e) {
            this.value.setError(e.getMessage());
            hasError = true;
        }

        if (this.editLayoutListener != null) {
            try {
                this.editLayoutListener.onChanged(this.descriptor, this.getValue(), hasError);
            } catch (ValidatorException e) {
                this.value.setError(e.getMessage());
            }
        }
    }

    static class SavedState extends BaseSavedState {
        protected String value;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.value = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(value);
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}

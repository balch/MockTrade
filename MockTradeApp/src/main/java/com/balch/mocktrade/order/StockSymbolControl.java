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

package com.balch.mocktrade.order;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.EditState;
import com.balch.android.app.framework.domain.ValidatorException;
import com.balch.android.app.framework.domain.ViewHint;
import com.balch.android.app.framework.domain.controls.ControlMapper;
import com.balch.android.app.framework.domain.controls.EditControl;
import com.balch.android.app.framework.model.ModelFactory;
import com.balch.android.app.framework.model.RequestListener;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.model.ModelProvider;

import java.util.ArrayList;
import java.util.List;

public class StockSymbolControl extends LinearLayout implements EditControl, TextWatcher {
    private static final String TAG = StockSymbolControl.class.getSimpleName();
    protected static final int TEXT_CHANGE_DELAY_MS = 500;

    protected TextView label;
    protected EditText value;
    protected TextView description;
    protected TextView price;

    protected ColumnDescriptor descriptor;
    protected EditControlListener editControlListener;
    protected ControlMapper controlMapper;

    protected FinanceModel financeModel;

    protected boolean allowEmpty;

    protected Handler textChangeHandler = new Handler();
    protected Runnable txtChangeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                doTextChanged();
            } catch (Exception ex) {
                Log.e(TAG, "Exception in TextChanged Runnable", ex);
            }
        }
    };

    public StockSymbolControl(Context context) {
        super(context);
        initialize();
    }

    public StockSymbolControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public StockSymbolControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        inflate(getContext(), com.balch.mocktrade.R.layout.symbol_edit_control, this);
        this.label = (TextView) findViewById(R.id.symbol_edit_label);
        this.value = (EditText) findViewById(R.id.symbol_edit_value);
        this.description = (TextView)findViewById(com.balch.mocktrade.R.id.symbol_edit_description);
        this.price = (TextView)findViewById(com.balch.mocktrade.R.id.symbol_edit_price);

        ModelFactory modelFactory;
        modelFactory = ((ModelProvider)this.getContext().getApplicationContext()).getModelFactory();
        this.financeModel = modelFactory.getModel(FinanceModel.class);

        this.value.setHint(R.string.order_symbol_hint);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.value.setEnabled(enabled);
    }

    protected void setInvestmentData(String description, Money price) {
        this.description.setText(description);
        this.price.setText((price != null) ? price.getFormatted(2) : "");
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
                }
            }
        } catch (IllegalAccessException e) {
            this.value.setText("IllegalAccessException getting value");
            enabled = false;
        }
        this.value.setEnabled(enabled);
        this.value.setFilters(filters.toArray(new InputFilter[filters.size()]));

        if (!TextUtils.isEmpty(this.value.getText())) {
            doTextChanged();
        }

        this.value.addTextChangedListener(this);
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
    public void validate() throws ValidatorException {
        String val = this.value.getText().toString();
        // empty string validation
        if (!this.allowEmpty) {
            if (TextUtils.isEmpty(val)) {
                throw new ValidatorException(getResources().getString(R.string.error_empty_string));
            }
        }
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
    public void setEditControlListener(EditControlListener listener) {
        this.editControlListener = listener;
    }

    @Override
    public void setControlMapper(ControlMapper controlMapper) {
        this.controlMapper = controlMapper;
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
        for (int x = 0; x < s.length(); x++) {
            char c = s.charAt(x);
            if (Character.isLowerCase(c)) {
                s.replace(x, x+1, String.valueOf(Character.toUpperCase(c)));
            }
        }
    }

    protected void doTextChanged() {
        final String symbol = value.getText().toString();

        boolean hasError = false;
        try {
            this.validate();
            value.setError(null);
        } catch (ValidatorException e) {
            this.value.setError(e.getMessage());
            hasError = true;
        }


        if (!hasError) {
            financeModel.getQuote(symbol, new RequestListener<Quote>() {
                @Override
                public void onResponse(final Quote response) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            setInvestmentData(response.getName(), response.getPrice());
                            value.setError(null);

                            callListenerOnChanged(false);
                        }
                    });
                }

                @Override
                public void onErrorResponse(final String error) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            setInvestmentData("", null);

                            String display = error;
                            int pos = display.indexOf('.');
                            if (pos != -1) {
                                display = display.substring(0, pos + 1);
                            }
                            value.setError(display);
                            callListenerOnChanged(true);
                            editControlListener.onError(descriptor, symbol, error);
//                        Toast.makeText(activity, error, Toast.LENGTH_LONG);
                        }});

                    }
                });

        }
    }

    protected void callListenerOnChanged(boolean hasError) {
        if (this.editControlListener != null) {
            try {
                this.editControlListener.onChanged(this.descriptor, this.getValue(), hasError);
            } catch (ValidatorException e) {
                this.value.setError(e.getMessage());
            }
        }
    }

    public Money getPrice() {
        return new Money(price.getText().toString());
    }
}

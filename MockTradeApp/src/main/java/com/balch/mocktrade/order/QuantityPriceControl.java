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
import android.graphics.Color;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.bean.BeanColumnDescriptor;
import com.balch.android.app.framework.bean.BeanEditState;
import com.balch.android.app.framework.bean.BeanValidatorException;
import com.balch.android.app.framework.bean.BeanViewHint;
import com.balch.android.app.framework.bean.controls.BeanControlMapper;
import com.balch.android.app.framework.bean.controls.BeanEditControl;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;

import java.util.ArrayList;
import java.util.List;

public class QuantityPriceControl extends LinearLayout implements BeanEditControl, TextWatcher {
    private static final String TAG = QuantityPriceControl.class.getName();
    protected static final int TEXT_CHANGE_DELAY_MS = 500;

    protected TextView label;
    protected EditText value;
    protected TextView availableFunds;
    protected LinearLayout availableFundsLayout;
    protected TextView cost;
    protected TextView costLabel;
    protected TextView marketClosedWarning;

    protected BeanColumnDescriptor descriptor;
    protected BeanEditControlListener beanEditControlListener;
    protected BeanControlMapper beanControlMapper;

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

    public QuantityPriceControl(Context context) {
        super(context);
        initialize();
    }

    public QuantityPriceControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public QuantityPriceControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        inflate(getContext(), R.layout.quantity_edit_control, this);
        this.label = (TextView) findViewById(R.id.quantity_edit_label);
        this.value = (EditText) findViewById(R.id.quantity_edit_value);

        this.availableFundsLayout = (LinearLayout)findViewById(R.id.quantity_edit_balance_layout);
        this.availableFunds = (TextView)findViewById(R.id.quantity_edit_balance);
        this.costLabel = (TextView)findViewById(R.id.quantity_edit_cost_label);
        this.cost = (TextView)findViewById(R.id.quantity_edit_cost);
        this.marketClosedWarning = (TextView)findViewById(R.id.quantity_edit_market_closed_text);
    }

    public void setAccountInfo(Account account) {
        this.availableFunds.setText(account.getAvailableFunds().getCurrency());
    }

    public void setOrderInfo(Order order) {
        boolean isBuy = (order.getAction()== Order.OrderAction.BUY);
        this.costLabel.setText(isBuy ? R.string.quantity_edit_control_cost_buy_label :
                R.string.quantity_edit_control_cost_sell_label);
        this.availableFundsLayout.setVisibility(isBuy ? VISIBLE : GONE);
    }

    public void setMarketIsOpen(boolean open) {
        this.marketClosedWarning.setVisibility(!open ? VISIBLE : GONE);
    }


    @Override
    public void bind(BeanColumnDescriptor descriptor) {
        this.descriptor = descriptor;
        this.label.setText(descriptor.getLabelResId());

        this.value.removeTextChangedListener(this);
        this.value.setLines(1);

        boolean enabled = (descriptor.getState() == BeanEditState.CHANGEABLE);
        List<InputFilter> filters = getInputFilters();
        try {
            Object obj = descriptor.getField().get(descriptor.getItem());
            this.value.setText(this.getValueAsString(obj));

            // check the hints associated with this field
            for (BeanViewHint hint : descriptor.getHints()) {
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
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.value.setEnabled(enabled);
    }

    protected List<InputFilter> getInputFilters() {
        return new ArrayList<InputFilter>();
    }

    protected String getValueAsString(Object obj) {
        String value = "";
        if (obj != null) {
            value = obj.toString();
        }
        return value;
    }

    @Override
    public BeanColumnDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public void validate() throws BeanValidatorException {
        String val = this.value.getText().toString();
        // empty string validation
        if (TextUtils.isEmpty(val)) {
            throw new BeanValidatorException(getResources().getString(R.string.error_empty_string));
        }
    }

    @Override
    public Long getValue() {
        String val = this.value.getText().toString();
        return TextUtils.isEmpty(val) ? 0 : Long.parseLong(val);
    }

    @Override
    public void setValue(Object value) {
        this.value.setText(this.getValueAsString(value));
    }

    @Override
    public void setBeanEditControlListener(BeanEditControlListener listener) {
        this.beanEditControlListener = listener;
    }

    @Override
    public void setBeanControlMapper(BeanControlMapper beanControlMapper) {
        this.beanControlMapper = beanControlMapper;
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

    protected void doTextChanged() {
        boolean hasError = false;
        try {
            this.validate();
            value.setError(null);
        } catch (BeanValidatorException e) {
            this.value.setError(e.getMessage());
            hasError = true;
        }

        if (this.beanEditControlListener != null) {
            try {
                this.beanEditControlListener.onChanged(this.descriptor, this.getValue(), hasError);
            } catch (BeanValidatorException e) {
                this.value.setError(e.getMessage());
            }
        }
    }

    public void setCost(Money cost, boolean hasAvailablefunds) {
        this.cost.setText(getCostSpan(cost, hasAvailablefunds));
    }

    protected Spannable getCostSpan(Money cost, boolean hasAvailablefunds) {
        ForegroundColorSpan spanColor = new ForegroundColorSpan(hasAvailablefunds ? Color.GREEN : Color.RED);

        String dollars = cost.getCurrency();
        SpannableStringBuilder spanString = new SpannableStringBuilder(dollars);
        spanString.setSpan(spanColor, 0, dollars.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }
}

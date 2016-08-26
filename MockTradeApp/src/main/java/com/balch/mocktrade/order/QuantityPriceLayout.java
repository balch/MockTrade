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
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.EditState;
import com.balch.android.app.framework.domain.ValidatorException;
import com.balch.android.app.framework.domain.ViewHint;
import com.balch.android.app.framework.domain.widget.ControlMapper;
import com.balch.android.app.framework.domain.widget.EditLayout;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.account.Account;

import java.util.ArrayList;
import java.util.List;

public class QuantityPriceLayout extends LinearLayout implements EditLayout, TextWatcher {
    private static final String TAG = QuantityPriceLayout.class.getSimpleName();
    protected static final int TEXT_CHANGE_DELAY_MS = 500;

    protected TextView mLabel;
    protected EditText mValue;
    protected TextView mAvailableFunds;
    protected LinearLayout mAvailableFundsLayout;
    protected TextView mCost;
    protected TextView mCostLabel;
    protected TextView mMarketClosedWarning;

    protected ColumnDescriptor mDescriptor;
    protected EditLayoutListener mEditLayoutListener;
    protected ControlMapper mControlMapper;

    protected Handler mTextChangeHandler = new Handler(Looper.getMainLooper());
    protected Runnable mTxtChangeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                doTextChanged();
            } catch (Exception ex) {
                Log.e(TAG, "Exception in TextChanged Runnable", ex);
            }
        }
    };

    public QuantityPriceLayout(Context context) {
        super(context);
        initialize();
    }

    public QuantityPriceLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public QuantityPriceLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int padding = getResources().getDimensionPixelSize(R.dimen.edit_control_padding);
        setPadding(0, padding, 0, padding);

        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.quantity_edit_control, this);
        this.mLabel = (TextView) findViewById(R.id.quantity_edit_label);
        this.mValue = (EditText) findViewById(R.id.quantity_edit_value);

        this.mAvailableFundsLayout = (LinearLayout)findViewById(R.id.quantity_edit_balance_layout);
        this.mAvailableFunds = (TextView)findViewById(R.id.quantity_edit_balance);
        this.mCostLabel = (TextView)findViewById(R.id.quantity_edit_cost_label);
        this.mCost = (TextView)findViewById(R.id.quantity_edit_cost);
        this.mMarketClosedWarning = (TextView)findViewById(R.id.quantity_edit_market_closed_text);
    }

    public void setAccountInfo(Account account) {
        this.mAvailableFunds.setText(account.getAvailableFunds().getFormatted());
    }

    public void setOrderInfo(Order order) {
        boolean isBuy = (order.getAction()== Order.OrderAction.BUY);
        this.mCostLabel.setText(isBuy ? R.string.quantity_edit_control_cost_buy_label :
                R.string.quantity_edit_control_cost_sell_label);
        this.mAvailableFundsLayout.setVisibility(isBuy ? VISIBLE : GONE);
    }

    public void setMarketIsOpen(boolean open) {
        this.mMarketClosedWarning.setVisibility(!open ? VISIBLE : GONE);
    }


    @Override
    public void bind(ColumnDescriptor descriptor) {
        this.mDescriptor = descriptor;
        this.mLabel.setText(descriptor.getLabelResId());

        this.mValue.removeTextChangedListener(this);
        this.mValue.setLines(1);

        boolean enabled = (descriptor.getState() == EditState.CHANGEABLE);
        List<InputFilter> filters = getInputFilters();
        try {
            Object obj = descriptor.getField().get(descriptor.getItem());
            this.mValue.setText(this.getValueAsString(obj));

            for (ViewHint hint : descriptor.getHints()) {
                if (hint.getHint() == ViewHint.Hint.INIT_EMPTY) {
                    if (hint.getBoolValue()) {
                        this.mValue.setText("");
                    }
                }
            }
        } catch (IllegalAccessException e) {
            this.mValue.setText("IllegalAccessException getting value");
            enabled = false;
        }
        this.mValue.setEnabled(enabled);
        this.mValue.setFilters(filters.toArray(new InputFilter[filters.size()]));

        this.mValue.addTextChangedListener(this);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mValue.setEnabled(enabled);
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
        return this.mDescriptor;
    }

    @Override
    public void validate() throws ValidatorException {
        String val = this.mValue.getText().toString();
        // empty string validation
        if (TextUtils.isEmpty(val)) {
            throw new ValidatorException(getResources().getString(R.string.error_empty_string));
        }
    }

    @Override
    public Long getValue() {
        String val = this.mValue.getText().toString();
        return TextUtils.isEmpty(val) ? 0 : Long.parseLong(val);
    }

    @Override
    public void setValue(Object value) {
        this.mValue.setText(this.getValueAsString(value));
    }

    @Override
    public void setEditControlListener(EditLayoutListener listener) {
        this.mEditLayoutListener = listener;
    }

    @Override
    public void setControlMapper(ControlMapper controlMapper) {
        this.mControlMapper = controlMapper;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        this.mTextChangeHandler.removeCallbacks(this.mTxtChangeRunnable);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        this.mTextChangeHandler.postDelayed(this.mTxtChangeRunnable, TEXT_CHANGE_DELAY_MS);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    protected void doTextChanged() {
        boolean hasError = false;
        try {
            this.validate();
            mValue.setError(null);
        } catch (ValidatorException e) {
            this.mValue.setError(e.getMessage());
            hasError = true;
        }

        if (this.mEditLayoutListener != null) {
            try {
                this.mEditLayoutListener.onChanged(this.mDescriptor, this.getValue(), hasError);
            } catch (ValidatorException e) {
                this.mValue.setError(e.getMessage());
            }
        }
    }

    public void setCost(Money cost, boolean hasAvailablefunds) {
        this.mCost.setText(getCostSpan(cost, hasAvailablefunds));
    }

    protected Spannable getCostSpan(Money cost, boolean hasAvailablefunds) {
        ForegroundColorSpan spanColor = new ForegroundColorSpan(hasAvailablefunds ? Color.GREEN : Color.RED);

        String dollars = cost.getFormatted();
        SpannableStringBuilder spanString = new SpannableStringBuilder(dollars);
        spanString.setSpan(spanColor, 0, dollars.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return spanString;
    }
}

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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.balch.mocktrade.TradeModelProvider;
import com.balch.mocktrade.R;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.GoogleFinanceModel;
import com.balch.mocktrade.finance.Quote;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class StockSymbolLayout extends LinearLayout implements EditLayout, TextWatcher {
    private static final String TAG = StockSymbolLayout.class.getSimpleName();
    private static final int TEXT_CHANGE_DELAY_MS = 500;

    private TextView mLabel;
    private EditText mValue;
    private TextView mDescription;
    private TextView mPrice;

    private ColumnDescriptor mDescriptor;
    private EditLayoutListener mEditLayoutListener;

    private FinanceModel mFinanceModel;

    private boolean mAllowEmpty;

    private Handler mTextChangeHandler = new Handler(Looper.getMainLooper());
    private Runnable mTxtChangeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                doTextChanged();
            } catch (Exception ex) {
                Log.e(TAG, "Exception in TextChanged Runnable", ex);
            }
        }
    };

    public StockSymbolLayout(Context context) {
        super(context);
        initialize();
    }

    public StockSymbolLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public StockSymbolLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int padding = getResources().getDimensionPixelSize(R.dimen.edit_control_padding);
        setPadding(0, padding, 0, padding);


        setOrientation(VERTICAL);
        inflate(getContext(), com.balch.mocktrade.R.layout.symbol_edit_control, this);
        this.mLabel = (TextView) findViewById(R.id.symbol_edit_label);
        this.mValue = (EditText) findViewById(R.id.symbol_edit_value);
        this.mDescription = (TextView)findViewById(com.balch.mocktrade.R.id.symbol_edit_description);
        this.mPrice = (TextView)findViewById(com.balch.mocktrade.R.id.symbol_edit_price);

        TradeModelProvider modelProvider = (TradeModelProvider)this.getContext().getApplicationContext();
        this.mFinanceModel = new GoogleFinanceModel(modelProvider.getContext(),
                modelProvider.getNetworkRequestProvider(), modelProvider.getSettings());

        this.mValue.setHint(R.string.order_symbol_hint);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mValue.setEnabled(enabled);
    }

    protected void setInvestmentData(String description, Money price) {
        this.mDescription.setText(description);
        this.mPrice.setText((price != null) ? price.getFormatted(2) : "");
    }

    @Override
    public void bind(ColumnDescriptor descriptor) {
        this.mDescriptor = descriptor;
        this.mLabel.setText(descriptor.getLabelResId());

        this.mValue.removeTextChangedListener(this);
        this.mValue.setLines(1);

        boolean enabled = (descriptor.getState() == EditState.CHANGEABLE);
        this.mAllowEmpty = true;
        List<InputFilter> filters = getInputFilters();
        try {
            if (descriptor.getField() != null) {
                Object obj = descriptor.getField().get(descriptor.getItem());
                this.mValue.setText(this.getValueAsString(obj));
            }

            // check the hints associated with this field
            for (ViewHint hint : descriptor.getHints()) {
                if (hint.getHint() == ViewHint.Hint.MAX_CHARS) {
                    filters.add(new InputFilter.LengthFilter(hint.getIntValue()));
                } else if (hint.getHint() == ViewHint.Hint.DISPLAY_LINES) {
                    this.mValue.setLines(hint.getIntValue());
                } else if (hint.getHint() == ViewHint.Hint.NOT_EMPTY) {
                    this.mAllowEmpty = !hint.getBoolValue();
                }
            }
        } catch (IllegalAccessException e) {
            Log.e(TAG, "bind error", e);
            this.mValue.setText(R.string.get_value_error);
            enabled = false;
        }
        this.mValue.setEnabled(enabled);
        this.mValue.setFilters(filters.toArray(new InputFilter[filters.size()]));

        if (!TextUtils.isEmpty(this.mValue.getText())) {
            doTextChanged();
        }

        this.mValue.addTextChangedListener(this);
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
        if (!this.mAllowEmpty) {
            if (TextUtils.isEmpty(val)) {
                throw new ValidatorException(getResources().getString(R.string.error_empty_string));
            }
        }
    }

    @Override
    public Object getValue() {
        return this.mValue.getText().toString();
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
        for (int x = 0; x < s.length(); x++) {
            char c = s.charAt(x);
            if (Character.isLowerCase(c)) {
                s.replace(x, x+1, String.valueOf(Character.toUpperCase(c)));
            }
        }
    }

    protected void doTextChanged() {
        final String symbol = mValue.getText().toString();

        boolean hasError = false;
        try {
            this.validate();
            mValue.setError(null);
        } catch (ValidatorException e) {
            this.mValue.setError(e.getMessage());
            hasError = true;
        }


        if (!hasError) {
            new GetQuoteTask(this, mFinanceModel).execute(symbol);
        }
    }

    protected void callListenerOnChanged(boolean hasError) {
        if (this.mEditLayoutListener != null) {
            try {
                this.mEditLayoutListener.onChanged(this.mDescriptor, this.getValue(), hasError);
            } catch (ValidatorException e) {
                this.mValue.setError(e.getMessage());
            }
        }
    }

    public Money getPrice() {
        return new Money(mPrice.getText().toString());
    }

    private static class GetQuoteTask extends AsyncTask<String, Void, Quote> {
        private final FinanceModel mFinanceModel;
        private final WeakReference<StockSymbolLayout> mStockSymbolLayout;

        GetQuoteTask(StockSymbolLayout layout, FinanceModel financeModel) {
            mStockSymbolLayout = new WeakReference<>(layout);
            mFinanceModel = financeModel;
        }

        @Override
        protected Quote doInBackground(String... symbols) {
            return mFinanceModel.getQuote(symbols[0]);
        }

        @Override
        protected void onPostExecute(Quote quote) {
            final StockSymbolLayout layout = mStockSymbolLayout.get();
            if (layout != null) {
                if (quote != null) {
                    layout.setInvestmentData(quote.getName(), quote.getPrice());
                    layout.mValue.setError(null);
                    layout.callListenerOnChanged(false);
                } else {
                    layout.setInvestmentData("", null);
                    String message = layout.getResources().getString(R.string.error_invalid_symbol);
                    layout.mValue.setError(message);
                    layout.callListenerOnChanged(true);
                }
            }
        }

    }

}

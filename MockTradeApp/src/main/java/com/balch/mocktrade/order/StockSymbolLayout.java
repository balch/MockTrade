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
import com.balch.mocktrade.finance.GoogleFinanceApi;
import com.balch.mocktrade.finance.GoogleFinanceModel;
import com.balch.mocktrade.finance.Quote;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class StockSymbolLayout extends LinearLayout implements EditLayout, TextWatcher {
    private static final String TAG = StockSymbolLayout.class.getSimpleName();
    private static final int TEXT_CHANGE_DELAY_MS = 500;

    private TextView symbolLabel;
    private EditText symbolValue;
    private TextView symbolDescription;
    private TextView symbolPrice;

    private ColumnDescriptor columnDescriptor;
    private EditLayoutListener editLayoutListener;

    private FinanceModel financeModel;

    private boolean allowEmpty;

    private Disposable disposableGetQuote = null;

    private Handler textChangeHandler = new Handler(Looper.getMainLooper());
    private Runnable textChangeRunnable = new Runnable() {
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
        symbolLabel = (TextView) findViewById(R.id.symbol_edit_label);
        symbolValue = (EditText) findViewById(R.id.symbol_edit_value);
        symbolDescription = (TextView)findViewById(com.balch.mocktrade.R.id.symbol_edit_description);
        symbolPrice = (TextView)findViewById(com.balch.mocktrade.R.id.symbol_edit_price);

        TradeModelProvider modelProvider = (TradeModelProvider)this.getContext().getApplicationContext();
        financeModel = new GoogleFinanceModel(modelProvider.getContext(),
                modelProvider.getModelApiFactory().getModelApi(GoogleFinanceApi.class),
                modelProvider.getSettings());

        symbolValue.setHint(R.string.order_symbol_hint);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        symbolValue.setEnabled(enabled);
    }

    protected void setInvestmentData(String description, Money price) {
        symbolDescription.setText(description);
        symbolPrice.setText((price != null) ? price.getFormatted(2) : "");
    }

    @Override
    public void bind(ColumnDescriptor descriptor) {
        columnDescriptor = descriptor;
        symbolLabel.setText(descriptor.getLabelResId());

        symbolValue.removeTextChangedListener(this);
        symbolValue.setLines(1);

        boolean enabled = (descriptor.getState() == EditState.CHANGEABLE);
        allowEmpty = true;
        List<InputFilter> filters = getInputFilters();
        try {
            if (descriptor.getField() != null) {
                Object obj = descriptor.getField().get(descriptor.getItem());
                symbolValue.setText(this.getValueAsString(obj));
            }

            // check the hints associated with this field
            for (ViewHint hint : descriptor.getHints()) {
                if (hint.getHint() == ViewHint.Hint.MAX_CHARS) {
                    filters.add(new InputFilter.LengthFilter(hint.getIntValue()));
                } else if (hint.getHint() == ViewHint.Hint.DISPLAY_LINES) {
                    symbolValue.setLines(hint.getIntValue());
                } else if (hint.getHint() == ViewHint.Hint.NOT_EMPTY) {
                    allowEmpty = !hint.getBoolValue();
                }
            }
        } catch (IllegalAccessException e) {
            Log.e(TAG, "bind error", e);
            symbolValue.setText(R.string.get_value_error);
            enabled = false;
        }
        symbolValue.setEnabled(enabled);
        symbolValue.setFilters(filters.toArray(new InputFilter[filters.size()]));

        if (!TextUtils.isEmpty(this.symbolValue.getText())) {
            doTextChanged();
        }

        this.symbolValue.addTextChangedListener(this);
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
        return this.columnDescriptor;
    }

    @Override
    public void validate() throws ValidatorException {
        String val = symbolValue.getText().toString();
        // empty string validation
        if (!this.allowEmpty) {
            if (TextUtils.isEmpty(val)) {
                throw new ValidatorException(getResources().getString(R.string.error_empty_string));
            }
        }
    }

    @Override
    public Object getValue() {
        return symbolValue.getText().toString();
    }

    @Override
    public void setValue(Object value) {
        this.symbolValue.setText(this.getValueAsString(value));
    }

    @Override
    public void setEditControlListener(EditLayoutListener listener) {
        this.editLayoutListener = listener;
    }

    @Override
    public void setControlMapper(ControlMapper controlMapper) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        textChangeHandler.removeCallbacks(this.textChangeRunnable);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        textChangeHandler.postDelayed(this.textChangeRunnable, TEXT_CHANGE_DELAY_MS);
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
        final String symbol = symbolValue.getText().toString();

        boolean hasError = false;
        try {
            validate();
            symbolValue.setError(null);
        } catch (ValidatorException e) {
            symbolValue.setError(e.getMessage());
            hasError = true;
        }


        if (!hasError) {
            getQuoteAsync(symbol);
        }
    }

    protected void callListenerOnChanged(boolean hasError) {
        if (editLayoutListener != null) {
            try {
                editLayoutListener.onChanged(this.columnDescriptor, this.getValue(), hasError);
            } catch (ValidatorException e) {
                symbolValue.setError(e.getMessage());
            }
        }
    }

    public Money getPrice() {
        return new Money(symbolPrice.getText().toString());
    }

    private void disposeGetQuote() {
        if (disposableGetQuote != null) {
            disposableGetQuote.dispose();
            disposableGetQuote = null;
        }
    }

    private void getQuoteAsync(String symbol) {
        disposeGetQuote();
        disposableGetQuote = financeModel.getQuote(symbol)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Quote>() {
                               @Override
                               public void accept(@NonNull Quote quote) throws Exception {
                                   setInvestmentData(quote.getName(), quote.getPrice());
                                   symbolValue.setError(null);
                                   callListenerOnChanged(false);
                               }
                           },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(@NonNull Throwable throwable) throws Exception {
                                    Log.e(TAG, "getQuoteAsync exception", throwable );
                                    setInvestmentData("", null);
                                    String message = getResources().getString(R.string.error_invalid_symbol);
                                    symbolValue.setError(message);
                                    callListenerOnChanged(true);
                                }
                            });
    }

    @Override
    protected void onDetachedFromWindow() {
        disposeGetQuote();
        super.onDetachedFromWindow();
    }

}

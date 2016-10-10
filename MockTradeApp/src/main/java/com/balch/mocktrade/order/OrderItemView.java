/*
 * Author: Balch
 * Created: 9/8/14 5:55 PM
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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.mocktrade.R;

import java.text.DateFormat;


public class OrderItemView extends LinearLayout {

    public interface OrderItemViewListener {
        boolean onCancelOrder(Order order);
    }

    private OrderItemViewListener mOrderItemViewListener;
    private TextView mOrderId;
    private TextView mSymbol;
    private TextView mAction;
    private TextView mCreateDate;
    private TextView mStrategy;
    private TextView mQuantity;
    private Order mOrder;

    public OrderItemView(Context context) {
        super(context);
        initialize();
    }

    public OrderItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public OrderItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientation(HORIZONTAL);

        int padding = getResources().getDimensionPixelSize(R.dimen.edit_control_padding);
        setPadding(padding, padding, padding, padding);

        inflate(getContext(), R.layout.order_item_view, this);

        this.mOrderId = (TextView)findViewById(R.id.order_item_id);
        this.mSymbol = (TextView)findViewById(R.id.order_item_symbol);
        this.mAction = (TextView)findViewById(R.id.order_item_action);
        this.mCreateDate = (TextView)findViewById(R.id.order_item_created);
        this.mStrategy = (TextView)findViewById(R.id.order_item_strategy);
        this.mQuantity = (TextView)findViewById(R.id.order_item_quantity);

        this.setLongClickable(true);
        this.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                boolean handled = false;
                if (mOrderItemViewListener != null) {
                    handled = mOrderItemViewListener.onCancelOrder(mOrder);
                }
                return handled;
            }
        });

    }

    public void bind(Order order) {
        this.mOrder = order;

        this.mOrderId.setText(order.getId().toString());
        this.mSymbol.setText(order.getSymbol());
        this.mAction.setText(order.getAction().toString());
        this.mCreateDate.setText(DateFormat.getDateTimeInstance().format(order.getCreateTime()));
        this.mStrategy.setText(order.getStrategy().toString());
        this.mQuantity.setText(String.valueOf(order.getQuantity()));

    }

    public void setOrderItemViewListener(OrderItemViewListener listener) {
        this.mOrderItemViewListener = listener;
    }
}


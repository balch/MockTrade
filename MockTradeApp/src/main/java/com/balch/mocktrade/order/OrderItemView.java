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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.mocktrade.R;

import java.text.DateFormat;


public class OrderItemView extends LinearLayout {

    public interface OrderItemViewListener {
        boolean onCancelOrder(Order order);
    }

    protected OrderItemViewListener listener;
    protected TextView orderId;
    protected TextView symbol;
    protected TextView action;
    protected TextView createDate;
    protected TextView strategy;
    protected TextView quantity;
    protected Order order;

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
        inflate(getContext(), R.layout.order_item_view, this);

        this.orderId = (TextView)findViewById(R.id.order_item_id);
        this.symbol = (TextView)findViewById(R.id.order_item_symbol);
        this.action = (TextView)findViewById(R.id.order_item_action);
        this.createDate = (TextView)findViewById(R.id.order_item_created);
        this.strategy = (TextView)findViewById(R.id.order_item_strategy);
        this.quantity = (TextView)findViewById(R.id.order_item_quantity);

        this.setLongClickable(true);
        this.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                boolean handled = false;
                if (listener != null) {
                    handled = listener.onCancelOrder(order);
                }
                return handled;
            }
        });

    }

    public void bind(Order order) {
        this.order = order;

        this.orderId.setText(order.getId().toString());
        this.symbol.setText(order.getSymbol());
        this.action.setText(order.getAction().toString());
        this.createDate.setText(DateFormat.getDateTimeInstance().format(order.getCreateTime().getDate()));
        this.strategy.setText(order.getStrategy().toString());
        this.quantity.setText(order.getQuantity().toString());

    }

    public void setOrderItemViewListener(OrderItemViewListener listener) {
        this.listener = listener;
    }
}


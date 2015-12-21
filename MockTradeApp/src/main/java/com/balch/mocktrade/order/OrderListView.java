/*
 * Author: Balch
 * Created: 9/8/14 3:39 PM
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
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.balch.android.app.framework.view.BaseView;
import com.balch.mocktrade.R;

import java.util.List;

public class OrderListView extends LinearLayout implements BaseView {

    protected OrderItemView.OrderItemViewListener listener;
    protected OrderListAdapter adapter;
    protected ListView list;

    public OrderListView(Context context) {
        super(context);
    }

    public OrderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OrderListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void initializeLayout() {
        inflate(getContext(), R.layout.order_list_view, this);
        this.list = (ListView)findViewById(R.id.order_list);
    }

    public void setOrderItemViewListener(OrderItemView.OrderItemViewListener listener) {
        this.listener = listener;
    }

    public void bind(List<Order> orders) {
        this.adapter = new OrderListAdapter(this.getContext(), orders);
        this.list.setAdapter(adapter);
    }

    private class OrderListAdapter extends  ArrayAdapter<Order> {

        public OrderListAdapter(Context context, List<Order> orders) {
            super(context, 0, orders);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            OrderItemView view = (OrderItemView) convertView;
            if (view == null) {
                view = new OrderItemView(this.getContext());
            }

            view.setOrderItemViewListener(listener);
            view.bind(getItem(position));
            return view;
        }
    }

}

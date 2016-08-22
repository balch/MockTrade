/*
 * Author: Balch
 * Created: 8/21/16 7:06 AM
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
 * Copyright (C) 2016
 *
 */

package com.balch.mocktrade;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import com.balch.mocktrade.shared.HighlightItem;
import com.balch.mocktrade.shared.PerformanceItem;
import com.balch.mocktrade.shared.widget.DailyGraphView;

import java.util.ArrayList;
import java.util.List;

public class GraphActivity extends WearableActivity {

    private final static String EXTRA_PERFORMANCE_ITEMS = "extra_performance_items";
    private final static String EXTRA_HIGHLIGHT_ITEM = "extra_highlight_item";

    public static Intent newIntent(Context context, HighlightItem item, ArrayList<PerformanceItem> performanceItems) {
        Intent intent = new Intent(context, GraphActivity.class);
        intent.putExtra(EXTRA_PERFORMANCE_ITEMS, performanceItems);
        intent.putExtra(EXTRA_HIGHLIGHT_ITEM, item);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_activity);

        setAmbientEnabled();

        Intent intent = getIntent();
        List<PerformanceItem> performanceItems = intent.getParcelableArrayListExtra(EXTRA_PERFORMANCE_ITEMS);
        HighlightItem highlightItem = intent.getParcelableExtra(EXTRA_HIGHLIGHT_ITEM);
        DailyGraphView graphView = (DailyGraphView) findViewById(R.id.graph_view);
        graphView.bind(performanceItems);

        TextView titleTextView = (TextView) findViewById(R.id.graph_title);
        String text = (highlightItem.getHighlightType() != HighlightItem.HighlightType.TOTAL_ACCOUNT) ?
                highlightItem.getDescription() : highlightItem.getSymbol();

        titleTextView.setText(text);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        finish();
    }

}

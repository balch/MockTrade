/*
 * Author: Balch
 * Created: 8/15/16 6:38 AM
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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.mocktrade.shared.WatchConfigItem;
import com.balch.mocktrade.shared.WearDataSync;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

public class MockTradeWatchConfigActivity extends Activity implements
        WearableListView.ClickListener, WearableListView.OnScrollListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MockTradeWatchConfig";

    private GoogleApiClient mGoogleApiClient;
    private TextView mHeader;
    private ConfigItemAdapter mConfigItemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_watchface_activity);

        mHeader = (TextView) findViewById(R.id.config_watch_title);
        WearableListView listView = (WearableListView) findViewById(R.id.config_watch_list);
        BoxInsetLayout content = (BoxInsetLayout) findViewById(R.id.content);
        // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    v.setPaddingRelative(
                            getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                            v.getPaddingTop(),
                            v.getPaddingEnd(),
                            v.getPaddingBottom());
                }
                return v.onApplyWindowInsets(insets);
            }
        });

        listView.setHasFixedSize(true);
        listView.setClickListener(this);
        listView.addOnScrollListener(this);

        mConfigItemAdapter = new ConfigItemAdapter(new ConfigItemAdapter.ConfigItemAdapterListener() {
            @Override
            public void onConfigItemChanged(WatchConfigItem item) {
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearDataSync.PATH_WATCH_CONFIG_SET);
                putDataMapRequest.getDataMap().putDataMap(WearDataSync.DATA_WATCH_CONFIG_DATA_ITEM, item.toDataMap());
                putDataMapRequest.setUrgent();
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest());
            }
        });
        listView.setAdapter(mConfigItemAdapter);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override // WearableListView.ClickListener
    public void onClick(WearableListView.ViewHolder viewHolder) {
        ConfigItemViewHolder configItemViewHolder = (ConfigItemViewHolder) viewHolder;
//        updateConfigDataItem(configItemViewHolder.mConfigItem);
        finish();
    }

    @Override // WearableListView.ClickListener
    public void onTopEmptyRegionClick() {}

    @Override // WearableListView.OnScrollListener
    public void onScroll(int scroll) {}

    @Override // WearableListView.OnScrollListener
    public void onAbsoluteScrollChange(int scroll) {
        float newTranslation = Math.min(-scroll, 0);
        mHeader.setTranslationY(newTranslation);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed: " + result);
}

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
        PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
        results.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for  (int x = 0; x < dataItems.getCount(); x++) {
                    DataItem dataItem = dataItems.get(x);

                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap dataMap = dataMapItem.getDataMap();

                    String uriPath = dataItem.getUri().getPath();
                    if (uriPath.equals(WearDataSync.PATH_WATCH_CONFIG_SYNC)) {
                        ArrayList<DataMap> dataMapList = dataMap.getDataMapArrayList(WearDataSync.DATA_WATCH_CONFIG_DATA_ITEMS);
                        if (dataMapList != null) {
                            for (DataMap dm : dataMapList) {
                                mConfigItemAdapter.add(new WatchConfigItem(dm));
                            }
                            mConfigItemAdapter.notifyDataSetChanged();
                        }
                    }
                }

                dataItems.release();
            }
        });
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }


    @Override // WearableListView.OnScrollListener
    public void onScrollStateChanged(int scrollState) {}

    @Override // WearableListView.OnScrollListener
    public void onCentralPositionChanged(int centralPosition) {}

    private void updateConfigDataItem(final WatchConfigItem configItem) {
    }

    private static class ConfigItemAdapter extends WearableListView.Adapter {
        public interface ConfigItemAdapterListener {
            void onConfigItemChanged(WatchConfigItem item);
        }

        private List<WatchConfigItem> mConfigItems = new ArrayList<>();
        private final ConfigItemAdapterListener listener;

        public ConfigItemAdapter(ConfigItemAdapterListener listener) {
            this.listener = listener;
        }

        public void add(WatchConfigItem item) {
            this.mConfigItems.add(item);
        }

        public void addAll(List<WatchConfigItem> items) {
            this.mConfigItems.addAll(items);
        }

        @Override
        public ConfigItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ConfigItemViewHolder(new ConfigItemView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, final int position) {
            final ConfigItemViewHolder configItemViewHolder = (ConfigItemViewHolder) holder;
            final WatchConfigItem configItem = mConfigItems.get(position);
            configItemViewHolder.mConfigItem.bind(configItem, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    configItem.setEnabled(!configItem.isEnabled());
                    configItemViewHolder.mConfigItem.setConfigItemEnabled(configItem.isEnabled());
                    listener.onConfigItemChanged(configItem);
                }
            });

            RecyclerView.LayoutParams layoutParams =
                    new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            int itemMargin = (int) ((ConfigItemViewHolder) holder).itemView.getResources()
                    .getDimension(R.dimen.digital_config_color_picker_item_margin);
            // Add margins to first and last item to make it possible for user to tap on them.
            if (position == 0) {
                layoutParams.setMargins(0, itemMargin, 0, 0);
            } else if (position == mConfigItems.size() - 1) {
                layoutParams.setMargins(0, 0, 0, itemMargin);
            } else {
                layoutParams.setMargins(0, 0, 0, 0);
            }
            configItemViewHolder.itemView.setLayoutParams(layoutParams);
        }

        @Override
        public int getItemCount() {
            return mConfigItems.size();
        }
    }

    private static class ConfigItemView extends LinearLayout {
        private final TextView mDescription;
        private final CircledImageView mEnabledImage;

        public ConfigItemView(Context context) {
            super(context);
            View.inflate(context, R.layout.config_watchface_item, this);

            mDescription = (TextView) findViewById(R.id.config_watch_item_text);
            mEnabledImage = (CircledImageView) findViewById(R.id.config_watch_item_image);
        }

        public void bind(WatchConfigItem item, OnClickListener clickListener) {
            mDescription.setText(item.getDescription());
            mEnabledImage.setOnClickListener(clickListener);
            setConfigItemEnabled(item.isEnabled());
        }

        public void setConfigItemEnabled(boolean enabled) {
            mEnabledImage.setImageResource(enabled ? R.drawable.sign_check : R.drawable.sign_error);
        }

    }

    private static class ConfigItemViewHolder extends WearableListView.ViewHolder {
        private final ConfigItemView mConfigItem;

        public ConfigItemViewHolder(ConfigItemView configItem) {
            super(configItem);
            mConfigItem = configItem;
        }
    }

}

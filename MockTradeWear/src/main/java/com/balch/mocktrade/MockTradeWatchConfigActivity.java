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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.DefaultOffsettingHelper;
import android.support.wearable.view.WearableRecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.balch.mocktrade.shared.WatchConfigItem;
import com.balch.mocktrade.shared.WearDataSync;
import com.balch.mocktrade.shared.utils.VersionUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

public class MockTradeWatchConfigActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MockTradeWatchConfig";

    private GoogleApiClient mGoogleApiClient;
    private ConfigItemAdapter mConfigItemAdapter;
    private Node mCompanionNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_watchface_activity);

        WearableRecyclerView wearableRecyclerView = (WearableRecyclerView) findViewById(R.id.config_watch_list);
        wearableRecyclerView.setHasFixedSize(true);
        wearableRecyclerView.setCircularScrollingGestureEnabled(true);
        wearableRecyclerView.setBezelWidth(0.5f);
        wearableRecyclerView.setScrollDegreesPerScreen(90);

        wearableRecyclerView.setOffsettingHelper(new OffsettingHelper());

        String version = "Version: " + VersionUtils.getVersion(this, BuildConfig.DEBUG);

        mConfigItemAdapter = new ConfigItemAdapter(version,
                new ConfigItemAdapter.ConfigItemAdapterListener() {
                    @Override
                    public void onConfigItemChanged(WatchConfigItem item) {
                        if (mCompanionNode != null && mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

                            Wearable.MessageApi.sendMessage(mGoogleApiClient, mCompanionNode.getId(),
                                    WearDataSync.MSG_WATCH_CONFIG_SET, item.toDataMap().toByteArray()).setResultCallback(

                                    new ResultCallback<MessageApi.SendMessageResult>() {
                                        @Override
                                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {

                                            if (!sendMessageResult.getStatus().isSuccess()) {
                                                Log.e("TAG", "Failed to send message with status code: "
                                                        + sendMessageResult.getStatus().getStatusCode());
                                            }
                                        }
                                    }
                            );
                        }
                    }
                });
        wearableRecyclerView.setAdapter(mConfigItemAdapter);

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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed: " + result);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);

        mCompanionNode = null;
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetConnectedNodesResult nodesResult) {
                List<Node> nodes = nodesResult.getNodes();
                if (!nodes.isEmpty()) {
                    for (Node node : nodes) {
                        if (node.isNearby()) {
                            mCompanionNode = node;
                            break;
                        }

                        if (mCompanionNode == null) {
                            mCompanionNode = nodes.get(0);
                        }
                    }
                }

                mCompanionNode = !nodes.isEmpty() ? nodes.get(0) : null;
            }
        });

        PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
        results.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(@NonNull DataItemBuffer dataItems) {
                for (int x = 0; x < dataItems.getCount(); x++) {
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

    private static class ConfigItemAdapter extends WearableRecyclerView.Adapter {

        private static final int VIEW_TYPE_STATIC = 0;
        private static final int VIEW_TYPE_CONFIG_ITEM = 1;

        private final ConfigItemAdapterListener listener;
        private List<Object> mConfigItems = new ArrayList<>();

        ConfigItemAdapter(String version, ConfigItemAdapterListener listener) {
            this.listener = listener;
            this.mConfigItems.add(version);
        }

        public void add(WatchConfigItem item) {
            this.mConfigItems.add(item);
        }

        @Override
        public WearableRecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            WearableRecyclerView.ViewHolder viewHolder;

            switch (viewType) {
                case VIEW_TYPE_STATIC:
                    viewHolder = new StaticItemViewHolder(parent);
                    break;
                case VIEW_TYPE_CONFIG_ITEM:
                    viewHolder = new ConfigItemViewHolder(parent);
                    break;
                default:
                    throw new IllegalArgumentException("invalid viewtype=" + viewType);
            }
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            if (holder instanceof StaticItemViewHolder) {
                final StaticItemViewHolder staticItemViewHolder = (StaticItemViewHolder) holder;
                staticItemViewHolder.bind((String) mConfigItems.get(position));

            } else if (holder instanceof ConfigItemViewHolder) {
                final ConfigItemViewHolder configItemViewHolder = (ConfigItemViewHolder) holder;
                final WatchConfigItem configItem = (WatchConfigItem) mConfigItems.get(position);
                configItemViewHolder.bind(configItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        configItem.setEnabled(!configItem.isEnabled());
                        configItemViewHolder.setConfigItemEnabled(configItem.isEnabled());
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
        }

        @Override
        public int getItemCount() {
            return mConfigItems.size();
        }

        @Override
        public int getItemViewType(int position) {
            int type;

            Object obj = mConfigItems.get(position);
            if (obj instanceof String) {
                type = VIEW_TYPE_STATIC;
            } else if (obj instanceof WatchConfigItem) {
                type = VIEW_TYPE_CONFIG_ITEM;
            } else {
                throw new IllegalArgumentException(obj.getClass().getName() + " not supported");
            }
            return type;
        }


        interface ConfigItemAdapterListener {
            void onConfigItemChanged(WatchConfigItem item);
        }
    }

    private static class ConfigItemViewHolder extends WearableRecyclerView.ViewHolder {
        private final TextView mDescription;
        private final CircledImageView mEnabledImage;

        ConfigItemViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.config_watchface_item, parent, false));

            mDescription = (TextView) itemView.findViewById(R.id.config_watch_item_text);
            mEnabledImage = (CircledImageView) itemView.findViewById(R.id.config_watch_item_image);

        }

        void bind(WatchConfigItem item, View.OnClickListener clickListener) {
            mDescription.setText(item.getDescription());
            mEnabledImage.setOnClickListener(clickListener);
            setConfigItemEnabled(item.isEnabled());
        }

        void setConfigItemEnabled(boolean enabled) {
            mEnabledImage.setImageResource(enabled ? R.drawable.sign_check : R.drawable.sign_error);
        }
    }

    private static class StaticItemViewHolder extends WearableRecyclerView.ViewHolder {
        private final TextView mDescription;

        StaticItemViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.static_watchface_item, parent, false));

            mDescription = (TextView) itemView.findViewById(R.id.static_watch_item_text);
        }

        void bind(String text) {
            mDescription.setText(text);
        }
    }

    public static class OffsettingHelper extends DefaultOffsettingHelper {

        /**
         * How much should we scale the icon at most.
         */
        private static final float MAX_ICON_PROGRESS = 0.65f;

        private float mProgressToCenter;

        OffsettingHelper() {
        }

        @Override

        public void updateChild(View child, WearableRecyclerView parent) {
            super.updateChild(child, parent);

            // Figure out % progress from top to bottom
            float centerOffset = ((float) child.getHeight() / 2.0f) / (float) parent.getHeight();
            float yRelativeToCenterOffset = (child.getY() / parent.getHeight()) + centerOffset;

            // Normalize for center
            mProgressToCenter = Math.abs(0.5f - yRelativeToCenterOffset);
            // Adjust to the maximum scale
            mProgressToCenter = Math.min(mProgressToCenter, MAX_ICON_PROGRESS);

            child.setScaleX(1 - mProgressToCenter);
            child.setScaleY(1 - mProgressToCenter);
        }


        @Override
        public void adjustAnchorOffsetXY(View child, float[] anchorOffsetXY) {
            anchorOffsetXY[0] = child.getHeight() / 2.0f;
        }
    }
}

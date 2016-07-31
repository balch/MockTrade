/*
 * Author: Balch
 * Created: 7/29/16 7:16 AM
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.shared.HighlightItem;
import com.balch.mocktrade.shared.PerformanceItem;
import com.balch.mocktrade.shared.WearDataSync;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MockTradeFace extends CanvasWatchFaceService {
    private static final String TAG = MockTradeFace.class.getSimpleName();

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final float TIME_TICK_STROKE_WIDTH = 2f;
    private static final int SHADOW_RADIUS = 6;

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(15);
    private static final long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MockTradeFace.Engine> mWeakReference;

        public EngineHandler(MockTradeFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MockTradeFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        private Paint mBackgroundPaint;
        private Paint mTextPaint;
        private Paint mDayCirclePaint;
        private Paint mCurrentTimeDayPaint;
        private Paint mMarketTimePaint;
        private Paint mMarketDayRingPaint;
        private boolean mAmbient;
        private Calendar mTime;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.setTimeZone(TimeZone.getDefault());
                calcMarketTimes();
                calcPerformanceGradient();
            }
        };

        private Rect mTextSizeRect = new Rect();
        private float mYOffset;
        private int mChinSize;
        private float mOuterWidth;
        private float mInnerWidth;
        private int mOuterColor;
        private int mInnerColor;

        private RectF mMarketArcRect = new RectF();
        private float mMarketOpenDegrees;
        private float mMarketDurationDegrees;

        private List<PerformanceItem> mPerformanceItems;
        private float mPerformanceDurationDegrees;

        private List<HighlightItem> mHighlightItems;
        private int mHighlightItemPosition = 0;
        private Paint mHighlightDescTextPaint;
        private Paint mHighlightSymbolTextPaint;
        private Paint mHighlightDataTextPaint;
        private float mHighlightYOffset;

        private int mColorPositive;
        private int mColorNegative;

        private boolean mLowBitAmbient;

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(MockTradeFace.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MockTradeFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .setStatusBarGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP)
                    .build());
            Resources resources = MockTradeFace.this.getResources();

            mOuterWidth = resources.getDimension(R.dimen.day_circle_outer_width);
            mInnerWidth = resources.getDimension(R.dimen.day_circle_inner_width);

            mOuterColor = ContextCompat.getColor(MockTradeFace.this, R.color.day_circle_outer_color);
            mInnerColor = ContextCompat.getColor(MockTradeFace.this, R.color.day_circle_inner_color);

            mOuterColor = ContextCompat.getColor(MockTradeFace.this, R.color.day_circle_outer_color);
            mInnerColor = ContextCompat.getColor(MockTradeFace.this, R.color.day_circle_inner_color);

            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mHighlightYOffset = resources.getDimension(R.dimen.highlight_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.background));

            mTextPaint = new Paint();
            mTextPaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.digital_text));
            mTextPaint.setTypeface(NORMAL_TYPEFACE);
            mTextPaint.setAntiAlias(true);

            mHighlightDescTextPaint = new Paint();
            mHighlightDescTextPaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.highlight_text_desc_color));
            mHighlightDescTextPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.highlight_desc_size));
            mHighlightDescTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mHighlightDescTextPaint.setAntiAlias(true);

            mHighlightSymbolTextPaint = new Paint();
            mHighlightSymbolTextPaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.highlight_text_symbol_color));
            mHighlightSymbolTextPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.highlight_symbol_size));
            mHighlightSymbolTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mHighlightSymbolTextPaint.setAntiAlias(true);

            mHighlightDataTextPaint = new Paint();
            mHighlightDataTextPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.highlight_data_size));
            mHighlightDataTextPaint.setTypeface(Typeface.DEFAULT);
            mHighlightDataTextPaint.setAntiAlias(true);

            mColorPositive = ContextCompat.getColor(MockTradeFace.this, R.color.data_color_pos);
            mColorNegative = ContextCompat.getColor(MockTradeFace.this, R.color.data_color_neg);

            mDayCirclePaint = new Paint();
            mDayCirclePaint.setStrokeCap(Paint.Cap.ROUND);
            mDayCirclePaint.setStyle(Paint.Style.STROKE);
            mDayCirclePaint.setAntiAlias(true);

            mCurrentTimeDayPaint = new Paint();
            mCurrentTimeDayPaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.day_circle_tick_color));
            mCurrentTimeDayPaint.setStrokeWidth(TIME_TICK_STROKE_WIDTH);
            mCurrentTimeDayPaint.setAntiAlias(true);
            mCurrentTimeDayPaint.setStyle(Paint.Style.STROKE);
            mCurrentTimeDayPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, Color.BLACK);

            mMarketTimePaint = new Paint();
            mMarketTimePaint.setStrokeCap(Paint.Cap.ROUND);
            mMarketTimePaint.setStyle(Paint.Style.STROKE);
            mMarketTimePaint.setAntiAlias(true);
            mMarketTimePaint.setStrokeWidth(mInnerWidth);
            mMarketTimePaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.market_inner_color));

            mMarketDayRingPaint = new Paint();
            mMarketDayRingPaint.setStrokeCap(Paint.Cap.ROUND);
            mMarketDayRingPaint.setStyle(Paint.Style.STROKE);
            mMarketDayRingPaint.setAntiAlias(true);
            mMarketDayRingPaint.setStrokeWidth(mInnerWidth);

            mTime = GregorianCalendar.getInstance();

            calcMarketTimes();

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();

                registerReceiver();

                mTime.setTimeInMillis(System.currentTimeMillis());
            } else {
                unregisterReceiver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }

            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);

            MockTradeFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MockTradeFace.this.unregisterReceiver(mTimeZoneReceiver);
        }


        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            sizeChanged();
        }

        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            mChinSize = insets.getSystemWindowInsetBottom();

            // Load resources that have alternate values for round watches.
            Resources resources = MockTradeFace.this.getResources();
            boolean isRound = insets.isRound();
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);

            sizeChanged();
        }

        private void sizeChanged() {
            Rect frame = getSurfaceHolder().getSurfaceFrame();
            float offset = mChinSize + ((mOuterWidth + (mOuterWidth - mInnerWidth)) / 2.0f);

            mMarketArcRect = new RectF(offset, offset, frame.width()-offset, frame.height()-offset);

            calcPerformanceGradient();
        }

        private void calcMarketTimes() {
            mMarketOpenDegrees = getDegrees(getMarketOpenTime());
            float endDegrees = getDegrees(getMarketCloseTime());

            // handle case where endtime is less than start time
            if (endDegrees < mMarketOpenDegrees) {
                endDegrees += 360;
            }

            mMarketDurationDegrees = endDegrees - mMarketOpenDegrees;
        }

        private void calcPerformanceGradient() {
            Rect frame = getSurfaceHolder().getSurfaceFrame();

            Shader shader = null;
            if ((mPerformanceItems != null) && mPerformanceItems.size() > 0) {
                float extents = (.01f * mPerformanceItems.get(0).getValue().getMicroCents());

                Calendar cal = Calendar.getInstance();

                int size = mPerformanceItems.size();
                int[] colors = new int[size];
                float[] positions = new float[size];
                float lastDegrees = 0;
                for (int x = 0; x < size; x++) {
                    PerformanceItem item = mPerformanceItems.get(x);

                    int color = Color.rgb(156, 156, 156);
                    long todayChange = item.getTodayChange().getMicroCents();
                    if (todayChange != 0) {
                        int colorComponent = (int) Math.min(255, 155 + 100 * (Math.abs(todayChange) / extents));
                        color = (todayChange < 0) ? Color.rgb(colorComponent, 0, 0) : Color.rgb(0, colorComponent, 0);
                    }

                    colors[x] = color;

                    cal.setTimeInMillis(item.getTimestamp().getTime());

                    lastDegrees = getDegrees(cal);
                    positions[x] = lastDegrees / 360.0f;

                }
                shader = new SweepGradient(frame.centerX(), frame.centerY(), colors, positions);

                if (lastDegrees < mMarketOpenDegrees) {
                    lastDegrees += 360;
                }
                mPerformanceDurationDegrees = lastDegrees - mMarketOpenDegrees;
            }
            mMarketDayRingPaint.setShader(shader);
        }


        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                    mDayCirclePaint.setAntiAlias(!inAmbientMode);
                    mCurrentTimeDayPaint.setAntiAlias(!inAmbientMode);
                    mMarketTimePaint.setAntiAlias(!inAmbientMode);
                    mMarketDayRingPaint.setAntiAlias(!inAmbientMode);
                    mHighlightDescTextPaint.setAntiAlias(!inAmbientMode);
                    mHighlightSymbolTextPaint.setAntiAlias(!inAmbientMode);
                    mHighlightDataTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    if (mHighlightItems != null) {
                        mHighlightItemPosition = ++mHighlightItemPosition % mHighlightItems.size();
                        invalidate();
                    }
                    break;
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            int width = bounds.width();
            int height = bounds.height();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, width, height, mBackgroundPaint);
            }

            float centerX = width / 2f;
            float centerY = height / 2f;

            float radius = Math.min(centerX, centerY) - mChinSize - mDayCirclePaint.getStrokeWidth();

            mDayCirclePaint.setStrokeWidth(mOuterWidth);
            mDayCirclePaint.setColor(mOuterColor);
            canvas.drawCircle(centerX, centerY, radius, mDayCirclePaint);

            mDayCirclePaint.setStrokeWidth(mInnerWidth);
            mDayCirclePaint.setColor(mInnerColor);
            canvas.drawCircle(centerX, centerY, radius, mDayCirclePaint);

            mTime.setTimeInMillis(System.currentTimeMillis());

            canvas.save();
            canvas.rotate(-90, centerX, centerY);
            canvas.drawArc(mMarketArcRect, mMarketOpenDegrees, mMarketDurationDegrees,false, mMarketTimePaint);

            if (mPerformanceItems != null) {
                canvas.drawArc(mMarketArcRect, mMarketOpenDegrees, mPerformanceDurationDegrees, false, mMarketDayRingPaint);
            }

            canvas.restore();

            drawTimeTick(mTime, centerX, centerY, canvas, mCurrentTimeDayPaint);

            int hour = mTime.get(Calendar.HOUR);
            if (hour == 0) {
                hour = 12;
            }

            String text =  String.format("%d:%02d", hour, mTime.get(Calendar.MINUTE)) ;

            mTextPaint.getTextBounds(text, 0, text.length(), mTextSizeRect);
            float x = centerX - mTextSizeRect.width() / 2f - mTextSizeRect.left;
            canvas.drawText(text, x, mYOffset, mTextPaint);

            if ((mHighlightItems != null) && (mHighlightItems.size() > 0)) {
                HighlightItem item = mHighlightItems.get(mHighlightItemPosition);

                text = (item.getHighlightType() != HighlightItem.HighlightType.TOTAL_ACCOUNT) ?
                        item.getDescription() : item.getSymbol();
                mHighlightDescTextPaint.getTextBounds(text, 0, text.length(), mTextSizeRect);
                x = centerX - mTextSizeRect.width() / 2f - mTextSizeRect.left;
                canvas.drawText(text, x, mHighlightYOffset, mHighlightDescTextPaint);

                float y = getYOffset(mHighlightYOffset, mTextSizeRect);

                if ((item.getHighlightType() == HighlightItem.HighlightType.TOTAL_OVERALL) ||
                    (item.getHighlightType() == HighlightItem.HighlightType.TOTAL_ACCOUNT)) {
                    drawAccountHighlightItem(item, centerX, y,  canvas);
                } else {
                    drawSymbolHighlightItem(item, centerX, y,  canvas);
                }
            }
        }

        private float getYOffset(float startValue, Rect textSizeRect) {
            return startValue + textSizeRect.height() + 5f;
        }

        private void drawSymbolHighlightItem(HighlightItem item, float centerX, float y,  Canvas canvas) {
            String text = item.getSymbol();

            mHighlightSymbolTextPaint.getTextBounds(text, 0, text.length(), mTextSizeRect);
            float x = centerX - mTextSizeRect.width() / 2f - mTextSizeRect.left;
            canvas.drawText(text, x, y, mHighlightSymbolTextPaint);

            y = getYOffset(y, mTextSizeRect);

            boolean isDayChange = item.isDayChangeType();

            Money value = isDayChange ? item.getTodayChange() : item.getTotalChange();
            String percent = String.format("%.2f", isDayChange ? item.getTodayChangePercent() : item.getTotalChangePercent());
            text = value.getFormatted(isDayChange ? 2 : 0) + " (" + percent + "%)";

            mHighlightDataTextPaint.getTextBounds(text, 0, text.length(), mTextSizeRect);
            x = centerX - mTextSizeRect.width() / 2f - mTextSizeRect.left;
            mHighlightDataTextPaint.setColor((value.getMicroCents() < 0) ? mColorNegative : mColorPositive);
            canvas.drawText(text, x, y, mHighlightDataTextPaint);
        }


        private void drawAccountHighlightItem(HighlightItem item, float centerX, float y,  Canvas canvas) {
            String text = item.getValue().getFormatted(0);
            mHighlightDataTextPaint.getTextBounds(text, 0, text.length(), mTextSizeRect);
            float x = centerX - mTextSizeRect.width() / 2f - mTextSizeRect.left;
            mHighlightDataTextPaint.setColor(Color.WHITE);
            canvas.drawText(text, x, y, mHighlightDataTextPaint);

            y = getYOffset(y, mTextSizeRect);

            Money value = item.getTodayChange();
            int percent = -1;
            if (value.getMicroCents() == 0) {
                value = item.getTotalChange();
                percent = (int) item.getTotalChangePercent();
            }

            text = value.getFormatted(0);
            if (percent != -1) {
                text +=   " (" + percent + "%)";
            }
            mHighlightDataTextPaint.getTextBounds(text, 0, text.length(), mTextSizeRect);
            x = centerX - mTextSizeRect.width() / 2f - mTextSizeRect.left;
            mHighlightDataTextPaint.setColor((value.getMicroCents() < 0) ? mColorNegative : mColorPositive);
            canvas.drawText(text, x, y, mHighlightDataTextPaint);
        }

        private void drawTimeTick(Calendar time, float centerX, float centerY, Canvas canvas, Paint paint) {
            float hours = time.get(Calendar.HOUR_OF_DAY) + time.get(Calendar.MINUTE) / 60.0f + time.get(Calendar.SECOND) / 3600.0f;

            float innerTickRadius = centerX - 10 - mChinSize;
            float outerTickRadius = centerX - 1 - mChinSize;
            float tickRot = (float) (hours * Math.PI * 2 / 24);
            float innerX = (float) Math.sin(tickRot) * innerTickRadius;
            float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
            float outerX = (float) Math.sin(tickRot) * outerTickRadius;
            float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
            canvas.drawLine(centerX + innerX, centerY + innerY,
                    centerX + outerX, centerY + outerY, paint);
        }

        private float getDegrees(Calendar time) {
            float hours = time.get(Calendar.HOUR_OF_DAY) + time.get(Calendar.MINUTE) / 60.0f + time.get(Calendar.SECOND) / 3600.0f;
            return hours / 24.0f * 360;
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private void updateDataItemAndUiOnStartup() {
            PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
            results.setResultCallback(new ResultCallback<DataItemBuffer>() {
                @Override
                public void onResult(DataItemBuffer dataItems) {
                    for  (int x = 0; x < dataItems.getCount(); x++) {
                        updateFromDataMap(dataItems.get(x));
                    }

                    dataItems.release();
                }
            });
        }

        private void updateFromDataMap(DataItem dataItem) {

            DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
            DataMap dataMap = dataMapItem.getDataMap();

            if (dataItem.getUri().getPath().equals(WearDataSync.PATH_SNAPSHOT_SYNC)) {
                ArrayList<DataMap> dataMapList = dataMap.getDataMapArrayList(WearDataSync.DATA_SNAPSHOT_DAILY);

                if (dataMapList != null) {
                    mPerformanceItems = new ArrayList<>(dataMapList.size());

                    // filter out any values that are outside of market open and close times
                    long marketOpen = getMarketOpenTime().getTimeInMillis() % MILLIS_PER_DAY;
                    long marketClose = getMarketCloseTime().getTimeInMillis() % MILLIS_PER_DAY;

                    for (DataMap data : dataMapList) {
                        PerformanceItem item = new PerformanceItem(data);
                        long ts = item.getTimestamp().getTime() % MILLIS_PER_DAY;
                        if ((ts >= marketOpen) && (ts <= marketClose)) {
                            mPerformanceItems.add(item);
                        }
                    }

                    if (mPerformanceItems.size() == 0) {
                        mPerformanceItems = null;
                    }
                } else {
                    mPerformanceItems = null;
                }

                calcPerformanceGradient();
            }

            if (dataItem.getUri().getPath().equals(WearDataSync.PATH_HIGHLIGHTS_SYNC)) {
                ArrayList<DataMap> dataMapList = dataMap.getDataMapArrayList(WearDataSync.DATA_HIGHLIGHTS);

                if (dataMapList != null) {
                    mHighlightItems = new ArrayList<>(dataMapList.size());

                    for (DataMap data : dataMapList) {
                        mHighlightItems.add(new HighlightItem(data));
                    }
                } else {
                    mHighlightItems = null;
                }
                mHighlightItemPosition = 0;
            }
        }

        private Calendar getMarketOpenTime() {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
            cal.set(Calendar.HOUR_OF_DAY, 6);
            cal.set(Calendar.MINUTE, 30);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Calendar cal2 = Calendar.getInstance();
            cal2.setTimeInMillis(cal.getTimeInMillis());

            return cal2;
        }

        private Calendar getMarketCloseTime() {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
            cal.set(Calendar.HOUR_OF_DAY, 13);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Calendar cal2 = Calendar.getInstance();
            cal2.setTimeInMillis(cal.getTimeInMillis());

            return cal2;
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.d(TAG, "onDataChanged: ");

            for (DataEvent dataEvent : dataEvents) {
                if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                    continue;
                }

                DataItem dataItem = dataEvent.getDataItem();
                updateFromDataMap(dataItem);
            }
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected: ");
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            updateDataItemAndUiOnStartup();
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.e(TAG, "onConnectionFailed: " + result);
        }
    }
}

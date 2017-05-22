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

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
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
import android.view.animation.AccelerateDecelerateInterpolator;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.shared.HighlightItem;
import com.balch.mocktrade.shared.PerformanceItem;
import com.balch.mocktrade.shared.WatchConfigItem;
import com.balch.mocktrade.shared.WearDataSync;
import com.balch.mocktrade.shared.utils.TextFormatUtils;
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
import com.google.android.gms.wearable.PutDataMapRequest;
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
    private static final float TIME_TICK_STROKE_WIDTH = 3f;
    private static final float SHADOW_RADIUS = 6;
    private static final int BASE_PERFORMANCE_COLOR_COMPONENT = 156;
    private static final int OFF_PERFORMANCE_COLOR_COMPONENT = 128;
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(15);
    private static final int MSG_UPDATE_TIME = 0;
    private static final int ANIMATION_DURATION_MS = 500;
    private Typeface timeFont;

    @Override
    public Engine onCreateEngine() {
        timeFont = Typeface.createFromAsset(this.getAssets(), "fonts/lovelyweekend.ttf");
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MockTradeFace.Engine> weakReference;

        public EngineHandler(MockTradeFace.Engine reference) {
            weakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MockTradeFace.Engine engine = weakReference.get();
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
        final Handler updateTimeHandler = new EngineHandler(this);
        boolean registeredTimeZoneReceiver = false;
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(MockTradeFace.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        private Paint backgroundPaint;
        private Paint textPaint;
        private Paint dayCirclePaint;
        private Paint currentTimeDayPaint;
        private Paint marketTimePaint;
        private Paint marketDayRingPaint;
        private Calendar calendar;
        private Rect textSizeRect = new Rect();
        private float yOffset;
        private int chinSize;
        private float outerWidth;
        private float innerWidth;
        private int outerColor;
        private int innerColor;

        private RectF marketArcRect = new RectF();
        private float marketOpenDegrees;
        private float marketDurationDegrees;
        private boolean zoomMarketArc; // if two, outer circle is 12HR

        private ArrayList<PerformanceItem> performanceItems;
        private float performanceDurationDegrees;
        private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getDefault());
                calcMarketTimes();
                calcPerformanceGradient();
            }
        };
        private List<HighlightItem> highlightItems;
        private int highlightItemPosition = 0;
        private Paint highlightDescTextPaint;
        private Paint highlightSymbolTextPaint;
        private Paint highlightDataTextPaint;
        private float highlightYOffset;
        private int colorPositive;
        private int colorNegative;
        private boolean lowBitAmbient;
        private boolean showTwentyFourHourTime;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MockTradeFace.this)
                    .setAcceptsTapEvents(true)
                    .setStatusBarGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP)
                    .build());
            Resources resources = MockTradeFace.this.getResources();

            outerWidth = resources.getDimension(R.dimen.day_circle_outer_width);
            innerWidth = resources.getDimension(R.dimen.day_circle_inner_width);

            outerColor = ContextCompat.getColor(MockTradeFace.this, R.color.day_circle_outer_color);
            innerColor = ContextCompat.getColor(MockTradeFace.this, R.color.day_circle_inner_color);

            yOffset = resources.getDimension(R.dimen.digital_y_offset);
            yOffset = resources.getDimension(R.dimen.digital_y_offset);
            highlightYOffset = resources.getDimension(R.dimen.highlight_y_offset);

            backgroundPaint = new Paint();
            backgroundPaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.background));

            textPaint = new Paint();
            textPaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.digital_text));
            textPaint.setTypeface(timeFont);
            textPaint.setAntiAlias(true);

            highlightDescTextPaint = new Paint();
            highlightDescTextPaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.highlight_text_desc_color));
            highlightDescTextPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.highlight_desc_size));
            highlightDescTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            highlightDescTextPaint.setAntiAlias(true);

            highlightSymbolTextPaint = new Paint();
            highlightSymbolTextPaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.highlight_text_symbol_color));
            highlightSymbolTextPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.highlight_symbol_size));
            highlightSymbolTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            highlightSymbolTextPaint.setAntiAlias(true);

            highlightDataTextPaint = new Paint();
            highlightDataTextPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.highlight_data_size));
            highlightDataTextPaint.setTypeface(Typeface.DEFAULT);
            highlightDataTextPaint.setAntiAlias(true);

            colorPositive = ContextCompat.getColor(MockTradeFace.this, R.color.data_color_pos);
            colorNegative = ContextCompat.getColor(MockTradeFace.this, R.color.data_color_neg);

            dayCirclePaint = new Paint();
            dayCirclePaint.setStrokeCap(Paint.Cap.ROUND);
            dayCirclePaint.setStyle(Paint.Style.STROKE);
            dayCirclePaint.setAntiAlias(true);
            dayCirclePaint.setStrokeJoin(Paint.Join.MITER);

            currentTimeDayPaint = new Paint();
            currentTimeDayPaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.day_circle_tick_color));
            currentTimeDayPaint.setStrokeWidth(TIME_TICK_STROKE_WIDTH);
            currentTimeDayPaint.setAntiAlias(true);
            currentTimeDayPaint.setStyle(Paint.Style.STROKE);
            currentTimeDayPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, Color.BLACK);
            currentTimeDayPaint.setStrokeJoin(Paint.Join.BEVEL);

            marketTimePaint = new Paint();
            marketTimePaint.setStrokeCap(Paint.Cap.ROUND);
            marketTimePaint.setStyle(Paint.Style.STROKE);
            marketTimePaint.setAntiAlias(true);
            marketTimePaint.setStrokeWidth(innerWidth);
            marketTimePaint.setColor(ContextCompat.getColor(MockTradeFace.this, R.color.market_inner_color));
            marketTimePaint.setStrokeJoin(Paint.Join.BEVEL);

            marketDayRingPaint = new Paint();
            marketDayRingPaint.setStrokeCap(Paint.Cap.ROUND);
            marketDayRingPaint.setStyle(Paint.Style.STROKE);
            marketDayRingPaint.setAntiAlias(true);
            marketDayRingPaint.setStrokeWidth(outerWidth);
            marketDayRingPaint.setStrokeJoin(Paint.Join.BEVEL);

            calendar = GregorianCalendar.getInstance();

            calcMarketTimes();

        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);

            if (googleApiClient != null && googleApiClient.isConnected()) {
                Wearable.DataApi.removeListener(googleApiClient, this);
                googleApiClient.disconnect();
            }

            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                googleApiClient.connect();

                registerReceiver();

                calendar.setTimeInMillis(System.currentTimeMillis());
            } else {
                unregisterReceiver();

                if (googleApiClient != null && googleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(googleApiClient, this);
                    googleApiClient.disconnect();
                }

            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);

            MockTradeFace.this.registerReceiver(timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = false;
            MockTradeFace.this.unregisterReceiver(timeZoneReceiver);
        }


        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            sizeChanged();
        }

        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            chinSize = insets.getSystemWindowInsetBottom();

            // Load resources that have alternate values for round watches.
            Resources resources = MockTradeFace.this.getResources();
            boolean isRound = insets.isRound();
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            textPaint.setTextSize(textSize);

            sizeChanged();
        }

        private void sizeChanged() {
            Rect frame = getSurfaceHolder().getSurfaceFrame();
            float offset = chinSize + ((outerWidth + (outerWidth - innerWidth)) / 2.0f);

            marketArcRect = new RectF(offset, offset, frame.width()-offset, frame.height()-offset);

            calcPerformanceGradient();
        }

        private void calcMarketTimes() {
            marketOpenDegrees = getDegrees(getMarketOpenTime());
            float endDegrees = getDegrees(getMarketCloseTime());

            // handle case where endtime is less than start time
            if (endDegrees < marketOpenDegrees) {
                endDegrees += 360;
            }

            marketDurationDegrees = endDegrees - marketOpenDegrees;
        }

        private void calcPerformanceGradient() {
            Rect frame = getSurfaceHolder().getSurfaceFrame();

            Shader shader = null;
            if ((performanceItems != null) && performanceItems.size() > 0) {
                float extent = getPerformanceExtent(performanceItems.get(0).getValue().getMicroCents());

                Calendar cal = Calendar.getInstance();

                int size = performanceItems.size();
                int[] colors = new int[size];
                float[] positions = new float[size];

                cal.setTimeInMillis(performanceItems.get(0).getTimestamp().getTime());
                float startDegrees = getDegrees(cal);

                for (int x = 0; x < size; x++) {
                    PerformanceItem item = performanceItems.get(x);

                    long todayChange = item.getTodayChange().getMicroCents();
//                    todayChange = (long)(-extents + (extents * 2*x/size));

                    colors[x] = getPerformanceColor(todayChange, extent);

                    cal.setTimeInMillis(item.getTimestamp().getTime());
                    float calDegrees = getDegrees(cal);
                    if (calDegrees < startDegrees) {
                        calDegrees += 360;
                    }

                    positions[x] = (calDegrees - startDegrees) / 360.0f;
                }

                shader = new SweepGradient(frame.centerX(), frame.centerY(), colors, positions);

                cal.setTimeInMillis(performanceItems.get(size-1).getTimestamp().getTime());

                float calDegrees = getDegrees(cal);
                if (calDegrees < marketOpenDegrees) {
                    calDegrees += 360;
                }

                performanceDurationDegrees = calDegrees - marketOpenDegrees;
            }
            marketDayRingPaint.setShader(shader);
        }

        private float getPerformanceExtent(long value) {
            return .01f * value / 2.0f;
        }

        private int getPerformanceColor(long value, float extent) {
            int color = Color.rgb(156, 156, 156);
            if (value != 0) {
                float colorPercent = (Math.abs(value) / extent);
                int colorComponent = (int) Math.min(255, BASE_PERFORMANCE_COLOR_COMPONENT + (255 - BASE_PERFORMANCE_COLOR_COMPONENT) * colorPercent);
                int secondaryColorComponent = (int) Math.max(0, OFF_PERFORMANCE_COLOR_COMPONENT - (255 - OFF_PERFORMANCE_COLOR_COMPONENT) * colorPercent * 1.5f);
                color = (value < 0) ? Color.rgb(colorComponent, secondaryColorComponent, secondaryColorComponent) : Color.rgb(secondaryColorComponent, colorComponent, secondaryColorComponent);
            }

            return color;
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            lowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (lowBitAmbient) {
                textPaint.setAntiAlias(!inAmbientMode);
                dayCirclePaint.setAntiAlias(!inAmbientMode);
                currentTimeDayPaint.setAntiAlias(!inAmbientMode);
                marketTimePaint.setAntiAlias(!inAmbientMode);
                marketDayRingPaint.setAntiAlias(!inAmbientMode);
                highlightDescTextPaint.setAntiAlias(!inAmbientMode);
                highlightSymbolTextPaint.setAntiAlias(!inAmbientMode);
                highlightDataTextPaint.setAntiAlias(!inAmbientMode);
            }

            if (inAmbientMode) {
                textPaint.setColorFilter(null);
            } else {
                if ((highlightItems != null) && !highlightItems.isEmpty()) {
                    HighlightItem item = highlightItems.get(highlightItemPosition);
                    setTimeTextPaint(item, true);
                }
            }

            invalidate();
            updateTimer();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    break;
                case TAP_TYPE_TAP:
                    if (timeTickHitTest(x, y)) {
                        zoomMarketArc = !zoomMarketArc;
                        calcMarketTimes();
                        calcPerformanceGradient();
                    } else if (marketTimeHitTest(x, y, true)) {
                        if (highlightItems != null) {
                            HighlightItem item = highlightItems.get(highlightItemPosition);
                            if (item.isTotalType()) {
                                startActivity(GraphActivity.newIntent(getApplicationContext(), item, performanceItems));
                            }
                        }
                    } else if (!marketTimeHitTest(x, y, false)) {  // did not click in time ring
                        if (highlightItems != null) {
                            highlightItemPosition = ++highlightItemPosition % highlightItems.size();
                            HighlightItem item = highlightItems.get(highlightItemPosition);
                            setTimeTextPaint(item, false);
                            setAccountIdDataItem(item);
                        }
                    }
                    invalidate();
                    break;
            }
        }

        private boolean marketTimeHitTest(int x, int y, boolean inPerformanceArea) {
            float circleRadius = marketArcRect.height() /2.0f;

            float centerX = marketArcRect.centerX();
            float centerY = marketArcRect.centerY();

            boolean isHit = false;

            //calculate the distance of the touch point from the center of your circle
            double dist = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));

            if (Math.abs(dist - circleRadius) <= outerWidth *2) {
                double angle = (Math.atan2(y - centerY, x - centerX) * 57.2958) + 90.0f;
                if (angle < 0 ){
                    angle += 360.0;
                } else if (angle >= 360.0) {
                    angle -= 360.0;
                }

                isHit = ((angle >= marketOpenDegrees) && (angle <= marketOpenDegrees + marketDurationDegrees));
                if (!inPerformanceArea) {
                    isHit = !isHit;
                }
            }

            return isHit;
        }

        private boolean timeTickHitTest(int x, int y) {
            float circleRadius = marketArcRect.height() /2.0f;

            float centerX = marketArcRect.centerX();
            float centerY = marketArcRect.centerY();

            boolean isHit = false;

            //calculate the distance of the touch point from the center of your circle
            double dist = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));

            if (Math.abs(dist - circleRadius) <= outerWidth *2) {
                double angle = (Math.atan2(y - centerY, x - centerX) * 57.2958) + 90.0f;
                if (angle < 0 ){
                    angle += 360.0;
                } else if (angle >= 360.0) {
                    angle -= 360.0;
                }


                float nowTick = getDegrees(calendar);
                isHit = ((angle >= nowTick-5) && (angle <= nowTick+5));
            }

            return isHit;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            int width = bounds.width();
            int height = bounds.height();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, width, height, backgroundPaint);
            }

            float centerX = width / 2f;
            float centerY = height / 2f;

            float radius = Math.min(centerX, centerY) - chinSize - dayCirclePaint.getStrokeWidth();

            dayCirclePaint.setStrokeWidth(outerWidth);
            dayCirclePaint.setColor(outerColor);
            canvas.drawCircle(centerX, centerY, radius, dayCirclePaint);

            dayCirclePaint.setStrokeWidth(innerWidth);
            dayCirclePaint.setColor(innerColor);
            canvas.drawCircle(centerX, centerY, radius, dayCirclePaint);

            calendar.setTimeInMillis(System.currentTimeMillis());

            canvas.save();
            canvas.rotate(marketOpenDegrees - 90, centerX, centerY);

            // only draw default arc if the performance arc is not complete
            if (marketDurationDegrees > performanceDurationDegrees) {
                canvas.drawArc(marketArcRect, 0, marketDurationDegrees, false, marketTimePaint);
            }

            // draw performance arc
            if (performanceItems != null) {
                canvas.drawArc(marketArcRect, 0, performanceDurationDegrees, false, marketDayRingPaint);
            }

            canvas.restore();

            drawTimeTick(calendar, centerX, centerY, canvas, currentTimeDayPaint);

            int hour = calendar.get(showTwentyFourHourTime ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
            if (!showTwentyFourHourTime && (hour == 0)) {
                hour = 12;
            }

            String formatText = showTwentyFourHourTime ? "%02d:%02d" : "%d:%02d";
            String text =  String.format(formatText, hour, calendar.get(Calendar.MINUTE)) ;

            textPaint.getTextBounds(text, 0, text.length(), textSizeRect);
            float x = centerX - textSizeRect.width() / 2f - textSizeRect.left;
            canvas.drawText(text, x, yOffset, textPaint);

            if ((highlightItems != null) && !highlightItems.isEmpty()) {
                HighlightItem item = highlightItems.get(highlightItemPosition);

                text = (item.getHighlightType() != HighlightItem.HighlightType.TOTAL_ACCOUNT) ?
                        item.getDescription() : item.getSymbol();
                highlightDescTextPaint.getTextBounds(text, 0, text.length(), textSizeRect);
                x = centerX - textSizeRect.width() / 2f - textSizeRect.left;
                canvas.drawText(text, x, highlightYOffset, highlightDescTextPaint);

                float y = getYOffset(highlightYOffset, textSizeRect);

                if (item.isTotalType()) {
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

            highlightSymbolTextPaint.getTextBounds(text, 0, text.length(), textSizeRect);
            float x = centerX - textSizeRect.width() / 2f - textSizeRect.left;
            canvas.drawText(text, x, y, highlightSymbolTextPaint);

            y = getYOffset(y, textSizeRect);

            boolean isDayChange = item.isDayChangeType();

            Money value = isDayChange ? item.getTodayChange() : item.getTotalChange();
            String percent = TextFormatUtils.getPercentString(isDayChange ? item.getTodayChangePercent() : item.getTotalChangePercent());
            text = TextFormatUtils.getDollarString(value.getDollars()) + " (" + percent + ")";

            highlightDataTextPaint.getTextBounds(text, 0, text.length(), textSizeRect);
            x = centerX - textSizeRect.width() / 2f - textSizeRect.left;
            highlightDataTextPaint.setColor((value.getMicroCents() < 0) ? colorNegative : colorPositive);
            canvas.drawText(text, x, y, highlightDataTextPaint);
        }


        private void drawAccountHighlightItem(HighlightItem item, float centerX, float y,  Canvas canvas) {
            String text = item.getValue().getFormatted(0);
            highlightDataTextPaint.getTextBounds(text, 0, text.length(), textSizeRect);
            float x = centerX - textSizeRect.width() / 2f - textSizeRect.left;
            highlightDataTextPaint.setColor(Color.WHITE);
            canvas.drawText(text, x, y, highlightDataTextPaint);

            y = getYOffset(y, textSizeRect);

            Money value = item.getTodayChange();
            int percent = -1;
            if (value.getMicroCents() == 0) {
                value = item.getTotalChange();
                percent = (int) item.getTotalChangePercent();
            }

            text = TextFormatUtils.getDollarString(value.getDollars());
            if (percent != -1) {
                text +=  " (" + TextFormatUtils.getPercentString(percent) + ")";
            }
            highlightDataTextPaint.getTextBounds(text, 0, text.length(), textSizeRect);
            x = centerX - textSizeRect.width() / 2f - textSizeRect.left;
            highlightDataTextPaint.setColor((value.getMicroCents() < 0) ? colorNegative : colorPositive);
            canvas.drawText(text, x, y, highlightDataTextPaint);
        }

        private void drawTimeTick(Calendar time, float centerX, float centerY, Canvas canvas, Paint paint) {
            float hours = getHours(time);

            float innerTickRadius = centerX - 13 - chinSize;
            float outerTickRadius = centerX - 2 - chinSize;
            float tickRot = (float) (hours * Math.PI * 2 / getRingIncrements());
            float innerX = (float) Math.sin(tickRot) * innerTickRadius;
            float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
            float outerX = (float) Math.sin(tickRot) * outerTickRadius;
            float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
            canvas.drawLine(centerX + innerX, centerY + innerY,
                    centerX + outerX, centerY + outerY, paint);
        }

        private float getHours(Calendar time) {
            return time.get(Calendar.HOUR_OF_DAY) + time.get(Calendar.MINUTE) / 60.0f + time.get(Calendar.SECOND) / 3600.0f;
        }

        private int getRingIncrements() {
            return zoomMarketArc ? 12 : 24;
        }

        private float getDegrees(Calendar time) {
            float hours = time.get(Calendar.HOUR_OF_DAY) + time.get(Calendar.MINUTE) / 60.0f + time.get(Calendar.SECOND) / 3600.0f;
            return (hours / getRingIncrements() * 360.0f) % 360;
        }

        /**
         * Starts the {@link #updateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #updateTimeHandler} timer should be running. The timer should
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
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private void updateDataItemAndUiOnStartup() {
            PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(googleApiClient);
            results.setResultCallback(new ResultCallback<DataItemBuffer>() {
                @Override
                public void onResult(DataItemBuffer dataItems) {
                    for  (int x = 0; x < dataItems.getCount(); x++) {
                        updateFromDataMap(dataItems.get(x));
                    }

                    if ((highlightItems != null) && !highlightItems.isEmpty()) {
                        setTimeTextPaint(highlightItems.get(highlightItemPosition), false);
                    }
                    dataItems.release();
                    invalidate();
                }
            });
        }

        private void updateFromDataMap(DataItem dataItem) {

            DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
            DataMap dataMap = dataMapItem.getDataMap();

            String uriPath = dataItem.getUri().getPath();
            if (uriPath.equals(WearDataSync.PATH_SNAPSHOT_SYNC)) {
                updatePathSnapshotSync(dataMap);
            } else if (uriPath.equals(WearDataSync.PATH_HIGHLIGHTS_SYNC)) {
                updatePathHighlightsSync(dataMap);
            } else if (uriPath.equals(WearDataSync.PATH_WATCH_CONFIG_SYNC)) {
                updatePathWatchConfigSync(dataMap);
            }
        }

        private void updatePathWatchConfigSync(DataMap dataMap) {
            ArrayList<DataMap> dataMapList = dataMap.getDataMapArrayList(WearDataSync.DATA_WATCH_CONFIG_DATA_ITEMS);
            if (dataMapList != null) {
                for (DataMap dm : dataMapList) {
                    WatchConfigItem item = new WatchConfigItem(dm);
                    if (item.getKey().equals("pref_twenty_four_hour_display")) {
                        showTwentyFourHourTime = item.isEnabled();
                    }
                }
            }
        }

        private void updatePathHighlightsSync(DataMap dataMap) {
            ArrayList<DataMap> dataMapList = dataMap.getDataMapArrayList(WearDataSync.DATA_HIGHLIGHTS);

            if (dataMapList != null) {
                highlightItems = new ArrayList<>(dataMapList.size());

                for (DataMap data : dataMapList) {
                    HighlightItem item = new HighlightItem(data);
                    highlightItems.add(item);

                    if (item.getHighlightType() == HighlightItem.HighlightType.TOTAL_ACCOUNT) {
                        setTimeTextPaint(item, false);
                    }
                }

                if (highlightItemPosition >= highlightItems.size()) {
                    highlightItemPosition = 0;
                }
            } else {
                highlightItems = null;
                highlightItemPosition = 0;
            }
        }

        private void updatePathSnapshotSync(DataMap dataMap) {
            ArrayList<DataMap> dataMapList = dataMap.getDataMapArrayList(WearDataSync.DATA_SNAPSHOT_DAILY);

            if (dataMapList != null) {
                performanceItems = new ArrayList<>(dataMapList.size());

                for (DataMap data : dataMapList) {
                    PerformanceItem item = new PerformanceItem(data);
                    performanceItems.add(item);
                }

                if (performanceItems.size() == 0) {
                    performanceItems = null;
                }
            } else {
                performanceItems = null;
            }

            calcPerformanceGradient();
        }

        private void setTimeTextPaint(HighlightItem item, boolean animate) {

            if (item.isTotalType()) {
                final float extent = getPerformanceExtent(item.getValue().getMicroCents());
                final long value = (item.getTodayChange().getMicroCents() != 0) ?
                        item.getTodayChange().getMicroCents() :
                        item.getTotalChange().getMicroCents();

                if (animate) {
                    ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
                    animator.setDuration(ANIMATION_DURATION_MS);
                    animator.setInterpolator(new AccelerateDecelerateInterpolator());
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float animationPercent = (float) animation.getAnimatedValue();
                            setPerformancePaint((long) (value * animationPercent), extent);
                        }
                    });
                    animator.start();
                } else {
                    setPerformancePaint(value, extent);
                }
            } else {
                textPaint.setColorFilter(null);
            }
        }

        private void setPerformancePaint(long value, float extent) {
            int color = getPerformanceColor(value , extent);
            textPaint.setColorFilter(new LightingColorFilter(color, Color.argb(28, 128, 126, 128)));
            invalidate();
        }

        private void setAccountIdDataItem(HighlightItem item) {
            long accountId = -1;
            if (item.getHighlightType() == HighlightItem.HighlightType.TOTAL_ACCOUNT) {
                accountId = item.getAccountId();
            }

            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearDataSync.PATH_WATCH_FACE_ACCOUNT_ID);
            putDataMapRequest.getDataMap().putLong(WearDataSync.DATA_WATCH_FACE_ACCOUNT_ID, accountId);
            putDataMapRequest.setUrgent();
            Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest());
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
            invalidate();
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected: ");
            Wearable.DataApi.addListener(googleApiClient, Engine.this);
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

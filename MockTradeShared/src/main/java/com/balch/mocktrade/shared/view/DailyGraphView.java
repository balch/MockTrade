/*
 * Author: Balch
 * Created: 8/21/16 7:27 AM
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

package com.balch.mocktrade.shared.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.shared.PerformanceItem;
import com.balch.mocktrade.shared.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Render the performance items on a daily graph.
 *
 * This component will graph absolute daily change over time. The
 * vertical center of the graph will be 0.0 and lines drawn above the
 * center will be green and below the center will render as red.
 *
 * The x Axis will represent the entire day, starting 30 mins before
 * market open and ending 30 mins after market close (unless the last
 * quote time is after market close, in which case the the end of the graph
 * will be after the last quote time.
 *
 */
public class DailyGraphView extends View {
    private static final String TAG = DailyGraphView.class.getSimpleName();

    private static final int ANIMATION_DURATION_MS = 700;

    private static final int GRAPH_PADDING_VERTICAL = 30;

    private static final int[] LINEAR_GRADIENT_COLORS_STROKE = new int[]{
            Color.argb(255, 0, 255, 0),
            Color.argb(255, 0, 156, 0),
            Color.argb(255, 156, 156, 156),
            Color.argb(255, 156, 0, 0),
            Color.argb(255, 255, 0, 0)
    };

    private static final float[] LINEAR_GRADIENT_POSITIONS_STROKE = new float[]{
            0f, .475f, .5f, .525f, 1f
    };

    private static final int[] LINEAR_GRADIENT_COLORS_FILL = new int[]{
            Color.argb(128, 0, 128, 0),
            Color.argb(64, 0, 128, 0),
            Color.argb(48, 0, 128, 0),
            Color.argb(32, 0, 92, 0),
            Color.argb(0, 0, 0, 0),
            Color.argb(32, 92, 0, 0),
            Color.argb(48, 128, 0, 0),
            Color.argb(64, 128, 0, 0),
            Color.argb(128, 128, 0, 0)
    };

    private static final float[] LINEAR_GRADIENT_POSITIONS_FILL = new float[]{
            0f, .1f, .20f, .499f, .5f, .501f, .80f, .9f, 1f
    };

    private final static DateFormat HOURLY_DATE_FORMAT = DateFormat.getTimeInstance(DateFormat.SHORT);
    private final static DateFormat DAILY_DATE_FORMAT = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());

    private static final int EXAMINER_WIDTH = 5;

    private Paint mPathPaintStroke;
    private Paint mPathPaintFill;
    private Paint mMarketTimesPaint;
    private Path mPathStroke;
    private Path mPathFill;
    private float mPathLengthStroke;
    private Paint mExaminerPaint;
    private RectF mExaminerRect;
    private Paint mExaminerTimePaint;
    private String mExaminerTime;
    private Rect mExaminerTimeTextBounds = new Rect();
    private Paint mExaminerValuePaint;
    private String mExaminerValue;
    private Rect mExaminerValueTextBounds = new Rect();

    private List<PerformanceItem> mPerformanceItems;

    private int mWidth;
    private int mHeight;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private long mOffsetY;
    private long mOffsetX;

    private long mMaxYValue;
    private long mMinYValue;
    private long mMarketStartTime;
    private long mMarketEndTime;
    private boolean mAllowMove = true;

    private boolean mHourly = true;

    public DailyGraphView(Context context) {
        super(context);
        initialize(null);
    }

    public DailyGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mHourly) {
            if (mMarketStartTime != 0) {
                float marketStartX = scaleX(mMarketStartTime);
                canvas.drawLine(marketStartX, GRAPH_PADDING_VERTICAL,
                        marketStartX, mHeight - GRAPH_PADDING_VERTICAL, mMarketTimesPaint);
            }

            if (mMarketEndTime != 0) {
                float marketEndX = scaleX(mMarketEndTime);
                canvas.drawLine(marketEndX, GRAPH_PADDING_VERTICAL,
                        marketEndX, mHeight - GRAPH_PADDING_VERTICAL, mMarketTimesPaint);
            }
        }

        float centerY = scaleY(getCenterValue());
        canvas.drawLine(0, centerY, mWidth, centerY, mMarketTimesPaint);

        canvas.drawPath(mPathStroke, mPathPaintStroke);
        canvas.drawPath(mPathFill, mPathPaintFill);

        if (mExaminerRect != null) {
            canvas.drawRect(mExaminerRect, mExaminerPaint);

            canvas.drawText(mExaminerTime,
                    mExaminerRect.left - mExaminerTimeTextBounds.centerX(), mHeight - 2,
                    mExaminerTimePaint);

            canvas.drawText(mExaminerValue,
                    mExaminerRect.left - mExaminerValueTextBounds.centerX(),
                    mExaminerValueTextBounds.height() + 2,
                    mExaminerValuePaint);
        }
    }

    private long getCenterValue() {
        long centerPos = 0;
        if (!mHourly && (mPerformanceItems.size() > 0)) {
            centerPos = getPerformanceItemValue(mPerformanceItems.get(0)).getMicroCents();
        }
        return centerPos;
    }

    private void initialize(AttributeSet attrs) {

        int examineTextSize = 34;
        mAllowMove = true;
        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.DailyGraphView,
                    0, 0);

            try {
                examineTextSize = a.getDimensionPixelSize(R.styleable.DailyGraphView_examineTextSize, 34);
                mAllowMove = a.getBoolean(R.styleable.DailyGraphView_allowMove, true);
            } finally {
                a.recycle();
            }
        }

        mMarketTimesPaint = new Paint();
        mMarketTimesPaint.setAntiAlias(true);
        mMarketTimesPaint.setStyle(Paint.Style.STROKE);
        mMarketTimesPaint.setColor(Color.LTGRAY);
        mMarketTimesPaint.setAlpha(128);
        mMarketTimesPaint.setStrokeWidth(2);
        mMarketTimesPaint.setPathEffect(new DashPathEffect(new float[]{4, 4}, 0));

        mExaminerPaint = new Paint();
        mExaminerPaint.setAntiAlias(true);
        mExaminerPaint.setColor(Color.argb(255, 168, 168, 168));
        mExaminerPaint.setStyle(Paint.Style.FILL);
        mExaminerPaint.setMaskFilter(new BlurMaskFilter(EXAMINER_WIDTH, BlurMaskFilter.Blur.NORMAL));
        mExaminerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));

        mExaminerTimePaint = new Paint();
        mExaminerTimePaint.setAntiAlias(true);
        mExaminerTimePaint.setColor(Color.WHITE);
        mExaminerTimePaint.setStyle(Paint.Style.FILL);
        mExaminerTimePaint.setTextSize(examineTextSize);

        mExaminerValuePaint = new Paint();
        mExaminerValuePaint.setAntiAlias(true);
        mExaminerValuePaint.setColor(Color.WHITE);
        mExaminerValuePaint.setStyle(Paint.Style.FILL);
        mExaminerValuePaint.setTextSize(examineTextSize);

        mPathPaintStroke = new Paint();
        mPathPaintStroke.setAntiAlias(true);
        mPathPaintStroke.setStyle(Paint.Style.STROKE);
        mPathPaintStroke.setColor(Color.WHITE);
        mPathPaintStroke.setStrokeWidth(4);
        mPathPaintStroke.setStrokeCap(Paint.Cap.ROUND);
        mPathPaintStroke.setStrokeJoin(Paint.Join.ROUND);
        mPathPaintStroke.setShadowLayer(7, 0f, 0f, Color.LTGRAY);

        mPathPaintFill = new Paint();
        mPathPaintFill.setAntiAlias(true);
        mPathPaintFill.setStyle(Paint.Style.FILL);
        mPathPaintFill.setColor(Color.WHITE);

        if (!isInEditMode() && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        mPathStroke = new Path();
        mPathFill = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if ((w != 0) && (h != 0)) {
            mWidth = w;
            mHeight = h;

            calculatePath();
        }
    }

    private void calculatePath() {
        mPathStroke.rewind();
        mPathFill.rewind();

        if ((mPerformanceItems != null) && (mPerformanceItems.size() >= 2)) {
            calculateScale();

            float centerY = scaleY(getCenterValue());

            int startIndex = mHourly ? 0 : 1;

            PerformanceItem performanceItem = mPerformanceItems.get(startIndex);

            float xScaleValue = scaleX(mHourly ? performanceItem.getTimestamp().getTime() : 0);
            float yScaleValue = scaleY(getPerformanceItemValue(performanceItem).getMicroCents());

            mPathStroke.moveTo(xScaleValue, yScaleValue);

            mPathFill.moveTo(xScaleValue, centerY);
            mPathFill.lineTo(xScaleValue, yScaleValue);

            for (int x = startIndex + 1; x < mPerformanceItems.size() - 1; x++) {
                PerformanceItem nextPerformanceItem = mPerformanceItems.get(x+1);
                float xScaleValueNext = scaleX(mHourly ? nextPerformanceItem.getTimestamp().getTime() : x);
                float yScaleValueNext = scaleY(getPerformanceItemValue(nextPerformanceItem).getMicroCents());

                if (mHourly) {
                    mPathStroke.quadTo(xScaleValue, yScaleValue, xScaleValueNext, yScaleValueNext);
                    mPathFill.quadTo(xScaleValue, yScaleValue, xScaleValueNext, yScaleValueNext);
                } else {
                    mPathStroke.lineTo(xScaleValueNext, yScaleValue);
                    mPathStroke.lineTo(xScaleValueNext, yScaleValueNext);

                    mPathFill.lineTo(xScaleValueNext, yScaleValue);
                    mPathFill.lineTo(xScaleValueNext, yScaleValueNext);
                }

                xScaleValue = xScaleValueNext;
                yScaleValue = yScaleValueNext;
            }

            int lastPos = mPerformanceItems.size() - 1;
            performanceItem = mPerformanceItems.get(lastPos);

            xScaleValue = scaleX(mHourly ? performanceItem.getTimestamp().getTime() : lastPos);
            yScaleValue = scaleY(getPerformanceItemValue(performanceItem).getMicroCents());

            mPathStroke.lineTo(xScaleValue, yScaleValue);

            mPathFill.lineTo(xScaleValue, yScaleValue);
            mPathFill.lineTo(xScaleValue, centerY);

            PathMeasure measure = new PathMeasure(mPathStroke, false);
            mPathLengthStroke = measure.getLength();

            Shader shader = new LinearGradient(0, GRAPH_PADDING_VERTICAL,
                    0, mHeight - GRAPH_PADDING_VERTICAL,
                    LINEAR_GRADIENT_COLORS_STROKE,
                    LINEAR_GRADIENT_POSITIONS_STROKE,
                    Shader.TileMode.CLAMP);

            mPathPaintStroke.setShader(shader);

            shader = new LinearGradient(0, GRAPH_PADDING_VERTICAL,
                    0, mHeight - GRAPH_PADDING_VERTICAL,
                    LINEAR_GRADIENT_COLORS_FILL,
                    LINEAR_GRADIENT_POSITIONS_FILL,
                    Shader.TileMode.CLAMP);
            mPathPaintFill.setShader(shader);
        }
    }

    private void calculateScale() {

        if ((mWidth != 0) && (mHeight != 0)) {

            long initialValue = mPerformanceItems.get(0).getValue().getMicroCents();
            long centerValue = getCenterValue();

            // set the Y range with room to accommodate the max gain or loss
            long absMaxY = Math.abs(mMaxYValue - centerValue);
            long absMinY = Math.abs(mMinYValue - centerValue);
            long deltaY = (absMaxY > absMinY) ? 2 * absMaxY : 2 * absMinY;

            // set the min scale to 1% of current value.
            long minDeltaY = (long) (.01f * initialValue);
            if (deltaY < minDeltaY) {
                deltaY = minDeltaY;
            }

            mScaleY = (float) (mHeight) / deltaY;
            mOffsetY = mHourly ? 0 : initialValue;

            if (mHourly) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));

                // get market start time on same day of first x
                long firstX = mPerformanceItems.get(mHourly ? 0 : 1).getTimestamp().getTime();
                cal.setTimeInMillis(firstX);
                cal.set(Calendar.HOUR_OF_DAY, 6);
                cal.set(Calendar.MINUTE, 30);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                mMarketStartTime = cal.getTimeInMillis();

                // set the start to 30 mins b4 market close
                cal.set(Calendar.MINUTE, 0);
                long startScaleX = cal.getTimeInMillis();

                // get the market end tme
                cal.set(Calendar.HOUR_OF_DAY, 13);
                cal.set(Calendar.MINUTE, 0);
                mMarketEndTime = cal.getTimeInMillis();

                // set the end scale to 30 mins after market close
                cal.set(Calendar.MINUTE, 30);
                long endScaleX = cal.getTimeInMillis();

                // see if there is a sample after the market close and extend the end if so
                long lastX = mPerformanceItems.get(mPerformanceItems.size() - 1).getTimestamp().getTime();
                if (endScaleX < lastX) {
                    endScaleX = lastX;
                }

                mScaleX = mWidth / (float) (endScaleX - startScaleX);
                mOffsetX = startScaleX;
            } else {
                mScaleX = mWidth / (float) (mPerformanceItems.size() - 1);
                mOffsetX = 0;
            }
        }
    }

    public void animateGraph() {
        ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        va.setDuration(ANIMATION_DURATION_MS);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Float percentage = (Float) animation.getAnimatedValue();

                // change the path effect to determine how much of path to render
                float visibleLength = mPathLengthStroke * percentage;
                mPathPaintStroke.setPathEffect(new DashPathEffect(new float[]{visibleLength, mPathLengthStroke - visibleLength}, 0));

                invalidate();
            }
        });
        va.start();
    }

    public void bind(List<PerformanceItem> performanceItems, boolean hourly) {
        mPerformanceItems = performanceItems;
        mHourly = hourly;

        mMaxYValue = Long.MIN_VALUE;
        mMinYValue = Long.MAX_VALUE;

        for (int idx = 0; idx < mPerformanceItems.size(); idx++) {
            PerformanceItem performanceItem = mPerformanceItems.get(idx);
            long y = getPerformanceItemValue(performanceItem).getMicroCents();
            if (y > mMaxYValue) {
                mMaxYValue = y;
            }

            if (y < mMinYValue) {
                mMinYValue = y;
            }
        }

        calculatePath();
        animateGraph();
    }

    private float scaleX(float x) {
        return ((x - mOffsetX) * mScaleX);
    }

    private float scaleY(float y) {
        return mHeight / 2.0f - ((y - mOffsetY) * mScaleY);
    }

    /**
     * This function will be used to abstract out which value to graph
     */
    private Money getPerformanceItemValue(PerformanceItem performanceItem) {
        Money money;
        if (mHourly) {
            money = performanceItem.getTodayChange();
        } else {
            money = performanceItem.getValue();
        }

        return money;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;

        float eventX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (!mAllowMove) {
                    break;
                } // else fallthrow

            case MotionEvent.ACTION_DOWN: {

                long val = (long) (eventX / mScaleX) + mOffsetX;
                Date timestamp = mHourly ? new Date(val) : mPerformanceItems.get((int)val).getTimestamp();

                if (mExaminerRect == null) {
                    mExaminerRect = new RectF();
                }

                mExaminerRect.set(eventX, GRAPH_PADDING_VERTICAL,
                        eventX + EXAMINER_WIDTH, mHeight - GRAPH_PADDING_VERTICAL);

                mExaminerTime = mHourly ?
                        HOURLY_DATE_FORMAT.format(timestamp) :
                        DAILY_DATE_FORMAT.format(timestamp);
                mExaminerTimePaint.getTextBounds(mExaminerTime, 0, mExaminerTime.length(), mExaminerTimeTextBounds);

                mExaminerValue = "";
                Money extrapolated = extrapolateValue(timestamp.getTime());
                if (extrapolated != null) {
                    mExaminerValue = extrapolated.getFormatted();
                    mExaminerValuePaint.setColor((extrapolated.getMicroCents() >= 0) ?
                            Color.GREEN : Color.RED);
                }
                mExaminerValuePaint.getTextBounds(mExaminerValue, 0,
                        mExaminerValue.length(), mExaminerValueTextBounds);

                handled = true;
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mExaminerRect = null;
                handled = true;
                break;
        }

        if (handled) {
            invalidate();
        }

        return handled;
    }

    private Money extrapolateValue(long timestamp) {
        Money money = null;

        if (mPerformanceItems.size() > 2) {
            for (int x = 0; x < mPerformanceItems.size(); x++) {
                PerformanceItem performanceItem = mPerformanceItems.get(x);

                if (timestamp <= performanceItem.getTimestamp().getTime()) {
                    if (!mHourly || (timestamp == performanceItem.getTimestamp().getTime()) || (x == 0)) {
                        money = getPerformanceItemValue(performanceItem);
                    } else {

                        PerformanceItem prevPerformanceItem = mPerformanceItems.get(x - 1);


                        long deltaY = getPerformanceItemValue(performanceItem).getMicroCents() -
                                getPerformanceItemValue(prevPerformanceItem).getMicroCents();

                        long deltaX = performanceItem.getTimestamp().getTime() -
                                prevPerformanceItem.getTimestamp().getTime();

                        money = new Money((deltaY * (timestamp - prevPerformanceItem.getTimestamp().getTime())) / deltaX +
                                getPerformanceItemValue(prevPerformanceItem).getMicroCents());
                    }
                    break;
                }
            }

            if (money == null) {
                money = getPerformanceItemValue(mPerformanceItems.get(mPerformanceItems.size() - 1));
            }
        }

        return money;
    }

}

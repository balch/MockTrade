package com.balch.mocktrade.portfolio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
 * will be 30 minutes after the last quote time.
 *
 */
public class DailyGraphView extends View {
    private static final String TAG = DailyGraphView.class.getSimpleName();

    private static final int ANIMATION_DURATION_MS = 1000;

    private static final int[] LINEAR_GRADIENT_COLORS = new int[] {
            Color.argb(255, 0, 255, 0),
            Color.argb(255, 0, 156, 0),
            Color.argb(255, 156, 156, 156),
            Color.argb(255, 156, 0, 0),
            Color.argb(255, 255, 0, 0)
    };

    private static final float[] LINEAR_GRADIENT_POSITIONS = new float[] {
            0f, .475f, .5f, .525f, 1
    };

    private Paint mPathPaint;
    private Paint mMartketTimesPaint;
    private Path mPath;
    private float mPathLength;

    private List<PerformanceItem> mPerformanceItems;

    private int mWidth;
    private int mHeight;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private int mOffsetY;
    private long mOffsetX;

    private long mMaxY = Long.MIN_VALUE;
    private long mMinY = Long.MAX_VALUE;
    private long mMarketStartTime;
    private long mMarketEndTime;

    public DailyGraphView(Context context) {
        super(context);
        initialize();
    }

    public DailyGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mMarketStartTime != 0) {
            float marketStartX = scaleX(mMarketStartTime);
            canvas.drawLine(marketStartX, 0, marketStartX, mHeight, mMartketTimesPaint);
        }

        if (mMarketEndTime != 0) {
            float marketEndX = scaleX(mMarketEndTime);
            canvas.drawLine(marketEndX, 0, marketEndX, mHeight, mMartketTimesPaint);
        }
//        canvas.drawPath(mPath, mPathPaint);
    }

    private void initialize() {

        mMartketTimesPaint = new Paint();
        mMartketTimesPaint.setStyle(Paint.Style.STROKE);
        mMartketTimesPaint.setColor(Color.LTGRAY);
        mMartketTimesPaint.setStrokeWidth(2);
        mMartketTimesPaint.setAntiAlias(true);
        mMartketTimesPaint.setPathEffect(new DashPathEffect(new float[]{4, 4}, 0));

        mPathPaint = new Paint();
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setColor(Color.WHITE);
        mPathPaint.setStrokeWidth(5);
        mPathPaint.setAntiAlias(true);
        mPathPaint.setStrokeCap(Paint.Cap.ROUND);
        mPathPaint.setStrokeJoin(Paint.Join.ROUND);
        mPathPaint.setShadowLayer(7, 0, 0, Color.LTGRAY);

        if (!isInEditMode() && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, mPathPaint);
        }

        mPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if ((w != 0) && (h!=0)) {
            mWidth = w;
            mHeight = h;

            calculatePath();
        }
    }

    private void calculatePath() {
        mPath.rewind();

        if ((mPerformanceItems != null) && (mPerformanceItems.size() >= 2)) {
            calculateScale();

            List<PointF> points = new ArrayList<>(mPerformanceItems.size());

            for (PerformanceItem performanceItem : mPerformanceItems) {
                points.add(new PointF(scaleX(performanceItem.getTimestamp().getTime()),
                        scaleY(performanceItem.getTodayChange().getMicroCents())));
            }

            mPath.moveTo(points.get(0).x, points.get(0).y);
            for (int x = 1; x < points.size() - 1; x++) {
                mPath.quadTo(points.get(x).x, points.get(x).y, points.get(x + 1).x, points.get(x + 1).y);
            }

            int lastPos = points.size() - 1;
            mPath.lineTo(points.get(lastPos).x, points.get(lastPos).y);

            PathMeasure measure = new PathMeasure(mPath, false);
            mPathLength = measure.getLength();

            Shader shader = new LinearGradient(0, 0, 0, mHeight,
                    LINEAR_GRADIENT_COLORS,
                    LINEAR_GRADIENT_POSITIONS,
                    Shader.TileMode.CLAMP);

            mPathPaint.setShader(shader);
        }
    }

    private void calculateScale() {
        long absMaxY = Math.abs(mMaxY);
        long absMinY = Math.abs(mMinY);
        long deltaY = (absMaxY > absMinY) ? 2 * absMaxY : 2 * absMinY;

        long minDeltaY = (long)(.01f * mPerformanceItems.get(0).getValue().getMicroCents());
        if (deltaY < minDeltaY) {
            deltaY = minDeltaY;
        }
        mScaleY = mHeight / deltaY;
        mOffsetY = mHeight / 2; // 0.0 will be the vertical center

        long minX = mPerformanceItems.get(0).getTimestamp().getTime();
        long maxX = mPerformanceItems.get(mPerformanceItems.size() - 1).getTimestamp().getTime();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        cal.setTimeInMillis(minX);
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 30);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        mMarketStartTime = cal.getTimeInMillis();
        cal.set(Calendar.MINUTE, 0);
        long startScaleX = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 0);
        mMarketEndTime = cal.getTimeInMillis();

        cal.set(Calendar.MINUTE, 30);
        long endScaleX = cal.getTimeInMillis();

        mScaleX = mWidth / (float) (endScaleX - startScaleX);
        mOffsetX = startScaleX;
    }

    public void animateGraph() {
        ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        va.setDuration(ANIMATION_DURATION_MS);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Float percentage = (Float) animation.getAnimatedValue();

                // change the path effect to determine how much of path to render
                float visibleLength = mPathLength * percentage;
                mPathPaint.setPathEffect(new DashPathEffect(new float[]{visibleLength, mPathLength - visibleLength}, 0));
                invalidate();
            }
        });
        va.start();
    }

    public void bind(List<PerformanceItem> performanceItems) {
        mPerformanceItems = performanceItems;

        mMaxY = Long.MIN_VALUE;
        mMinY = Long.MAX_VALUE;
        for (PerformanceItem performanceItem : mPerformanceItems) {
            long y = performanceItem.getTodayChange().getMicroCents();
            if (y > mMaxY) {
                mMaxY = y;
            }

            if (y < mMinY) {
                mMinY = y;
            }
        }

        calculatePath();
        animateGraph();
    }

    private float scaleX(float x) {
        return mOffsetX - (x * mScaleX);
    }

    private float scaleY(float y) {
        return mOffsetY - (y * mScaleY);
    }

}

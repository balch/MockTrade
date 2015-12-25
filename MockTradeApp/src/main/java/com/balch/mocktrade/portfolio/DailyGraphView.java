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


public class DailyGraphView extends View {
    private static final String TAG = DailyGraphView.class.getSimpleName();

    private static final int ANIMATION_DURATION_MS = 1000;

    private static final int[] LINEAR_GRADIENT_COLORS = new int[]
            {Color.argb(255, 0, 255, 0),
            Color.argb(255, 0, 156, 0),
            Color.argb(255, 156, 156, 156),
            Color.argb(255, 156, 0, 0),
            Color.argb(255, 255, 0, 0) };

    private static final float[] LINEAR_GRADIENT_POSITIONS = new float[] {0f, .475f, .5f, .525f, 1};

    private Paint mPathPaint;
    private Path mPath;
    private float mPathLength;

    private int mWidth;
    private int mHeight;

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
        canvas.drawPath(mPath, mPathPaint);
    }

    private void initialize() {

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

        PointF[] points = new PointF[]{
                new PointF(0, 0),
                new PointF(mWidth * .25f, mHeight * .25f),
                new PointF(mWidth * .75f, mHeight * .25f),
                new PointF(mWidth, mHeight),
        };


        if (points.length >= 3) {

            mPath.moveTo(points[0].x, points[0].y);
            for (int x = 1; x < points.length - 1; x++) {
                mPath.quadTo(points[x].x, points[x].y, points[x + 1].x, points[x + 1].y);
            }

            int lastPos = points.length - 1;
            mPath.lineTo(points[lastPos].x, points[lastPos].y);
        }

        PathMeasure measure = new PathMeasure(mPath, false);
        mPathLength = measure.getLength();

        Shader shader = new LinearGradient(0, 0, 0, mHeight,
                LINEAR_GRADIENT_COLORS,
                LINEAR_GRADIENT_POSITIONS,
                Shader.TileMode.CLAMP);

        mPathPaint.setShader(shader);
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

}

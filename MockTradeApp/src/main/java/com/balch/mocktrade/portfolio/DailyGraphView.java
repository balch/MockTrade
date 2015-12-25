package com.balch.mocktrade.portfolio;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.balch.mocktrade.R;


public class DailyGraphView extends View {
    private static final String TAG = DailyGraphView.class.getSimpleName();

    public DailyGraphView(Context context) {
        super(context);
        initialize();
    }

    public DailyGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DailyGraphView,
                0, 0
        );

        try {
/*
            mTextY = a.getDimension(R.styleable.DailyGraphView_labelY, 0.0f);
            mTextWidth = a.getDimension(R.styleable.DailyGraphView_labelWidth, 0.0f);
            mTextHeight = a.getDimension(R.styleable.DailyGraphView_labelHeight, 0.0f);
            mTextPos = a.getInteger(R.styleable.DailyGraphView_labelPosition, 0);
            mTextColor = a.getColor(R.styleable.DailyGraphView_labelColor, 0xff000000);
*/
        } finally {
            // release the TypedArray so that it can be reused.
            a.recycle();
        }

        initialize();
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }

    private void initialize() {
    }

    private void setLayerToSW(View v) {
        if (!v.isInEditMode() && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    private void setLayerToHW(View v) {
        if (!v.isInEditMode() && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

}

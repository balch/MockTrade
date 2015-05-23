/*
 * Author: Balch
 * Created: 9/4/14 12:26 AM
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

package com.balch.android.app.framework.nav;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balch.android.app.framework.R;

public class NavButton extends LinearLayout {

    protected int offColor;
    protected int onColor;
    protected int imageResourceId;
    protected int state;

    protected Drawable image;

    protected LinearLayout rootLayout;
    protected ImageView imageView;
    protected TextView textView;

    public NavButton(Context context) {
        super(context);
        initialize(null);
    }

    public NavButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs);
    }

    public NavButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(attrs);
    }

    protected void initialize(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray attributes = this.getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.NavButton,
                    0, 0);

            try {
                this.imageResourceId = attributes.getResourceId(R.styleable.NavButton_image, 0);
                this.offColor = attributes.getColor(R.styleable.NavButton_off_color, Color.GRAY);
                this.onColor = attributes.getColor(R.styleable.NavButton_on_color, Color.BLUE);
                this.state = attributes.getInteger(R.styleable.NavButton_state, 0);

                if (this.imageResourceId != 0) {
                    this.image = this.getResources().getDrawable(this.imageResourceId);
                }


            } finally {
                attributes.recycle();
            }
        }

        inflate(getContext(), R.layout.nav_button, this);
        this.rootLayout = (LinearLayout) this.findViewById(R.id.nav_button_layout);
        this.textView = (TextView) this.findViewById(R.id.nav_button_text);
        this.imageView = (ImageView) this.findViewById(R.id.nav_button_image);

        intitializeState();
    }

    protected void intitializeState() {
        int color = this.getButtonColor();
        if (this.image != null) {
            this.image.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            this.imageView.setImageDrawable(this.image);
        }

        this.textView.setTextColor(color);
    }

    protected int getButtonColor() {
        return (state == 0) ? this.offColor : this.onColor;
    }

    public int getOffColor() {
        return offColor;
    }

    public void setOffColor(int offColor) {
        this.offColor = offColor;
        intitializeState();
    }

    public int getOnColor() {
        return onColor;
    }

    public void setOnColor(int onColor) {
        this.onColor = onColor;
        intitializeState();
    }

    public Drawable getImage() {
        return this.image;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
        if (this.imageResourceId != 0) {
            this.image = this.getResources().getDrawable(this.imageResourceId);
        }

        intitializeState();
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
        intitializeState();
    }

    public void setTextSize(int unit, float size) {
        this.textView.setTextSize(unit, size);
    }

    public void setText(CharSequence text) {
        boolean isVisible = !TextUtils.isEmpty(text);
        this.textView.setVisibility(isVisible ? VISIBLE : GONE);

        if (isVisible) {
            this.textView.setText(text);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);

        ss.offColor = this.offColor;
        ss.onColor = this.onColor;
        ss.imageResourceId = this.imageResourceId;
        ss.state = this.state;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());

        this.offColor = ss.offColor;
        this.onColor = ss.onColor;
        this.imageResourceId = ss.imageResourceId;
        this.state = ss.state;

        if (this.imageResourceId != 0) {
            this.image = this.getResources().getDrawable(this.imageResourceId);
        }
    }

    static class SavedState extends BaseSavedState {
        protected int offColor;
        protected int onColor;
        protected int imageResourceId;
        protected int state;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.offColor = in.readInt();
            this.onColor = in.readInt();
            this.imageResourceId = in.readInt();
            this.state = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.offColor);
            out.writeInt(this.onColor);
            out.writeInt(this.imageResourceId);
            out.writeInt(this.state);
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}

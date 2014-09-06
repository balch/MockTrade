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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.balch.android.app.framework.R;

import java.util.ArrayList;
import java.util.List;

public class NavBar extends LinearLayout {

    protected List<NavButton> buttons;
    protected LinearLayout rootLayout;

    protected OnNavClickListener onNavClickListener;

    protected int onColor;
    protected int offColor;

    protected int selectedIndex;

    public NavBar(Context context) {
        super(context);
        init(null, 0);
    }

    public NavBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public NavBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    public void init(AttributeSet attrs, int defStyle) {
        this.buttons = new ArrayList<NavButton>();
        inflate(getContext(), R.layout.nav_bar, this);
        this.rootLayout = (LinearLayout) this.findViewById(R.id.nav_bar_layout);
    }

    public void setOnNavClickListener(OnNavClickListener onNavClickListener) {
        this.onNavClickListener = onNavClickListener;
    }

    public NavButton addButton(int textId, int drawableId) {
        Resources resources = this.getResources();

        int orientation = resources.getConfiguration().orientation;

        // add a separator if this is not the first button
        if (!this.buttons.isEmpty()) {
            ImageView imageView = new ImageView(this.getContext());

            LayoutParams params;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            } else {
                params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
            }

            imageView.setLayoutParams(params);
            imageView.setImageDrawable(resources.getDrawable(R.drawable.nav_sep_buttons));
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            this.rootLayout.addView(imageView);
        }

        LayoutParams params = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        params.weight = 1;
        params.gravity = Gravity.CENTER;
        final NavButton button = new NavButton(this.getContext());
        button.setLayoutParams(params);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        button.setImageResourceId(drawableId);
        button.setText(resources.getText(textId));
        button.setOnColor(this.onColor);
        button.setOffColor(this.offColor);
        button.setTag(new Integer(this.buttons.size()));
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NavButton button = (NavButton) v;

                Integer idx = (Integer) button.getTag();
                NavBar.this.buttons.get(NavBar.this.selectedIndex).setState(0);
                NavBar.this.selectedIndex = idx;
                NavBar.this.buttons.get(NavBar.this.selectedIndex).setState(1);

                if (NavBar.this.onNavClickListener != null) {
                    NavBar.this.onNavClickListener.onClick(button);
                }

            }
        });

        this.rootLayout.addView(button);
        this.buttons.add(button);
        return button;
    }

    public void completeConfiguration() {
        if (this.selectedIndex < buttons.size()) {
            buttons.get(this.selectedIndex).setState(1);
        }
        this.rootLayout.setVisibility(View.VISIBLE);
    }

    public void configure(int onColor, int offColor) {
        this.onColor = onColor;
        this.offColor = offColor;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.buttons.get(this.selectedIndex).setState(0);
        this.selectedIndex = selectedIndex;
        this.buttons.get(this.selectedIndex).setState(1);
    }

    public interface OnNavClickListener {
        void onClick(NavButton navButton);
    }

    @Override
    public void setBackground(Drawable drawable) {
        super.setBackground(drawable);
        this.rootLayout.setBackground(drawable);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);

        ss.offColor = this.offColor;
        ss.onColor = this.onColor;
        ss.selectedIndex = this.selectedIndex;

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
        this.setSelectedIndex(ss.selectedIndex);
    }

    static class SavedState extends BaseSavedState {
        protected int offColor;
        protected int onColor;
        protected int selectedIndex;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.offColor = in.readInt();
            this.onColor = in.readInt();
            this.selectedIndex = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.offColor);
            out.writeInt(this.onColor);
            out.writeInt(this.selectedIndex);
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

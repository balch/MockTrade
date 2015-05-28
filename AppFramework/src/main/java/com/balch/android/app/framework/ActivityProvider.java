package com.balch.android.app.framework;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;

public interface ActivityProvider {
    void showProgress();
    void hideProgress();
    void showView(Fragment fragment);
    FragmentManager getFragManager();
}

package com.balch.android.app.framework;

import android.app.Fragment;
import android.app.FragmentManager;

public interface ActivityProvider {
    void showProgress();
    void hideProgress();
    void showView(Fragment fragment);
    FragmentManager getFragManager();
}

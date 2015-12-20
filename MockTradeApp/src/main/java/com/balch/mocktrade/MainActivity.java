package com.balch.mocktrade;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.balch.android.app.framework.NavBarActivity;
import com.balch.android.app.framework.nav.NavBar;
import com.balch.android.app.framework.nav.NavButton;
import com.balch.mocktrade.portfolio.PortfolioFragment;
import com.balch.mocktrade.settings.SettingsFragment;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends NavBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Map<NavButton, Fragment> buttonMap = new HashMap<>();

    @Override
    public void configureNavBar(NavBar navBar, Bundle savedInstanceState) {
        showProgress();

        Resources resources = getResources();

        setBackground(resources.getDrawable(R.drawable.main_bmp_rpt));

        navBar.setBackground(resources.getDrawable(R.drawable.navbar_bkg));
        navBar.configure(resources.getColor(R.color.nav_on_color), resources.getColor(R.color.nav_off_color));

        Fragment mainFragment = new PortfolioFragment();
        this.buttonMap.put(navBar.addButton(R.string.nav_money_center, R.drawable.ic_nav_money_center), mainFragment);
        this.buttonMap.put(navBar.addButton(R.string.nav_research, R.drawable.ic_nav_research), new WebViewFragment().setCustomArguments("http://www.cnbc.com/"));
        this.buttonMap.put(navBar.addButton(R.string.nav_ideas, R.drawable.ic_nav_ideas), new WebViewFragment().setCustomArguments("http://wallstcheatsheet.com/category/investing/"));
        this.buttonMap.put(navBar.addButton(R.string.nav_settings, R.drawable.ic_nav_settings), new SettingsFragment());

        navBar.setOnNavClickListener(new NavBar.OnNavClickListener() {
            @Override
            public void onClick(NavButton button) {
                Fragment fragment = MainActivity.this.buttonMap.get(button);
                if (fragment != null) {
                    showView(fragment);
                }
            }
        });

        if (savedInstanceState == null) {
            showView(mainFragment);
        }

    }

}

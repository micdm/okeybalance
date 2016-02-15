package com.micdm.okeybalance.utils;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.micdm.okeybalance.AnalyticsConfig;

public class AnalyticsTracker {

    protected Tracker tracker;

    public void trackScreenView(Context context, String name) {
        Tracker tracker = getTracker(context);
        if (tracker != null) {
            tracker.setScreenName(name);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    protected Tracker getTracker(Context context) {
        if (AnalyticsConfig.ID == null) {
            return null;
        }
        if (tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
            return analytics.newTracker(AnalyticsConfig.ID);
        }
        return tracker;
    }
}

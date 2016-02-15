package com.micdm.okeybalance;

import com.micdm.okeybalance.events.EventBus;
import com.micdm.okeybalance.utils.AnalyticsTracker;

public class Application extends android.app.Application {

    protected final EventBus eventBus = new EventBus();
    protected final AnalyticsTracker analyticsTracker = new AnalyticsTracker();

    public EventBus getEventBus() {
        return eventBus;
    }

    public AnalyticsTracker getAnalyticsTracker() {
        return analyticsTracker;
    }
}

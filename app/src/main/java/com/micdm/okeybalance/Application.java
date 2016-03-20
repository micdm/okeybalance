package com.micdm.okeybalance;

import com.micdm.okeybalance.events.EventBus;
import com.micdm.okeybalance.events.IEventBusKeeper;
import com.micdm.okeybalance.utils.analytics.AnalyticsTracker;
import com.micdm.okeybalance.utils.analytics.IAnalyticsTrackerKeeper;

public class Application extends android.app.Application implements IEventBusKeeper, IAnalyticsTrackerKeeper {

    protected final EventBus eventBus = new EventBus();
    protected final AnalyticsTracker analyticsTracker = new AnalyticsTracker();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public AnalyticsTracker getAnalyticsTracker() {
        return analyticsTracker;
    }
}

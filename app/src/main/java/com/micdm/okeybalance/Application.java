package com.micdm.okeybalance;

import com.micdm.okeybalance.events.EventBus;

public class Application extends android.app.Application {

    protected static final EventBus eventBus = new EventBus();

    public static EventBus getEventBus() {
        return eventBus;
    }
}

package com.micdm.okeybalance;

import com.micdm.okeybalance.events.EventBus;

public class Application extends android.app.Application {

    protected static Application _instance;
    protected static final EventBus _eventBus = new EventBus();

    public static Application get() {
        return _instance;
    }

    public static EventBus getEventBus() {
        return _eventBus;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _instance = this;
    }
}

package com.micdm.okeybalance.events;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

public class EventBus {

    protected final Subject<Event, Event> subject = PublishSubject.create();

    public void send(Event event) {
        subject.onNext(event);
    }

    public Observable<Event> getEventObservable(final Class eventClass) {
        return subject
            .observeOn(AndroidSchedulers.mainThread())
            .filter(eventClass::isInstance);
    }
}

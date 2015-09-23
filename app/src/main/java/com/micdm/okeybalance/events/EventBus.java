package com.micdm.okeybalance.events;

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

public class EventBus {

    private final Subject<Event, Event> subject = PublishSubject.create();

    public void send(Event event) {
        subject.onNext(event);
    }

    public Observable<Event> getObservable() {
        return subject;
    }

    public Observable<Event> getEventObservable(final Class eventClass) {
        return getObservable()
            .filter(new Func1<Event, Boolean>() {
                @Override
                public Boolean call(Event event) {
                    return eventClass.isInstance(event);
                }
            });
    }
}

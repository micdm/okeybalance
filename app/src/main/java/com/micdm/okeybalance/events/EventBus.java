package com.micdm.okeybalance.events;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

public class EventBus {

    protected final Subject<Event, Event> subject = PublishSubject.create();

    public void send(Event event) {
        subject.onNext(event);
    }

    public <T extends Event> Observable<T> getEventObservable(Class<T> eventClass) {
        return subject
            .ofType(eventClass)
            .cast(eventClass);
    }
}

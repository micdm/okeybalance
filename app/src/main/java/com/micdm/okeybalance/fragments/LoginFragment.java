package com.micdm.okeybalance.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.micdm.okeybalance.Application;
import com.micdm.okeybalance.R;
import com.micdm.okeybalance.events.Event;
import com.micdm.okeybalance.events.LoginEvent;
import com.micdm.okeybalance.events.LoginFailedEvent;
import com.micdm.okeybalance.events.RequestLoginEvent;

import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

public class LoginFragment extends Fragment {

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    private final CompositeSubscription subscriptions = new CompositeSubscription();

    @Bind(R.id.f__login__card_number)
    protected TextView cardNumberView;
    @Bind(R.id.f__login__password)
    protected TextView passwordView;
    @Bind(R.id.f__login__submit)
    protected View submitView;
    @Bind(R.id.f__login__error)
    protected TextView errorView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f__login, container, false);
        ButterKnife.bind(this, view);
        subscriptions.add(subscribeForChangeText());
        subscriptions.add(subscribeForSubmit());
        subscriptions.add(subscribeForRequestLoginEvent());
        subscriptions.add(subscribeForLoginFailedEvent());
        subscriptions.add(subscribeForLoginEvent());
        return view;
    }

    private Subscription subscribeForChangeText() {
        return Observable.merge(RxTextView.textChanges(cardNumberView), RxTextView.textChanges(passwordView))
            .map(new Func1<CharSequence, Boolean>() {
                @Override
                public Boolean call(CharSequence charSequence) {
                    return charSequence.length() != 0;
                }
            })
            .startWith(false)
            .distinctUntilChanged()
            .subscribe(RxView.enabled(submitView));
    }

    private Subscription subscribeForSubmit() {
        return RxView.clicks(submitView)
            .throttleWithTimeout(300, TimeUnit.MILLISECONDS)
            .subscribe(new Action1<Object>() {
                @Override
                public void call(Object o) {
                    String cardNumber = cardNumberView.getText().toString();
                    String password = passwordView.getText().toString();
                    Event event = new RequestLoginEvent(cardNumber, password);
                    Application.getEventBus().send(event);
                }
            });
    }

    private Subscription subscribeForRequestLoginEvent() {
        return Application.getEventBus().getEventObservable(RequestLoginEvent.class)
            .map(new Func1<Event, Boolean>() {
                @Override
                public Boolean call(Event event) {
                    return false;
                }
            })
            .doOnNext(RxView.enabled(cardNumberView))
            .doOnNext(RxView.enabled(passwordView))
            .doOnNext(RxView.enabled(submitView))
            .subscribe();
    }

    private Subscription subscribeForLoginFailedEvent() {
        Observable<Event> eventObservable = Application.getEventBus().getEventObservable(LoginFailedEvent.class);
        CompositeSubscription subscription = new CompositeSubscription();
        subscription.add(eventObservable
            .map(new Func1<Event, Boolean>() {
                @Override
                public Boolean call(Event event) {
                    return true;
                }
            })
            .doOnNext(RxView.enabled(cardNumberView))
            .doOnNext(RxView.enabled(passwordView))
            .doOnNext(RxView.enabled(submitView))
            .subscribe());
        subscription.add(eventObservable
            .map(new Func1<Event, String>() {
                @Override
                public String call(Event event) {
                    switch (((LoginFailedEvent) event).reason) {
                        case SERVER_UNAVAILABLE:
                            return "Сервер недоступен";
                        case WRONG_CREDENTIALS:
                            return "Неправильные номер карты или пароль";
                        default:
                            return "Неизвестная ошибка";
                    }
                }
            })
            .subscribe(RxTextView.text(errorView)));
        subscription.add(eventObservable
            .map(new Func1<Event, Boolean>() {
                @Override
                public Boolean call(Event event) {
                    return true;
                }
            })
            .subscribe(RxView.visibility(errorView)));
        return subscription;
    }

    private Subscription subscribeForLoginEvent() {
        Observable<Event> eventObservable = Application.getEventBus().getEventObservable(LoginEvent.class);
        CompositeSubscription subscription = new CompositeSubscription();
        subscription.add(eventObservable
            .map(new Func1<Event, Boolean>() {
                @Override
                public Boolean call(Event event) {
                    return true;
                }
            })
            .doOnNext(RxView.enabled(cardNumberView))
            .doOnNext(RxView.enabled(passwordView))
            .doOnNext(RxView.enabled(submitView))
            .subscribe());
        subscription.add(eventObservable
            .map(new Func1<Event, Boolean>() {
                @Override
                public Boolean call(Event event) {
                    return false;
                }
            })
            .subscribe(RxView.visibility(errorView)));
        return subscription;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscriptions.unsubscribe();
        ButterKnife.unbind(this);
    }
}

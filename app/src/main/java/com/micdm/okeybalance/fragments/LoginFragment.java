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
import com.micdm.okeybalance.events.AuthRequiredEvent;
import com.micdm.okeybalance.events.Event;
import com.micdm.okeybalance.events.RequestLoginEvent;
import com.micdm.okeybalance.events.ServerUnavailableEvent;

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
        subscriptions.add(subscribeForServerUnavailableEvent());
        subscriptions.add(subscribeForAuthRequiredEvent());

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

    private Subscription subscribeForServerUnavailableEvent() {
        return Application.getEventBus().getEventObservable(ServerUnavailableEvent.class)
            .map(new Func1<Event, String>() {
                @Override
                public String call(Event event) {
                    return "Сервер недоступен";
                }
            })
            .subscribe(RxTextView.text(errorView));
    }

    private Subscription subscribeForAuthRequiredEvent() {
        return Application.getEventBus().getEventObservable(AuthRequiredEvent.class)
            .map(new Func1<Event, String>() {
                @Override
                public String call(Event event) {
                    return "Неправильный номер карты или пароль";
                }
            })
            .subscribe(RxTextView.text(errorView));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscriptions.unsubscribe();
        ButterKnife.unbind(this);
    }
}

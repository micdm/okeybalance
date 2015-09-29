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
import com.micdm.okeybalance.events.EventBus;
import com.micdm.okeybalance.events.FinishLoginRequestEvent;
import com.micdm.okeybalance.events.RequestLoginEvent;
import com.micdm.okeybalance.events.StartLoginRequestEvent;
import com.micdm.okeybalance.events.WrongCredentialsEvent;

import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class LoginFragment extends Fragment {

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    protected final CompositeSubscription subscriptions = new CompositeSubscription();

    @Bind(R.id.f__login__card_number)
    protected TextView cardNumberView;
    @Bind(R.id.f__login__password)
    protected TextView passwordView;
    @Bind(R.id.f__login__submit)
    protected View submitView;
    @Bind(R.id.f__login__error)
    protected TextView errorView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f__login, container, false);
        ButterKnife.bind(this, view);
        EventBus eventBus = Application.getEventBus();
        subscribeForEvents(eventBus);
        return view;
    }

    protected void subscribeForEvents(EventBus eventBus) {
        subscriptions.add(subscribeForChangeText());
        subscriptions.add(subscribeForSubmit(eventBus));
        subscriptions.add(subscribeForStartLoginRequestEvent(eventBus));
        subscriptions.add(subscribeForFinishLoginRequestEvent(eventBus));
        subscriptions.add(subscribeForWrongCredentialsEvent(eventBus));
    }

    protected Subscription subscribeForChangeText() {
        return Observable.combineLatest(isInputValid(cardNumberView), isInputValid(passwordView), (isValid1, isValid2) -> isValid1 && isValid2)
            .distinctUntilChanged()
            .subscribe(RxView.enabled(submitView));
    }

    protected Observable<Boolean> isInputValid(TextView view) {
        return RxTextView.textChanges(view)
            .map(charSequence -> charSequence.length() != 0)
            .startWith(false);
    }

    protected Subscription subscribeForSubmit(EventBus eventBus) {
        return RxView.clicks(submitView)
            .throttleWithTimeout(300, TimeUnit.MILLISECONDS)
            .map(o -> {
                String cardNumber = cardNumberView.getText().toString();
                String password = passwordView.getText().toString();
                return new RequestLoginEvent(cardNumber, password);
            })
            .subscribe(eventBus::send);
    }

    protected Subscription subscribeForStartLoginRequestEvent(EventBus eventBus) {
        return eventBus.getEventObservable(StartLoginRequestEvent.class)
            .map(event -> false)
            .doOnNext(RxView.enabled(cardNumberView))
            .doOnNext(RxView.enabled(passwordView))
            .doOnNext(RxView.enabled(submitView))
            .doOnNext(RxView.visibility(errorView))
            .subscribe();
    }

    protected Subscription subscribeForFinishLoginRequestEvent(EventBus eventBus) {
        return eventBus.getEventObservable(FinishLoginRequestEvent.class)
            .map(event -> true)
            .doOnNext(RxView.enabled(cardNumberView))
            .doOnNext(RxView.enabled(passwordView))
            .doOnNext(RxView.enabled(submitView))
            .subscribe();
    }

    protected Subscription subscribeForWrongCredentialsEvent(EventBus eventBus) {
        return eventBus.getEventObservable(WrongCredentialsEvent.class)
            .map(event -> R.string.f__login__wrong_credentials_error)
            .doOnNext(RxTextView.textRes(errorView))
            .map(event -> true)
            .doOnNext(RxView.visibility(errorView))
            .subscribe();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscriptions.unsubscribe();
        ButterKnife.unbind(this);
    }
}

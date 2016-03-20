package com.micdm.okeybalance.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.micdm.okeybalance.R;
import com.micdm.okeybalance.events.EventBus;
import com.micdm.okeybalance.events.FinishLoginRequestEvent;
import com.micdm.okeybalance.events.IEventBusKeeper;
import com.micdm.okeybalance.events.RequestLoginEvent;
import com.micdm.okeybalance.events.StartLoginRequestEvent;
import com.micdm.okeybalance.events.WrongCredentialsEvent;
import com.micdm.okeybalance.utils.analytics.AnalyticsTracker;
import com.micdm.okeybalance.utils.analytics.IAnalyticsTrackerKeeper;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class LoginFragment extends Fragment {

    private static final String CARD_NUMBER_ARG_KEY = "cardNumber";

    public static LoginFragment newInstance(String cardNumber) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle(1);
        args.putString(CARD_NUMBER_ARG_KEY, cardNumber);
        fragment.setArguments(args);
        return fragment;
    }

    protected Subscription subscription;
    protected AnalyticsTracker analyticsTracker;

    @Bind(R.id.f__login__card_number)
    protected TextView cardNumberView;
    @Bind(R.id.f__login__password)
    protected TextView passwordView;
    @Bind(R.id.f__login__submit)
    protected View submitView;
    @Bind(R.id.f__login__error)
    protected TextView errorView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        analyticsTracker = ((IAnalyticsTrackerKeeper) getActivity().getApplication()).getAnalyticsTracker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f__login, container, false);
        ButterKnife.bind(this, view);
        setupCardNumber();
        EventBus eventBus = ((IEventBusKeeper) getActivity().getApplication()).getEventBus();
        subscription = subscribeForEvents(eventBus);
        return view;
    }

    protected void setupCardNumber() {
        String cardNumber = getArguments().getString(CARD_NUMBER_ARG_KEY);
        if (cardNumber != null) {
            cardNumberView.setText(cardNumber);
        }
    }

    protected Subscription subscribeForEvents(EventBus eventBus) {
        return new CompositeSubscription(
            subscribeForChangeText(),
            subscribeForStartLoginRequestEvent(eventBus),
            subscribeForFinishLoginRequestEvent(eventBus),
            subscribeForWrongCredentialsEvent(eventBus),
            subscribeForClickSubmitButton(eventBus)
        );
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
        Observable<WrongCredentialsEvent> common = eventBus.getEventObservable(WrongCredentialsEvent.class);
        return new CompositeSubscription(
            common
                .map(event -> R.string.f__login__wrong_credentials_error)
                .subscribe(RxTextView.textRes(errorView)),
            common
                .map(event -> true)
                .subscribe(RxView.visibility(errorView))
        );
    }

    protected Subscription subscribeForClickSubmitButton(EventBus eventBus) {
        return RxView.clicks(submitView)
            .doOnNext(o -> analyticsTracker.trackEvent(getContext(), AnalyticsTracker.CATEGORY_LOGIN, "click", "submit"))
            .map(o -> {
                String cardNumber = cardNumberView.getText().toString();
                String password = passwordView.getText().toString();
                return new RequestLoginEvent(cardNumber, password);
            })
            .subscribe(eventBus::send);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (subscription != null) {
            subscription.unsubscribe();
        }
        ButterKnife.unbind(this);
    }
}

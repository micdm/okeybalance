package com.micdm.okeybalance.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.micdm.okeybalance.R;
import com.micdm.okeybalance.events.BalanceEvent;
import com.micdm.okeybalance.events.EventBus;
import com.micdm.okeybalance.events.FinishBalanceRequestEvent;
import com.micdm.okeybalance.events.IEventBusKeeper;
import com.micdm.okeybalance.events.RequestBalanceEvent;
import com.micdm.okeybalance.events.RequestLogoutEvent;
import com.micdm.okeybalance.events.StartBalanceRequestEvent;
import com.micdm.okeybalance.utils.MarketUtils;
import com.micdm.okeybalance.utils.ObservableFactory;
import com.micdm.okeybalance.utils.analytics.AnalyticsTracker;
import com.micdm.okeybalance.utils.analytics.IAnalyticsTrackerKeeper;

import java.math.BigDecimal;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class BalanceFragment extends Fragment {

    protected static final String ARGUMENT_CARD_NUMBER = "card_number";
    protected static final String ARGUMENT_PASSWORD = "password";

    protected Subscription subscription;
    protected Animation incomeAnimation;
    protected Animation outcomeAnimation;
    protected Pair<Observable<Object>, Observable<Object>> incomeAnimationObservables;
    protected Pair<Observable<Object>, Observable<Object>> outcomeAnimationObservables;
    protected AnalyticsTracker analyticsTracker;

    @Bind(R.id.f__balance__share)
    protected View shareView;
    @Bind(R.id.f__balance__logout)
    protected View logoutView;
    @Bind(R.id.f__balance__delta)
    protected TextView deltaView;
    @Bind(R.id.f__balance__balance)
    protected TextView balanceView;
    @Bind(R.id.f__balance__container)
    protected View reloadView;
    @Bind(R.id.f__balance__tip)
    protected TextView tipView;

    public static Fragment newInstance(String cardNumber, String password) {
        Bundle args = new Bundle();
        args.putString(ARGUMENT_CARD_NUMBER, cardNumber);
        args.putString(ARGUMENT_PASSWORD, password);
        Fragment fragment = new BalanceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    protected String getCardNumber() {
        return getArguments().getString(ARGUMENT_CARD_NUMBER);
    }

    protected String getPassword() {
        return getArguments().getString(ARGUMENT_PASSWORD);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        incomeAnimation = AnimationUtils.loadAnimation(activity, R.anim.income);
        outcomeAnimation = AnimationUtils.loadAnimation(activity, R.anim.outcome);
        incomeAnimationObservables = ObservableFactory.getForAnimation(incomeAnimation);
        outcomeAnimationObservables = ObservableFactory.getForAnimation(outcomeAnimation);
        analyticsTracker = ((IAnalyticsTrackerKeeper) getActivity().getApplication()).getAnalyticsTracker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f__balance, container, false);
        ButterKnife.bind(this, view);
        setupViews();
        EventBus eventBus = ((IEventBusKeeper) getActivity().getApplication()).getEventBus();
        subscription = subscribeForEvents(eventBus);
        eventBus.send(new RequestBalanceEvent(getCardNumber(), getPassword()));
        return view;
    }

    protected void setupViews() {
        shareView.setVisibility(View.GONE);
        deltaView.setVisibility(View.INVISIBLE);
        balanceView.setVisibility(View.GONE);
    }

    protected Subscription subscribeForEvents(EventBus eventBus) {
        return new CompositeSubscription(
            subscribeForAnimation(),
            subscribeForStartBalanceRequestEvent(eventBus),
            subscribeForFinishBalanceRequestEvent(eventBus),
            subscribeForBalanceEvent(eventBus),
            subscribeForClickReloadButton(eventBus),
            subscribeForClickShareButton(eventBus),
            subscribeForClickLogoutButton(eventBus)
        );
    }

    protected Subscription subscribeForAnimation() {
        return new CompositeSubscription(
            Observable.merge(incomeAnimationObservables.first, outcomeAnimationObservables.first)
                .map(o -> true)
                .subscribe(RxView.visibility(deltaView)),
            Observable.merge(incomeAnimationObservables.second, outcomeAnimationObservables.second)
                .map(o -> false)
                .subscribe(RxView.visibility(deltaView, View.INVISIBLE)));
    }

    protected Subscription subscribeForStartBalanceRequestEvent(EventBus eventBus) {
        Observable<StartBalanceRequestEvent> common = eventBus.getEventObservable(StartBalanceRequestEvent.class);
        return new CompositeSubscription(
            common
                .map(event -> false)
                .subscribe(RxView.clickable(reloadView)),
            common
                .map(event -> R.string.f__balance__reloading)
                .subscribe(RxTextView.textRes(tipView))
        );
    }

    protected Subscription subscribeForFinishBalanceRequestEvent(EventBus eventBus) {
        Observable<FinishBalanceRequestEvent> common = eventBus.getEventObservable(FinishBalanceRequestEvent.class);
        return new CompositeSubscription(
            common
                .map(event -> true)
                .subscribe(RxView.clickable(reloadView)),
            common
                .map(event -> R.string.f__balance__press_to_reload)
                .subscribe(RxTextView.textRes(tipView))
        );
    }

    protected Subscription subscribeForBalanceEvent(EventBus eventBus) {
        Observable<Pair<BigDecimal, BigDecimal>> common = eventBus.getEventObservable(BalanceEvent.class)
            .map(event -> event.balance)
            .scan(null, (Pair<BigDecimal, BigDecimal> pair, BigDecimal balance) -> {
                if (pair == null) {
                    return new Pair<>(balance, null);
                }
                return new Pair<>(balance, balance.subtract(pair.first));
            })
            .filter(pair -> pair != null);
        return new CompositeSubscription(
            common
                .map(pair -> true)
                .subscribe(RxView.visibility(shareView)),
            common
                .map(pair -> true)
                .subscribe(RxView.visibility(balanceView)),
            common
                .filter(pair -> pair.second == null)
                .map(pair -> getString(R.string.f__balance__balance, pair.first))
                .subscribe(RxTextView.text(balanceView)),
            common
                .filter(pair -> pair.second != null && pair.second.compareTo(BigDecimal.ZERO) == 1)
                .flatMap(pair -> {
                    deltaView.setText(getString(R.string.f__balance__income, pair.second));
                    deltaView.startAnimation(incomeAnimation);
                    return incomeAnimationObservables.second
                        .map(state -> getString(R.string.f__balance__balance, pair.first))
                        .doOnNext(RxTextView.text(balanceView));
                })
                .subscribe(),
            common
                .filter(pair -> pair.second != null && pair.second.compareTo(BigDecimal.ZERO) == -1)
                .flatMap(pair -> {
                    deltaView.setText(getString(R.string.f__balance__outcome, pair.second));
                    deltaView.startAnimation(outcomeAnimation);
                    return outcomeAnimationObservables.first
                        .map(state -> getString(R.string.f__balance__balance, pair.first))
                        .doOnNext(RxTextView.text(balanceView));
                })
                .subscribe()
        );
    }

    protected Subscription subscribeForClickReloadButton(EventBus eventBus) {
        return RxView.clicks(reloadView)
            .doOnNext(o -> analyticsTracker.trackEvent(getContext(), AnalyticsTracker.CATEGORY_BALANCE, "click", "reload"))
            .subscribe(o -> eventBus.send(new RequestBalanceEvent(getCardNumber(), getPassword())));
    }

    protected Subscription subscribeForClickShareButton(EventBus eventBus) {
        return Observable
            .combineLatest(
                RxView.clicks(shareView).timestamp(),
                eventBus.getEventObservable(BalanceEvent.class)
                    .map(event -> event.balance),
                (timestamp, balance) -> new Pair<>(timestamp, balance)
            )
            .distinctUntilChanged(pair -> pair.first)
            .map(pair -> pair.second)
            .doOnNext(o -> analyticsTracker.trackEvent(getContext(), AnalyticsTracker.CATEGORY_BALANCE, "click", "share"))
            .subscribe(this::shareBalance);
    }

    protected void shareBalance(BigDecimal balance) {
        int rounded = balance.setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
        String message;
        if (rounded == 0) {
            message = getString(R.string.f__balance__share_empty,
                                MarketUtils.getMarketUri(getContext().getPackageName()));
        } else {
            message = getString(R.string.f__balance__share,
                                getResources().getQuantityString(R.plurals.f__balance__share_plurals, rounded, rounded),
                                MarketUtils.getMarketUri(getContext().getPackageName()));
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setType("text/plain");
        startActivity(intent);
    }

    protected Subscription subscribeForClickLogoutButton(EventBus eventBus) {
        return RxView.clicks(logoutView)
            .doOnNext(o -> analyticsTracker.trackEvent(getContext(), AnalyticsTracker.CATEGORY_BALANCE, "click", "logout"))
            .subscribe(o -> eventBus.send(new RequestLogoutEvent()));
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

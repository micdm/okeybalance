package com.micdm.okeybalance.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.micdm.okeybalance.Application;
import com.micdm.okeybalance.R;
import com.micdm.okeybalance.events.BalanceEvent;
import com.micdm.okeybalance.events.Event;
import com.micdm.okeybalance.events.EventBus;
import com.micdm.okeybalance.events.FinishBalanceRequestEvent;
import com.micdm.okeybalance.events.RequestBalanceEvent;
import com.micdm.okeybalance.events.StartBalanceRequestEvent;
import com.micdm.okeybalance.utils.ObservableFactory;

import java.math.BigDecimal;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class BalanceFragment extends Fragment {

    public static BalanceFragment newInstance() {
        return new BalanceFragment();
    }

    protected final CompositeSubscription subscriptions = new CompositeSubscription();
    protected Animation incomeAnimation;
    protected Animation outcomeAnimation;
    protected Pair<Observable<Object>, Observable<Object>> incomeAnimationObservables;
    protected Pair<Observable<Object>, Observable<Object>> outcomeAnimationObservables;

    @Bind(R.id.f__balance__delta)
    protected TextView deltaView;
    @Bind(R.id.f__balance__balance)
    protected TextView balanceView;
    @Bind(R.id.f__balance__container)
    protected View reloadView;
    @Bind(R.id.f__balance__tip)
    protected TextView tipView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        incomeAnimation = AnimationUtils.loadAnimation(activity, R.anim.income);
        outcomeAnimation = AnimationUtils.loadAnimation(activity, R.anim.outcome);
        incomeAnimationObservables = ObservableFactory.getForAnimation(incomeAnimation);
        outcomeAnimationObservables = ObservableFactory.getForAnimation(outcomeAnimation);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f__balance, container, false);
        ButterKnife.bind(this, view);
        setupViews();
        EventBus eventBus = Application.getEventBus();
        subscribeForEvents(eventBus);
        eventBus.send(new RequestBalanceEvent());
        return view;
    }

    protected void setupViews() {
        deltaView.setVisibility(View.INVISIBLE);
        balanceView.setVisibility(View.GONE);
    }

    protected void subscribeForEvents(EventBus eventBus) {
        subscriptions.add(subscribeForReload(eventBus));
        subscriptions.add(subscribeForAnimation());
        subscriptions.add(subscribeForStartBalanceRequestEvent(eventBus));
        subscriptions.add(subscribeForFinishBalanceRequestEvent(eventBus));
        subscriptions.add(subscribeForBalanceEvent(eventBus));
    }

    protected Subscription subscribeForReload(EventBus eventBus) {
        return RxView.clicks(reloadView)
            .map(o -> new RequestBalanceEvent())
            .subscribe(eventBus::send);
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
        Observable<Event> eventObservable = eventBus.getEventObservable(StartBalanceRequestEvent.class);
        return new CompositeSubscription(
            eventObservable
                .map(event -> false)
                .subscribe(RxView.clickable(reloadView)),
            eventObservable
                .map(event -> R.string.f__balance__reloading)
                .subscribe(RxTextView.textRes(tipView))
        );
    }

    protected Subscription subscribeForFinishBalanceRequestEvent(EventBus eventBus) {
        Observable<Event> eventObservable = eventBus.getEventObservable(FinishBalanceRequestEvent.class);
        return new CompositeSubscription(
            eventObservable
                .map(event -> true)
                .subscribe(RxView.clickable(reloadView)),
            eventObservable
                .map(event -> R.string.f__balance__press_to_reload)
                .subscribe(RxTextView.textRes(tipView))
        );
    }

    protected Subscription subscribeForBalanceEvent(EventBus eventBus) {
        Observable<Event> eventObservable = eventBus.getEventObservable(BalanceEvent.class);
        Observable<Pair<BigDecimal, BigDecimal>> balanceObservable = eventObservable
            .map(event -> ((BalanceEvent) event).balance)
            .scan(null, (Pair<BigDecimal, BigDecimal> pair, BigDecimal balance) -> {
                if (pair == null) {
                    return new Pair<>(balance, null);
                }
                return new Pair<>(balance, balance.subtract(pair.first));
            })
            .filter(pair -> pair != null);
        return new CompositeSubscription(
            eventObservable
                .map(event -> true)
                .subscribe(RxView.visibility(balanceView)),
            balanceObservable
                .filter(pair -> pair.second != null && pair.second.compareTo(BigDecimal.ZERO) == 1)
                .flatMap(pair -> {
                    deltaView.setText(getString(R.string.f__balance__income, pair.second));
                    deltaView.setTextColor(getResources().getColor(R.color.income));
                    deltaView.startAnimation(incomeAnimation);
                    return incomeAnimationObservables.second
                        .map(state -> getString(R.string.f__balance__balance, pair.first))
                        .doOnNext(RxTextView.text(balanceView));
                })
                .subscribe(),
            balanceObservable
                .filter(pair -> pair.second != null && pair.second.compareTo(BigDecimal.ZERO) == -1)
                .flatMap(pair -> {
                    deltaView.setText(getString(R.string.f__balance__outcome, pair.second));
                    deltaView.setTextColor(getResources().getColor(R.color.outcome));
                    deltaView.startAnimation(outcomeAnimation);
                    return outcomeAnimationObservables.first
                        .map(state -> getString(R.string.f__balance__balance, pair.first))
                        .doOnNext(RxTextView.text(balanceView));
                })
                .subscribe()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscriptions.unsubscribe();
        ButterKnife.unbind(this);
    }
}

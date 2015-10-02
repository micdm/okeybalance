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

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
            .throttleWithTimeout(300, TimeUnit.MILLISECONDS)
            .map(o -> new RequestBalanceEvent())
            .subscribe(eventBus::send);
    }

    protected Subscription subscribeForAnimation() {
        CompositeSubscription subscription = new CompositeSubscription();
        Observable<String> incomeAnimationObservable = getAnimationObservable(incomeAnimation);
        Observable<String> outcomeAnimationObservable = getAnimationObservable(outcomeAnimation);
        subscription.add(Observable.merge(incomeAnimationObservable, outcomeAnimationObservable)
            .filter(event -> event.equals("start"))
            .map(o -> true)
            .subscribe(RxView.visibility(deltaView)));
        subscription.add(Observable.merge(incomeAnimationObservable, outcomeAnimationObservable)
            .filter(event -> event.equals("finish"))
            .map(o -> false)
            .subscribe(RxView.visibility(deltaView, View.INVISIBLE)));
        return subscription;
    }

    protected Observable<String> getAnimationObservable(Animation animation) {
        return Observable.create(subscriber -> animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                subscriber.onNext("start");
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                subscriber.onNext("finish");
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        }));
    }

    protected Subscription subscribeForStartBalanceRequestEvent(EventBus eventBus) {
        return eventBus.getEventObservable(StartBalanceRequestEvent.class)
            .map(event -> R.string.f__balance__reloading)
            .subscribe(RxTextView.textRes(tipView));
    }

    protected Subscription subscribeForFinishBalanceRequestEvent(EventBus eventBus) {
        return eventBus.getEventObservable(FinishBalanceRequestEvent.class)
            .map(event -> R.string.f__balance__press_to_reload)
            .subscribe(RxTextView.textRes(tipView));
    }

    protected Subscription subscribeForBalanceEvent(EventBus eventBus) {
        CompositeSubscription subscription = new CompositeSubscription();
        subscription.add(subscribeForBalance(eventBus));
        subscription.add(subscribeForBalanceDelta(eventBus));
        return subscription;
    }

    protected Subscription subscribeForBalance(EventBus eventBus) {
        CompositeSubscription subscription = new CompositeSubscription();
        Observable<Event> eventObservable = eventBus.getEventObservable(BalanceEvent.class);
        subscription.add(eventObservable
            .map(event -> ((BalanceEvent) event).balance)
            .map(balance -> getString(R.string.f__balance__balance, balance))
            .subscribe(RxTextView.text(balanceView)));
        subscription.add(eventObservable
            .map(event -> true)
            .subscribe(RxView.visibility(balanceView)));
        return subscription;
    }

    protected Subscription subscribeForBalanceDelta(EventBus eventBus) {
        CompositeSubscription subscription = new CompositeSubscription();
        Observable<BigDecimal> deltaObservable = eventBus.getEventObservable(BalanceEvent.class)
            .map(event -> ((BalanceEvent) event).balance)
            .scan(null, (Pair<BigDecimal, BigDecimal> pair, BigDecimal balance) -> {
                if (pair == null) {
                    return new Pair<>(balance, null);
                }
                return new Pair<>(balance, balance.subtract(pair.first));
            })
            .filter(pair -> pair != null)
            .map(pair -> pair.second)
            .cache();
        subscription.add(deltaObservable
            .filter(delta -> delta != null && delta.compareTo(BigDecimal.ZERO) == 1)
            .map(delta -> getString(R.string.f__balance__income, delta))
            .subscribe(delta -> {
                deltaView.setText(delta);
                deltaView.setTextColor(getResources().getColor(R.color.income));
                deltaView.startAnimation(incomeAnimation);
            }));
        subscription.add(deltaObservable
            .filter(delta -> delta != null && delta.compareTo(BigDecimal.ZERO) == -1)
            .map(delta -> getString(R.string.f__balance__outcome, delta))
            .subscribe(delta -> {
                deltaView.setText(delta);
                deltaView.setTextColor(getResources().getColor(R.color.outcome));
                deltaView.startAnimation(outcomeAnimation);
            }));
        return subscription;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscriptions.unsubscribe();
        ButterKnife.unbind(this);
    }
}

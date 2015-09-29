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
import com.micdm.okeybalance.events.BalanceEvent;
import com.micdm.okeybalance.events.EventBus;
import com.micdm.okeybalance.events.FinishBalanceRequestEvent;
import com.micdm.okeybalance.events.RequestBalanceEvent;
import com.micdm.okeybalance.events.StartBalanceRequestEvent;

import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class BalanceFragment extends Fragment {

    public static BalanceFragment newInstance() {
        return new BalanceFragment();
    }

    protected final CompositeSubscription subscriptions = new CompositeSubscription();

    @Bind(R.id.f__balance__balance)
    protected TextView balanceView;
    @Bind(R.id.f__balance__container)
    protected View reloadView;
    @Bind(R.id.f__balance__tip)
    protected TextView tipView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f__balance, container, false);
        ButterKnife.bind(this, view);
        EventBus eventBus = Application.getEventBus();
        subscribeForEvents(eventBus);
        eventBus.send(new RequestBalanceEvent());
        return view;
    }

    protected void subscribeForEvents(EventBus eventBus) {
        subscriptions.add(subscribeForReload(eventBus));
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
        return eventBus.getEventObservable(BalanceEvent.class)
            .map(event -> ((BalanceEvent) event).balance)
            .subscribe(RxTextView.text(balanceView));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscriptions.unsubscribe();
        ButterKnife.unbind(this);
    }
}

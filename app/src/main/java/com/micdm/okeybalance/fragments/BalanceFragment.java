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
import com.micdm.okeybalance.events.Event;
import com.micdm.okeybalance.events.RequestBalanceEvent;

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
        subscriptions.add(subscribeForReload());
        subscriptions.add(subscribeForRequestBalanceEvent());
        subscriptions.add(subscribeForBalanceEvent());
        Application.getEventBus().send(new RequestBalanceEvent());
        return view;
    }

    protected Subscription subscribeForReload() {
        return RxView.clicks(reloadView)
            .throttleWithTimeout(300, TimeUnit.MILLISECONDS)
            .subscribe(o -> {
                Application.getEventBus().send(new RequestBalanceEvent());
            });
    }

    protected Subscription subscribeForRequestBalanceEvent() {
        return Application.getEventBus().getEventObservable(RequestBalanceEvent.class)
            .map(event -> getString(R.string.f__balance__reloading))
            .subscribe(RxTextView.text(tipView));
    }

    protected Subscription subscribeForBalanceEvent() {
        Observable<Event> eventObservable = Application.getEventBus().getEventObservable(BalanceEvent.class);
        CompositeSubscription subscription = new CompositeSubscription();
        subscription.add(eventObservable
            .map(event -> ((BalanceEvent) event).balance)
            .subscribe(RxTextView.text(balanceView)));
        subscription.add(eventObservable
            .map(event -> getString(R.string.f__balance__press_to_reload))
            .subscribe(RxTextView.text(tipView)));
        return subscription;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscriptions.unsubscribe();
        ButterKnife.unbind(this);
    }
}

package com.micdm.okeybalance;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.micdm.okeybalance.content.InformationRetriever;
import com.micdm.okeybalance.events.AuthRequiredEvent;
import com.micdm.okeybalance.events.Event;
import com.micdm.okeybalance.events.EventBus;
import com.micdm.okeybalance.events.LoadBalanceEvent;
import com.micdm.okeybalance.events.RequestLoginEvent;
import com.micdm.okeybalance.events.ServerUnavailableEvent;
import com.micdm.okeybalance.exceptions.AuthRequiredException;
import com.micdm.okeybalance.exceptions.ServerUnavailableException;
import com.micdm.okeybalance.fragments.BalanceFragment;
import com.micdm.okeybalance.fragments.LoginFragment;

import java.math.BigDecimal;

import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {

    private final CompositeSubscription subscriptions = new CompositeSubscription();
    private String balance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a__main);
        subscribeForEvents();
        showFragment(LoginFragment.newInstance());
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.a__main__fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void subscribeForEvents() {
        EventBus eventBus = Application.getEventBus();
        subscriptions.add(subscribeForRequestLoginEvent(eventBus));
        subscriptions.add(subscribeForLoadBalanceEvent(eventBus));
    }

    private Subscription subscribeForRequestLoginEvent(final EventBus eventBus) {
        return eventBus.getEventObservable(RequestLoginEvent.class)
            //.subscribeOn(Schedulers.newThread())
            .map(new Func1<Event, String>() {
                @Override
                public String call(Event event) {
                    return InformationRetriever.getBalance(((RequestLoginEvent) event).cardNumber, ((RequestLoginEvent) event).password);
                }
            })
            .subscribe(new Subscriber<String>() {
                @Override
                public void onNext(String balance) {
                    MainActivity.this.balance = balance;
                }
                @Override
                public void onCompleted() {
                    return;
                }
                @Override
                public void onError(Throwable e) {
                    Log.e("okey", e.getMessage(), e);
                    if (e instanceof ServerUnavailableException) {
                        eventBus.send(new ServerUnavailableEvent());
                    }
                    if (e instanceof AuthRequiredException) {
                        eventBus.send(new AuthRequiredEvent());
                    }
                }
            });
    }

    private Subscription subscribeForLoadBalanceEvent(EventBus eventBus) {
        return eventBus.getEventObservable(LoadBalanceEvent.class)
            .subscribe(new Action1<Event>() {
                @Override
                public void call(Event event) {
                    showFragment(BalanceFragment.newInstance(((LoadBalanceEvent) event).balance));
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        subscriptions.unsubscribe();
    }
}

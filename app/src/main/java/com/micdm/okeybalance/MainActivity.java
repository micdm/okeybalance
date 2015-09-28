package com.micdm.okeybalance;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.micdm.okeybalance.content.InformationRetriever;
import com.micdm.okeybalance.events.BalanceEvent;
import com.micdm.okeybalance.events.Event;
import com.micdm.okeybalance.events.EventBus;
import com.micdm.okeybalance.events.LoginEvent;
import com.micdm.okeybalance.events.LoginFailedEvent;
import com.micdm.okeybalance.events.RequestBalanceEvent;
import com.micdm.okeybalance.events.RequestLoginEvent;
import com.micdm.okeybalance.events.RequireLoginEvent;
import com.micdm.okeybalance.exceptions.WrongCredentialsException;
import com.micdm.okeybalance.exceptions.ServerUnavailableException;
import com.micdm.okeybalance.fragments.BalanceFragment;
import com.micdm.okeybalance.fragments.LoginFragment;
import com.micdm.okeybalance.utils.CredentialStore;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {

    protected final CompositeSubscription subscriptions = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a__main);
        subscribeForEvents();
        init();
    }

    protected void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.a__main__fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    protected void subscribeForEvents() {
        EventBus eventBus = Application.getEventBus();
        subscriptions.add(subscribeForRequireLoginEvent(eventBus));
        subscriptions.add(subscribeForRequestLoginEvent(eventBus));
        subscriptions.add(subscribeForLoginEvent(eventBus));
        subscriptions.add(subscribeForRequestBalanceEvent(eventBus));
    }

    protected Subscription subscribeForRequireLoginEvent(final EventBus eventBus) {
        return eventBus.getEventObservable(RequireLoginEvent.class)
            .subscribe(new Action1<Event>() {
                @Override
                public void call(Event event) {
                    showFragment(LoginFragment.newInstance());
                }
            });
    }

    protected Subscription subscribeForRequestLoginEvent(final EventBus eventBus) {
        return eventBus.getEventObservable(RequestLoginEvent.class)
            .observeOn(Schedulers.io())
            .map(new Func1<Event, Event>() {
                @Override
                public Event call(Event event) {
                    try {
                        String cardNumber = ((RequestLoginEvent) event).cardNumber;
                        String password = ((RequestLoginEvent) event).password;
                        InformationRetriever.getBalance(cardNumber, password);
                        return new LoginEvent(cardNumber, password);
                    } catch (ServerUnavailableException e) {
                        return new LoginFailedEvent(LoginFailedEvent.Reasons.SERVER_UNAVAILABLE);
                    } catch (WrongCredentialsException e) {
                        return new LoginFailedEvent(LoginFailedEvent.Reasons.WRONG_CREDENTIALS);
                    }
                }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Event>() {
                @Override
                public void call(Event event) {
                    eventBus.send(event);
                }
            });
    }

    protected Subscription subscribeForLoginEvent(final EventBus eventBus) {
        return eventBus.getEventObservable(LoginEvent.class)
            .subscribe(new Action1<Event>() {
                @Override
                public void call(Event event) {
                    CredentialStore.Credentials credentials = new CredentialStore.Credentials(((LoginEvent) event).cardNumber, ((LoginEvent) event).password);
                    CredentialStore.put(MainActivity.this, credentials);
                    showFragment(BalanceFragment.newInstance());
                }
            });
    }

    protected Subscription subscribeForRequestBalanceEvent(final EventBus eventBus) {
        return eventBus.getEventObservable(RequestBalanceEvent.class)
            .observeOn(Schedulers.io())
            .map(new Func1<Event, Event>() {
                @Override
                public Event call(Event event) {
                    try {
                        CredentialStore.Credentials credentials = CredentialStore.get(MainActivity.this);
                        String balance = InformationRetriever.getBalance(credentials.cardNumber, credentials.password);
                        return new BalanceEvent(balance);
                    } catch (ServerUnavailableException e) {
                        return new LoginFailedEvent(LoginFailedEvent.Reasons.SERVER_UNAVAILABLE);
                    } catch (WrongCredentialsException e) {
                        return new LoginFailedEvent(LoginFailedEvent.Reasons.WRONG_CREDENTIALS);
                    }
                }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Event>() {
                @Override
                public void call(Event event) {
                    eventBus.send(event);
                }
            });
    }

    protected void init() {
        EventBus eventBus = Application.getEventBus();
        CredentialStore.Credentials credentials = CredentialStore.get(this);
        if (credentials == null) {
            eventBus.send(new RequireLoginEvent());
        } else {
            eventBus.send(new LoginEvent(credentials.cardNumber, credentials.password));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        subscriptions.unsubscribe();
    }
}

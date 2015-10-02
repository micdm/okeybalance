package com.micdm.okeybalance;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.micdm.okeybalance.content.InformationRetriever;
import com.micdm.okeybalance.events.BalanceEvent;
import com.micdm.okeybalance.events.EventBus;
import com.micdm.okeybalance.events.FinishBalanceRequestEvent;
import com.micdm.okeybalance.events.FinishLoginRequestEvent;
import com.micdm.okeybalance.events.LoginEvent;
import com.micdm.okeybalance.events.RequestBalanceEvent;
import com.micdm.okeybalance.events.RequestLoginEvent;
import com.micdm.okeybalance.events.RequireLoginEvent;
import com.micdm.okeybalance.events.ServerUnavailableEvent;
import com.micdm.okeybalance.events.StartBalanceRequestEvent;
import com.micdm.okeybalance.events.StartLoginRequestEvent;
import com.micdm.okeybalance.events.WrongCredentialsEvent;
import com.micdm.okeybalance.exceptions.ServerUnavailableException;
import com.micdm.okeybalance.exceptions.WrongCredentialsException;
import com.micdm.okeybalance.fragments.BalanceFragment;
import com.micdm.okeybalance.fragments.LoginFragment;
import com.micdm.okeybalance.utils.CredentialStore;

import java.math.BigDecimal;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
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
        subscriptions.add(subscribeForServerUnavailableEvent(eventBus));
        subscriptions.add(subscribeForLoginEvent(eventBus));
        subscriptions.add(subscribeForRequestBalanceEvent(eventBus));
    }

    protected Subscription subscribeForRequireLoginEvent(EventBus eventBus) {
        return eventBus.getEventObservable(RequireLoginEvent.class)
            .map(event -> ((RequireLoginEvent) event).cardNumber)
            .subscribe(cardNumber -> showFragment(LoginFragment.newInstance(cardNumber)));
    }

    protected Subscription subscribeForRequestLoginEvent(EventBus eventBus) {
        return eventBus.getEventObservable(RequestLoginEvent.class)
            .doOnNext(event -> eventBus.send(new StartLoginRequestEvent()))
            .observeOn(Schedulers.io())
            .map(event -> {
                try {
                    String cardNumber = ((RequestLoginEvent) event).cardNumber;
                    String password = ((RequestLoginEvent) event).password;
                    InformationRetriever.getBalance(cardNumber, password);
                    return new LoginEvent(cardNumber, password);
                } catch (ServerUnavailableException e) {
                    return new ServerUnavailableEvent();
                } catch (WrongCredentialsException e) {
                    return new WrongCredentialsEvent();
                }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(event -> {
                eventBus.send(new FinishLoginRequestEvent());
                eventBus.send(event);
            });
    }

    protected Subscription subscribeForServerUnavailableEvent(EventBus eventBus) {
        return eventBus.getEventObservable(ServerUnavailableEvent.class)
            .subscribe(event -> Toast.makeText(MainActivity.this, R.string.a__main__server_unavailable_error, Toast.LENGTH_SHORT).show());
    }

    protected Subscription subscribeForLoginEvent(EventBus eventBus) {
        return eventBus.getEventObservable(LoginEvent.class)
            .map(event -> {
                String cardNumber = ((LoginEvent) event).cardNumber;
                String password = ((LoginEvent) event).password;
                return new CredentialStore.Credentials(cardNumber, password);
            })
            .doOnNext(credentials -> CredentialStore.put(MainActivity.this, credentials))
            .subscribe(o -> showFragment(BalanceFragment.newInstance()));
    }

    protected Subscription subscribeForRequestBalanceEvent(EventBus eventBus) {
        return eventBus.getEventObservable(RequestBalanceEvent.class)
            .doOnNext(event -> eventBus.send(new StartBalanceRequestEvent()))
            .observeOn(Schedulers.io())
            .map(event -> {
                CredentialStore.Credentials credentials = CredentialStore.get(MainActivity.this);
                try {
                    BigDecimal balance = InformationRetriever.getBalance(credentials.cardNumber, credentials.password);
                    return new BalanceEvent(balance);
                } catch (ServerUnavailableException e) {
                    return new ServerUnavailableEvent();
                } catch (WrongCredentialsException e) {
                    CredentialStore.clear(MainActivity.this);
                    return new RequireLoginEvent(credentials.cardNumber);
                }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(event -> {
                eventBus.send(new FinishBalanceRequestEvent());
                eventBus.send(event);
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

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

    protected Subscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a__main);
        subscribeForEvents(((Application) getApplication()).getEventBus());
        init();
    }

    protected void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.a__main__fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
        ((Application) getApplication()).getAnalyticsTracker().trackScreenView(this, fragment.getClass().getSimpleName());
    }

    protected Subscription subscribeForEvents(EventBus eventBus) {
        return new CompositeSubscription(
            subscribeForRequireLoginEvent(eventBus),
            subscribeForRequestLoginEvent(eventBus),
            subscribeForServerUnavailableEvent(eventBus),
            subscribeForLoginEvent(eventBus),
            subscribeForRequestBalanceEvent(eventBus)
        );
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
            .doOnNext(event -> {
                String cardNumber = ((LoginEvent) event).cardNumber;
                String password = ((LoginEvent) event).password;
                CredentialStore.put(MainActivity.this, cardNumber, password);
            })
            .subscribe(o -> showFragment(BalanceFragment.newInstance()));
    }

    protected Subscription subscribeForRequestBalanceEvent(EventBus eventBus) {
        return eventBus.getEventObservable(RequestBalanceEvent.class)
            .doOnNext(event -> eventBus.send(new StartBalanceRequestEvent()))
            .observeOn(Schedulers.io())
            .map(event -> {
                String cardNumber = CredentialStore.getCardNumber(MainActivity.this);
                String password = CredentialStore.getPassword(MainActivity.this);
                try {
                    BigDecimal balance = InformationRetriever.getBalance(cardNumber, password);
                    return new BalanceEvent(balance);
                } catch (ServerUnavailableException e) {
                    return new ServerUnavailableEvent();
                } catch (WrongCredentialsException e) {
                    CredentialStore.clearPassword(MainActivity.this);
                    return new RequireLoginEvent(cardNumber);
                }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(event -> {
                eventBus.send(new FinishBalanceRequestEvent());
                eventBus.send(event);
            });
    }

    protected void init() {
        EventBus eventBus = ((Application) getApplication()).getEventBus();
        String cardNumber = CredentialStore.getCardNumber(this);
        if (cardNumber == null) {
            eventBus.send(new RequireLoginEvent());
        } else if (!CredentialStore.hasPassword(this)) {
            eventBus.send(new RequireLoginEvent(cardNumber));
        } else {
            String password = CredentialStore.getPassword(this);
            eventBus.send(new LoginEvent(cardNumber, password));
        }
    }

    @Override
    public void onBackPressed() {
        if (CredentialStore.hasPassword(this)) {
            CredentialStore.clearPassword(this);
            String cardNumber = CredentialStore.getCardNumber(this);
            ((Application) getApplication()).getEventBus().send(new RequireLoginEvent(cardNumber));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }
}

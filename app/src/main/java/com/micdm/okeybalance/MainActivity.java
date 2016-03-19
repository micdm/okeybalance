package com.micdm.okeybalance;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import com.micdm.okeybalance.events.RequestLogoutEvent;
import com.micdm.okeybalance.events.RequireLoginEvent;
import com.micdm.okeybalance.events.ServerUnavailableEvent;
import com.micdm.okeybalance.events.StartBalanceRequestEvent;
import com.micdm.okeybalance.events.StartLoginRequestEvent;
import com.micdm.okeybalance.events.WrongCredentialsEvent;
import com.micdm.okeybalance.exceptions.ServerUnavailableException;
import com.micdm.okeybalance.exceptions.WrongCredentialsException;
import com.micdm.okeybalance.fragments.BalanceFragment;
import com.micdm.okeybalance.fragments.LoginFragment;
import com.micdm.okeybalance.utils.BalanceStore;
import com.micdm.okeybalance.utils.CredentialStore;

import java.math.BigDecimal;

import rx.Observable;
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
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (getSupportFragmentManager().getFragments() != null) {
            transaction = transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        }
        transaction
            .replace(R.id.a__main__content, fragment)
            .addToBackStack(null)
            .commit();
        ((Application) getApplication()).getAnalyticsTracker().trackScreenView(this, fragment.getClass().getSimpleName());
    }

    protected Subscription subscribeForEvents(EventBus eventBus) {
        return new CompositeSubscription(
            subscribeForRequireLoginEvent(eventBus),
            subscribeForRequestLoginEvent(eventBus),
            subscribeForServerUnavailableEvent(eventBus),
            subscribeForLoginEvent(eventBus),
            subscribeForRequestBalanceEvent(eventBus),
            subscribeForBalanceEvent(eventBus),
            subscribeForRequestLogoutEvent(eventBus)
        );
    }

    protected Subscription subscribeForRequireLoginEvent(EventBus eventBus) {
        return eventBus.getEventObservable(RequireLoginEvent.class)
            .map(event -> event.cardNumber)
            .subscribe(cardNumber -> showFragment(LoginFragment.newInstance(cardNumber)));
    }

    protected Subscription subscribeForRequestLoginEvent(EventBus eventBus) {
        return eventBus.getEventObservable(RequestLoginEvent.class)
            .doOnNext(event -> eventBus.send(new StartLoginRequestEvent()))
            .observeOn(Schedulers.io())
            .map(event -> {
                try {
                    String cardNumber = event.cardNumber;
                    String password = event.password;
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
        Observable<LoginEvent> common = eventBus.getEventObservable(LoginEvent.class);
        return new CompositeSubscription(
            common.subscribe(event -> CredentialStore.put(MainActivity.this, event.cardNumber, event.password)),
            common.subscribe(event -> showFragment(BalanceFragment.newInstance(event.cardNumber, event.password)))
        );
    }

    protected Subscription subscribeForRequestBalanceEvent(EventBus eventBus) {
        return eventBus.getEventObservable(RequestBalanceEvent.class)
            .doOnNext(event -> eventBus.send(new StartBalanceRequestEvent()))
            .doOnNext(event -> {
                String cardNumber = BalanceStore.getCardNumber(MainActivity.this);
                if (cardNumber == null || !cardNumber.equals(event.cardNumber)) {
                    return;
                }
                BigDecimal balance = BalanceStore.getBalance(MainActivity.this);
                if (balance != null) {
                    eventBus.send(new BalanceEvent(cardNumber, balance));
                }
            })
            .observeOn(Schedulers.io())
            .map(event -> {
                try {
                    BigDecimal balance = InformationRetriever.getBalance(event.cardNumber, event.password);
                    return new BalanceEvent(event.cardNumber, balance);
                } catch (ServerUnavailableException e) {
                    return new ServerUnavailableEvent();
                } catch (WrongCredentialsException e) {
                    CredentialStore.clearPassword(MainActivity.this);
                    return new RequireLoginEvent(event.cardNumber);
                }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(event -> {
                eventBus.send(new FinishBalanceRequestEvent());
                eventBus.send(event);
            });
    }

    protected Subscription subscribeForBalanceEvent(EventBus eventBus) {
        return eventBus.getEventObservable(BalanceEvent.class)
            .subscribe(event -> BalanceStore.put(MainActivity.this, event.cardNumber, event.balance));
    }

    protected Subscription subscribeForRequestLogoutEvent(EventBus eventBus) {
        return eventBus.getEventObservable(RequestLogoutEvent.class)
            .subscribe(event -> {
                CredentialStore.clearPassword(MainActivity.this);
                BalanceStore.clear(MainActivity.this);
                String cardNumber = CredentialStore.getCardNumber(MainActivity.this);
                eventBus.send(new RequireLoginEvent(cardNumber));
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
    protected void onDestroy() {
        super.onDestroy();
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }
}

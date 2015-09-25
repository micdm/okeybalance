package com.micdm.okeybalance.events;

public class BalanceEvent implements Event {

    public final String balance;

    public BalanceEvent(String balance) {
        this.balance = balance;
    }
}

package com.micdm.okeybalance.events;

import java.math.BigDecimal;

public class LoadBalanceEvent implements Event {

    public final String balance;

    public LoadBalanceEvent(String balance) {
        this.balance = balance;
    }
}

package com.micdm.okeybalance.events;

import java.math.BigDecimal;

public class BalanceEvent implements Event {

    public final BigDecimal balance;

    public BalanceEvent(BigDecimal balance) {
        this.balance = balance;
    }
}

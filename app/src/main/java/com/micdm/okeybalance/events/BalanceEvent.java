package com.micdm.okeybalance.events;

import java.math.BigDecimal;

public class BalanceEvent implements Event {

    public final String cardNumber;
    public final BigDecimal balance;

    public BalanceEvent(String cardNumber, BigDecimal balance) {
        this.cardNumber = cardNumber;
        this.balance = balance;
    }
}

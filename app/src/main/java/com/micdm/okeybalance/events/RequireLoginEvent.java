package com.micdm.okeybalance.events;

public class RequireLoginEvent implements Event {

    public final String cardNumber;

    public RequireLoginEvent() {
        this(null);
    }

    public RequireLoginEvent(String cardNumber) {
        this.cardNumber = cardNumber;
    }
}

package com.micdm.okeybalance.events;

public class RequestLoginEvent implements Event {

    public final String cardNumber;
    public final String password;

    public RequestLoginEvent(String cardNumber, String password) {
        this.cardNumber = cardNumber;
        this.password = password;
    }
}

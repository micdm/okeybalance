package com.micdm.okeybalance.events;

public class RequestBalanceEvent implements Event {

    public final String cardNumber;
    public final String password;

    public RequestBalanceEvent(String cardNumber, String password) {
        this.cardNumber = cardNumber;
        this.password = password;
    }
}

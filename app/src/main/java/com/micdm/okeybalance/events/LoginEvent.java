package com.micdm.okeybalance.events;

public class LoginEvent implements Event {

    public final String cardNumber;
    public final String password;

    public LoginEvent(String cardNumber, String password) {
        this.cardNumber = cardNumber;
        this.password = password;
    }
}

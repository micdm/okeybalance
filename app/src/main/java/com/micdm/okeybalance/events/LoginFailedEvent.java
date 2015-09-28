package com.micdm.okeybalance.events;

public class LoginFailedEvent implements Event {

    public enum Reasons {
        SERVER_UNAVAILABLE,
        WRONG_CREDENTIALS,
    }

    public final Reasons reason;

    public LoginFailedEvent(Reasons reason) {
        this.reason = reason;
    }
}

package com.micdm.okeybalance.exceptions;

public class ServerUnavailableException extends RuntimeException {

    public ServerUnavailableException() {

    }

    public ServerUnavailableException(Throwable throwable) {
        super(throwable);
    }
}

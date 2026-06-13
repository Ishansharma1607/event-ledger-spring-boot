package com.mphasis.eventledger.gateway.client;

public class AccountClientException extends RuntimeException {

    public AccountClientException(String message) {
        super(message);
    }

    public AccountClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

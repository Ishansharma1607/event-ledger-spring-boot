package com.mphasis.eventledger.account.service;

public class AccountIdMismatchException extends RuntimeException {

    public AccountIdMismatchException(String pathAccountId, String bodyAccountId) {
        super("Path accountId '%s' does not match request accountId '%s'".formatted(pathAccountId, bodyAccountId));
    }
}

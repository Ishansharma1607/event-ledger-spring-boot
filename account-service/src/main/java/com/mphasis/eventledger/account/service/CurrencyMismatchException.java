package com.mphasis.eventledger.account.service;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String accountId, String accountCurrency, String transactionCurrency) {
        super("Account '%s' uses currency '%s' but transaction uses '%s'"
                .formatted(accountId, accountCurrency, transactionCurrency));
    }
}

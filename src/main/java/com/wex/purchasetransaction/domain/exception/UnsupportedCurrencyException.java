package com.wex.purchasetransaction.domain.exception;

public class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String currencyCode) {
        super("Unsupported or unknown ISO 4217 currency code: " + currencyCode);
    }
}

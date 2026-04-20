package com.wex.purchasetransaction.infrastructure.external.treasury;

class NoExchangeRateFoundException extends RuntimeException {
    NoExchangeRateFoundException(String message) {
        super(message);
    }
}

package com.wex.purchasetransaction.domain.exception;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(UUID id) {
        super("Purchase transaction not found with id: " + id);
    }
}

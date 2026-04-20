package com.wex.purchasetransaction.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record StorePurchaseTransactionCommand(
        String description,
        LocalDate transactionDate,
        BigDecimal amountUsd) {
}

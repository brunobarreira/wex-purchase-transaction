package com.wex.purchasetransaction.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PurchaseTransactionResult(
        UUID id,
        String description,
        LocalDate transactionDate,
        BigDecimal amountUsd) {
}

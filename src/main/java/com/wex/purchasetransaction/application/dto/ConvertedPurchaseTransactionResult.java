package com.wex.purchasetransaction.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ConvertedPurchaseTransactionResult(
        UUID id,
        String description,
        LocalDate transactionDate,
        BigDecimal amountUsd,
        BigDecimal exchangeRate,
        BigDecimal convertedAmount,
        String currencyCode,
        String currencyName) {
}

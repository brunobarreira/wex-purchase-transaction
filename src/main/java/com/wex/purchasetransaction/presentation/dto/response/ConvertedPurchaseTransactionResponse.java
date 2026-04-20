package com.wex.purchasetransaction.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Schema(description = "Purchase transaction converted to the target currency")
@Builder
public record ConvertedPurchaseTransactionResponse(

        @Schema(description = "Unique identifier of the transaction")
        UUID id,

        @Schema(description = "Transaction description")
        String description,

        @Schema(description = "Date the transaction occurred")
        LocalDate transactionDate,

        @Schema(description = "Original purchase amount in US dollars")
        BigDecimal amountUsd,

        @Schema(description = "Exchange rate used for conversion")
        BigDecimal exchangeRate,

        @Schema(description = "Converted amount in the target currency, rounded to 2 decimal places")
        BigDecimal convertedAmount,

        @Schema(description = "ISO 4217 currency code of the target currency", example = "BRL")
        String currencyCode,

        @Schema(description = "Treasury API currency name", example = "Real")
        String currencyName) {
}

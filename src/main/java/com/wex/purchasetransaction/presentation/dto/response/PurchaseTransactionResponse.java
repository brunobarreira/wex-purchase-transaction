package com.wex.purchasetransaction.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Schema(description = "Stored purchase transaction")
@Builder
public record PurchaseTransactionResponse(

        @Schema(description = "Unique identifier of the transaction")
        UUID id,

        @Schema(description = "Transaction description")
        String description,

        @Schema(description = "Date the transaction occurred")
        LocalDate transactionDate,

        @Schema(description = "Purchase amount in US dollars, rounded to nearest cent")
        BigDecimal amountUsd) {
}

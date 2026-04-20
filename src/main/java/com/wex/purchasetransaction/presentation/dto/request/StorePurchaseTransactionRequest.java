package com.wex.purchasetransaction.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request payload to store a purchase transaction")
public record StorePurchaseTransactionRequest(

        @Schema(description = "Transaction description (max 50 characters)", example = "Hotel stay in NYC")
        @NotBlank(message = "Description must not be blank")
        @Size(max = 50, message = "Description must not exceed 50 characters")
        String description,

        @Schema(description = "Date the transaction occurred (ISO 8601 format)", example = "2024-03-15")
        @NotNull(message = "Transaction date must not be null")
        @PastOrPresent(message = "Transaction date must be in the past or present")
        LocalDate transactionDate,

        @Schema(description = "Purchase amount in US dollars (positive value)", example = "123.45")
        @NotNull(message = "Amount must not be null")
        @DecimalMin(value = "0.01", message = "Amount must be a positive value")
        BigDecimal amountUsd) {
}

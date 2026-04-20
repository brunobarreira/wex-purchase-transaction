package com.wex.purchasetransaction.domain.entity;

import com.wex.purchasetransaction.domain.valueobject.Money;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(of = "id")
@Builder
public class PurchaseTransaction {

    private static final int MAX_DESCRIPTION_LENGTH = 50;

    private final UUID id;
    private final String description;
    private final LocalDate transactionDate;
    private final Money amount;

    public static PurchaseTransaction create(
            String description,
            LocalDate transactionDate,
            Money amount) {
        validateDescription(description);
        validateTransactionDate(transactionDate);
        return PurchaseTransaction.builder()
                .id(UUID.randomUUID())
                .description(description)
                .transactionDate(transactionDate)
                .amount(amount)
                .build();
    }

    public static PurchaseTransaction reconstitute(
            UUID id,
            String description,
            LocalDate transactionDate,
            Money amount) {
        return PurchaseTransaction.builder()
                .id(id)
                .description(description)
                .transactionDate(transactionDate)
                .amount(amount)
                .build();
    }

    private static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be blank");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
    }

    private static void validateTransactionDate(LocalDate transactionDate) {
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date must not be null");
        }
        if (transactionDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Transaction date must not be in the future");
        }
    }
}

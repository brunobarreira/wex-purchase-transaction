package com.wex.purchasetransaction.unit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wex.purchasetransaction.application.dto.PurchaseTransactionResult;
import com.wex.purchasetransaction.application.dto.StorePurchaseTransactionCommand;
import com.wex.purchasetransaction.application.service.StorePurchaseTransactionService;
import com.wex.purchasetransaction.domain.entity.PurchaseTransaction;
import com.wex.purchasetransaction.domain.repository.PurchaseTransactionRepository;
import com.wex.purchasetransaction.domain.valueobject.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorePurchaseTransactionServiceTest {

    @Mock
    private PurchaseTransactionRepository repository;

    @InjectMocks
    private StorePurchaseTransactionService service;

    @Test
    void store_shouldSaveAndReturnResult() {
        StorePurchaseTransactionCommand command = StorePurchaseTransactionCommand.builder()
                .description("Hotel stay")
                .transactionDate(LocalDate.of(2024, 3, 15))
                .amountUsd(new BigDecimal("123.45"))
                .build();

        PurchaseTransaction saved = PurchaseTransaction.reconstitute(
                UUID.randomUUID(), "Hotel stay", LocalDate.of(2024, 3, 15),
                Money.of(new BigDecimal("123.45")));

        when(repository.save(any(PurchaseTransaction.class))).thenReturn(saved);

        PurchaseTransactionResult result = service.store(command);

        assertThat(result.id()).isEqualTo(saved.getId());
        assertThat(result.description()).isEqualTo("Hotel stay");
        assertThat(result.transactionDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(result.amountUsd()).isEqualByComparingTo("123.45");
        verify(repository).save(any(PurchaseTransaction.class));
    }

    @Test
    void store_shouldRoundAmountBeforePersisting() {
        StorePurchaseTransactionCommand command = StorePurchaseTransactionCommand.builder()
                .description("Rounded")
                .transactionDate(LocalDate.now())
                .amountUsd(new BigDecimal("10.555"))
                .build();

        PurchaseTransaction saved = PurchaseTransaction.reconstitute(
                UUID.randomUUID(), "Rounded", LocalDate.now(), Money.of(new BigDecimal("10.56")));

        when(repository.save(any(PurchaseTransaction.class))).thenReturn(saved);

        PurchaseTransactionResult result = service.store(command);

        assertThat(result.amountUsd()).isEqualByComparingTo("10.56");
    }
}

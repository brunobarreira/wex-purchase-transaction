package com.wex.purchasetransaction.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.wex.purchasetransaction.domain.entity.PurchaseTransaction;
import com.wex.purchasetransaction.domain.repository.PurchaseTransactionRepository;
import com.wex.purchasetransaction.domain.valueobject.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(com.wex.purchasetransaction.infrastructure.persistence.PurchaseTransactionRepositoryAdapter.class)
class PurchaseTransactionRepositoryAdapterIT {

    @Autowired
    private PurchaseTransactionRepository repository;

    @Test
    void save_shouldPersistAndRetrieveTransaction() {
        PurchaseTransaction transaction = PurchaseTransaction.create(
                "Business dinner",
                LocalDate.of(2024, 6, 10),
                Money.of(new BigDecimal("250.75")));

        PurchaseTransaction saved = repository.save(transaction);
        Optional<PurchaseTransaction> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Business dinner");
        assertThat(found.get().getTransactionDate()).isEqualTo(LocalDate.of(2024, 6, 10));
        assertThat(found.get().getAmount().value()).isEqualByComparingTo("250.75");
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        Optional<PurchaseTransaction> result = repository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void save_shouldAssignUniqueIds() {
        PurchaseTransaction t1 = PurchaseTransaction.create("Item 1", LocalDate.now(), Money.of(BigDecimal.ONE));
        PurchaseTransaction t2 = PurchaseTransaction.create("Item 2", LocalDate.now(), Money.of(BigDecimal.TEN));
        PurchaseTransaction s1 = repository.save(t1);
        PurchaseTransaction s2 = repository.save(t2);
        assertThat(s1.getId()).isNotEqualTo(s2.getId());
    }
}

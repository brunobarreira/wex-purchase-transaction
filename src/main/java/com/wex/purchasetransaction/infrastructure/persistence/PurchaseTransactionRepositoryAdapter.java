package com.wex.purchasetransaction.infrastructure.persistence;

import com.wex.purchasetransaction.domain.entity.PurchaseTransaction;
import com.wex.purchasetransaction.domain.repository.PurchaseTransactionRepository;
import com.wex.purchasetransaction.domain.valueobject.Money;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseTransactionRepositoryAdapter implements PurchaseTransactionRepository {

    private final PurchaseTransactionJpaRepository jpaRepository;

    @Override
    public PurchaseTransaction save(PurchaseTransaction transaction) {
        PurchaseTransactionJpaEntity entity = toEntity(transaction);
        PurchaseTransactionJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PurchaseTransaction> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private PurchaseTransactionJpaEntity toEntity(PurchaseTransaction transaction) {
        return PurchaseTransactionJpaEntity.builder()
                .id(transaction.getId())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .amountUsd(transaction.getAmount().value())
                .build();
    }

    private PurchaseTransaction toDomain(PurchaseTransactionJpaEntity entity) {
        return PurchaseTransaction.reconstitute(
                entity.getId(),
                entity.getDescription(),
                entity.getTransactionDate(),
                Money.of(entity.getAmountUsd()));
    }
}

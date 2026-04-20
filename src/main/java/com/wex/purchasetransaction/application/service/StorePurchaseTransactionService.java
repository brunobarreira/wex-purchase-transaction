package com.wex.purchasetransaction.application.service;

import com.wex.purchasetransaction.application.dto.PurchaseTransactionResult;
import com.wex.purchasetransaction.application.dto.StorePurchaseTransactionCommand;
import com.wex.purchasetransaction.application.port.in.StorePurchaseTransactionUseCase;
import com.wex.purchasetransaction.domain.entity.PurchaseTransaction;
import com.wex.purchasetransaction.domain.repository.PurchaseTransactionRepository;
import com.wex.purchasetransaction.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StorePurchaseTransactionService implements StorePurchaseTransactionUseCase {

    private final PurchaseTransactionRepository repository;

    @Override
    @Transactional
    public PurchaseTransactionResult store(StorePurchaseTransactionCommand command) {
        Money amount = Money.of(command.amountUsd());
        PurchaseTransaction transaction = PurchaseTransaction.create(
                command.description(),
                command.transactionDate(),
                amount);
        PurchaseTransaction saved = repository.save(transaction);
        return toResult(saved);
    }

    private PurchaseTransactionResult toResult(PurchaseTransaction transaction) {
        return PurchaseTransactionResult.builder()
                .id(transaction.getId())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .amountUsd(transaction.getAmount().value())
                .build();
    }
}

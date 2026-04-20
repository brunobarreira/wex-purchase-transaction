package com.wex.purchasetransaction.domain.repository;

import com.wex.purchasetransaction.domain.entity.PurchaseTransaction;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseTransactionRepository {

    PurchaseTransaction save(PurchaseTransaction transaction);

    Optional<PurchaseTransaction> findById(UUID id);
}

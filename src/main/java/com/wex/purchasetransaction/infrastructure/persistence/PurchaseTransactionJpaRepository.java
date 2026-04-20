package com.wex.purchasetransaction.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PurchaseTransactionJpaRepository extends JpaRepository<PurchaseTransactionJpaEntity, UUID> {
}

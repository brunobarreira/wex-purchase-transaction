package com.wex.purchasetransaction.application.port.in;

import com.wex.purchasetransaction.application.dto.ConvertedPurchaseTransactionResult;
import java.util.UUID;

public interface RetrievePurchaseTransactionUseCase {

    ConvertedPurchaseTransactionResult retrieve(UUID id, String currencyCode);
}

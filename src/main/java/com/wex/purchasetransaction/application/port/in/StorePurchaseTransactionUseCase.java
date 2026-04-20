package com.wex.purchasetransaction.application.port.in;

import com.wex.purchasetransaction.application.dto.StorePurchaseTransactionCommand;
import com.wex.purchasetransaction.application.dto.PurchaseTransactionResult;

public interface StorePurchaseTransactionUseCase {

    PurchaseTransactionResult store(StorePurchaseTransactionCommand command);
}

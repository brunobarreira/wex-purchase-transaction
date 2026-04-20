package com.wex.purchasetransaction.presentation.mapper;

import com.wex.purchasetransaction.application.dto.ConvertedPurchaseTransactionResult;
import com.wex.purchasetransaction.application.dto.PurchaseTransactionResult;
import com.wex.purchasetransaction.application.dto.StorePurchaseTransactionCommand;
import com.wex.purchasetransaction.presentation.dto.request.StorePurchaseTransactionRequest;
import com.wex.purchasetransaction.presentation.dto.response.ConvertedPurchaseTransactionResponse;
import com.wex.purchasetransaction.presentation.dto.response.PurchaseTransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class PurchaseTransactionMapper {

    public StorePurchaseTransactionCommand toCommand(StorePurchaseTransactionRequest request) {
        return StorePurchaseTransactionCommand.builder()
                .description(request.description())
                .transactionDate(request.transactionDate())
                .amountUsd(request.amountUsd())
                .build();
    }

    public PurchaseTransactionResponse toResponse(PurchaseTransactionResult result) {
        return PurchaseTransactionResponse.builder()
                .id(result.id())
                .description(result.description())
                .transactionDate(result.transactionDate())
                .amountUsd(result.amountUsd())
                .build();
    }

    public ConvertedPurchaseTransactionResponse toConvertedResponse(
            ConvertedPurchaseTransactionResult result) {
        return ConvertedPurchaseTransactionResponse.builder()
                .id(result.id())
                .description(result.description())
                .transactionDate(result.transactionDate())
                .amountUsd(result.amountUsd())
                .exchangeRate(result.exchangeRate())
                .convertedAmount(result.convertedAmount())
                .currencyCode(result.currencyCode())
                .currencyName(result.currencyName())
                .build();
    }
}

package com.wex.purchasetransaction.application.service;

import com.wex.purchasetransaction.application.dto.ConvertedPurchaseTransactionResult;
import com.wex.purchasetransaction.application.port.in.RetrievePurchaseTransactionUseCase;
import com.wex.purchasetransaction.application.port.out.ExchangeRatePort;
import com.wex.purchasetransaction.domain.entity.PurchaseTransaction;
import com.wex.purchasetransaction.domain.exception.CurrencyConversionException;
import com.wex.purchasetransaction.domain.exception.TransactionNotFoundException;
import com.wex.purchasetransaction.domain.exception.UnsupportedCurrencyException;
import com.wex.purchasetransaction.domain.repository.PurchaseTransactionRepository;
import com.wex.purchasetransaction.domain.valueobject.Money;
import com.wex.purchasetransaction.infrastructure.external.treasury.CurrencyMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RetrievePurchaseTransactionService implements RetrievePurchaseTransactionUseCase {

    private final PurchaseTransactionRepository repository;
    private final ExchangeRatePort exchangeRatePort;
    private final CurrencyMapper currencyMapper;

    @Override
    @Transactional(readOnly = true)
    public ConvertedPurchaseTransactionResult retrieve(UUID id, String currencyCode) {
        String treasuryName = currencyMapper.toTreasuryName(currencyCode)
                .orElseThrow(() -> new UnsupportedCurrencyException(currencyCode));

        PurchaseTransaction transaction = repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        ExchangeRatePort.ExchangeRateResult rateResult =
                exchangeRatePort.findRate(treasuryName, transaction.getTransactionDate())
                        .orElseThrow(() -> new CurrencyConversionException(
                                "The purchase cannot be converted to the target currency. "
                                        + "No exchange rate found for currency '"
                                        + currencyCode
                                        + "' within 6 months of the transaction date "
                                        + transaction.getTransactionDate()));

        Money convertedAmount = transaction.getAmount().multiply(rateResult.rate());

        return ConvertedPurchaseTransactionResult.builder()
                .id(transaction.getId())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .amountUsd(transaction.getAmount().value())
                .exchangeRate(rateResult.rate())
                .convertedAmount(convertedAmount.value())
                .currencyCode(currencyCode.toUpperCase())
                .currencyName(rateResult.currencyName())
                .build();
    }
}

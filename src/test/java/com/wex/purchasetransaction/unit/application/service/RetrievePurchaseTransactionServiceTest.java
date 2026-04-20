package com.wex.purchasetransaction.unit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.wex.purchasetransaction.application.dto.ConvertedPurchaseTransactionResult;
import com.wex.purchasetransaction.application.port.out.ExchangeRatePort;
import com.wex.purchasetransaction.application.service.RetrievePurchaseTransactionService;
import com.wex.purchasetransaction.domain.entity.PurchaseTransaction;
import com.wex.purchasetransaction.domain.exception.CurrencyConversionException;
import com.wex.purchasetransaction.domain.exception.TransactionNotFoundException;
import com.wex.purchasetransaction.domain.exception.UnsupportedCurrencyException;
import com.wex.purchasetransaction.domain.repository.PurchaseTransactionRepository;
import com.wex.purchasetransaction.domain.valueobject.Money;
import com.wex.purchasetransaction.infrastructure.external.treasury.CurrencyMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetrievePurchaseTransactionServiceTest {

    @Mock
    private PurchaseTransactionRepository repository;

    @Mock
    private ExchangeRatePort exchangeRatePort;

    @Mock
    private CurrencyMapper currencyMapper;

    @InjectMocks
    private RetrievePurchaseTransactionService service;

    private static final UUID TRANSACTION_ID = UUID.randomUUID();
    private static final LocalDate PURCHASE_DATE = LocalDate.of(2024, 3, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal RATE = new BigDecimal("4.97");

    @Test
    void retrieve_shouldReturnConvertedTransaction() {
        PurchaseTransaction transaction = PurchaseTransaction.reconstitute(
                TRANSACTION_ID, "Hotel stay", PURCHASE_DATE, Money.of(AMOUNT));

        when(currencyMapper.toTreasuryName("BRL")).thenReturn(Optional.of("Brazil-Real"));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
        when(exchangeRatePort.findRate("Brazil-Real", PURCHASE_DATE))
                .thenReturn(Optional.of(new ExchangeRatePort.ExchangeRateResult(
                        RATE, LocalDate.of(2024, 3, 1), "Real")));

        ConvertedPurchaseTransactionResult result = service.retrieve(TRANSACTION_ID, "BRL");

        assertThat(result.id()).isEqualTo(TRANSACTION_ID);
        assertThat(result.amountUsd()).isEqualByComparingTo("100.00");
        assertThat(result.exchangeRate()).isEqualByComparingTo("4.97");
        assertThat(result.convertedAmount()).isEqualByComparingTo("497.00");
        assertThat(result.currencyCode()).isEqualTo("BRL");
        assertThat(result.currencyName()).isEqualTo("Real");
    }

    @Test
    void retrieve_shouldNormalizeCurrencyCodeToUppercase() {
        PurchaseTransaction transaction = PurchaseTransaction.reconstitute(
                TRANSACTION_ID, "Coffee", PURCHASE_DATE, Money.of(new BigDecimal("5.00")));

        when(currencyMapper.toTreasuryName("brl")).thenReturn(Optional.of("Brazil-Real"));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
        when(exchangeRatePort.findRate("Brazil-Real", PURCHASE_DATE))
                .thenReturn(Optional.of(new ExchangeRatePort.ExchangeRateResult(
                        RATE, PURCHASE_DATE, "Real")));

        ConvertedPurchaseTransactionResult result = service.retrieve(TRANSACTION_ID, "brl");

        assertThat(result.currencyCode()).isEqualTo("BRL");
    }

    @Test
    void retrieve_shouldThrowWhenTransactionNotFound() {
        when(currencyMapper.toTreasuryName("BRL")).thenReturn(Optional.of("Brazil-Real"));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retrieve(TRANSACTION_ID, "BRL"))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void retrieve_shouldThrowWhenCurrencyCodeUnsupported() {
        when(currencyMapper.toTreasuryName("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retrieve(TRANSACTION_ID, "XYZ"))
                .isInstanceOf(UnsupportedCurrencyException.class)
                .hasMessageContaining("XYZ");
    }

    @Test
    void retrieve_shouldThrowWhenNoExchangeRateWithin6Months() {
        PurchaseTransaction transaction = PurchaseTransaction.reconstitute(
                TRANSACTION_ID, "Hotel stay", PURCHASE_DATE, Money.of(AMOUNT));

        when(currencyMapper.toTreasuryName("BRL")).thenReturn(Optional.of("Brazil-Real"));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
        when(exchangeRatePort.findRate("Brazil-Real", PURCHASE_DATE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retrieve(TRANSACTION_ID, "BRL"))
                .isInstanceOf(CurrencyConversionException.class)
                .hasMessageContaining("cannot be converted");
    }

    @Test
    void retrieve_shouldRoundConvertedAmountToTwoDecimalPlaces() {
        PurchaseTransaction transaction = PurchaseTransaction.reconstitute(
                TRANSACTION_ID, "Snack", PURCHASE_DATE, Money.of(new BigDecimal("10.00")));
        BigDecimal oddRate = new BigDecimal("1.234");

        when(currencyMapper.toTreasuryName("EUR")).thenReturn(Optional.of("Euro Zone-Euro"));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
        when(exchangeRatePort.findRate("Euro Zone-Euro", PURCHASE_DATE))
                .thenReturn(Optional.of(new ExchangeRatePort.ExchangeRateResult(
                        oddRate, PURCHASE_DATE, "Euro")));

        ConvertedPurchaseTransactionResult result = service.retrieve(TRANSACTION_ID, "EUR");

        assertThat(result.convertedAmount()).isEqualByComparingTo("12.34");
    }

    @Test
    void retrieve_shouldReturnRate_whenRecordDateEqualsExactPurchaseDate() {
        PurchaseTransaction transaction = PurchaseTransaction.reconstitute(
                TRANSACTION_ID, "Same day rate", PURCHASE_DATE, Money.of(AMOUNT));

        when(currencyMapper.toTreasuryName("BRL")).thenReturn(Optional.of("Brazil-Real"));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
        when(exchangeRatePort.findRate("Brazil-Real", PURCHASE_DATE))
                .thenReturn(Optional.of(new ExchangeRatePort.ExchangeRateResult(
                        RATE, PURCHASE_DATE, "Real")));

        ConvertedPurchaseTransactionResult result = service.retrieve(TRANSACTION_ID, "BRL");

        assertThat(result.exchangeRate()).isEqualByComparingTo(RATE);
    }

    @Test
    void retrieve_shouldReturnRate_whenRecordDateIsExactly6MonthsBeforePurchase() {
        LocalDate purchaseDate = LocalDate.of(2024, 9, 15);
        LocalDate sixMonthsAgo = purchaseDate.minusMonths(6);
        PurchaseTransaction transaction = PurchaseTransaction.reconstitute(
                TRANSACTION_ID, "6 month boundary", purchaseDate, Money.of(AMOUNT));

        when(currencyMapper.toTreasuryName("BRL")).thenReturn(Optional.of("Brazil-Real"));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
        when(exchangeRatePort.findRate("Brazil-Real", purchaseDate))
                .thenReturn(Optional.of(new ExchangeRatePort.ExchangeRateResult(
                        RATE, sixMonthsAgo, "Real")));

        ConvertedPurchaseTransactionResult result = service.retrieve(TRANSACTION_ID, "BRL");

        assertThat(result.exchangeRate()).isEqualByComparingTo(RATE);
    }

    @Test
    void retrieve_shouldHandleHighPrecisionExchangeRate() {
        BigDecimal highPrecisionRate = new BigDecimal("5.123456789");
        PurchaseTransaction transaction = PurchaseTransaction.reconstitute(
                TRANSACTION_ID, "High precision", PURCHASE_DATE, Money.of(AMOUNT));

        when(currencyMapper.toTreasuryName("BRL")).thenReturn(Optional.of("Brazil-Real"));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
        when(exchangeRatePort.findRate("Brazil-Real", PURCHASE_DATE))
                .thenReturn(Optional.of(new ExchangeRatePort.ExchangeRateResult(
                        highPrecisionRate, PURCHASE_DATE, "Real")));

        ConvertedPurchaseTransactionResult result = service.retrieve(TRANSACTION_ID, "BRL");

        // 100.00 * 5.123456789 = 512.35 (rounded HALF_UP)
        assertThat(result.convertedAmount()).isEqualByComparingTo("512.35");
    }

    @Test
    void retrieve_shouldHandleVerySmallAmountWithHighRate() {
        BigDecimal smallAmount = new BigDecimal("0.01");
        BigDecimal highRate = new BigDecimal("1500.00");
        PurchaseTransaction transaction = PurchaseTransaction.reconstitute(
                TRANSACTION_ID, "Small amount", PURCHASE_DATE, Money.of(smallAmount));

        when(currencyMapper.toTreasuryName("JPY")).thenReturn(Optional.of("Japan-Yen"));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
        when(exchangeRatePort.findRate("Japan-Yen", PURCHASE_DATE))
                .thenReturn(Optional.of(new ExchangeRatePort.ExchangeRateResult(
                        highRate, PURCHASE_DATE, "Yen")));

        ConvertedPurchaseTransactionResult result = service.retrieve(TRANSACTION_ID, "JPY");

        // 0.01 * 1500 = 15.00
        assertThat(result.convertedAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    void retrieve_shouldHandleMixedCaseCurrencyCode() {
        PurchaseTransaction transaction = PurchaseTransaction.reconstitute(
                TRANSACTION_ID, "Mixed case", PURCHASE_DATE, Money.of(AMOUNT));

        when(currencyMapper.toTreasuryName("Brl")).thenReturn(Optional.of("Brazil-Real"));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
        when(exchangeRatePort.findRate("Brazil-Real", PURCHASE_DATE))
                .thenReturn(Optional.of(new ExchangeRatePort.ExchangeRateResult(
                        RATE, PURCHASE_DATE, "Real")));

        ConvertedPurchaseTransactionResult result = service.retrieve(TRANSACTION_ID, "Brl");

        assertThat(result.currencyCode()).isEqualTo("BRL");
    }
}

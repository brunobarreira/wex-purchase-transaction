package com.wex.purchasetransaction.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRatePort {

    Optional<ExchangeRateResult> findRate(String treasuryCurrencyName, LocalDate purchaseDate);

    record ExchangeRateResult(BigDecimal rate, LocalDate recordDate, String currencyName) {
    }
}

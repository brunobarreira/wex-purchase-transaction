package com.wex.purchasetransaction.infrastructure.external.treasury;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TreasuryApiResponse(List<ExchangeRateData> data) {

    public record ExchangeRateData(
            @JsonProperty("country_currency_desc") String countryCurrentDesc,
            @JsonProperty("exchange_rate") BigDecimal exchangeRate,
            @JsonProperty("record_date") LocalDate recordDate) {
    }
}

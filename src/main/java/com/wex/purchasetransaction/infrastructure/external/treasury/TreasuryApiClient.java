package com.wex.purchasetransaction.infrastructure.external.treasury;

import com.wex.purchasetransaction.application.port.out.ExchangeRatePort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class TreasuryApiClient implements ExchangeRatePort {

    private static final int LOOKBACK_MONTHS = 6;
    private static final String FIELDS = "country_currency_desc,exchange_rate,record_date";

    private final WebClient treasuryWebClient;

    @Override
    @Cacheable(value = "exchangeRates", key = "#treasuryCurrencyName + '_' + #purchaseDate")
    public Optional<ExchangeRateResult> findRate(String treasuryCurrencyName, LocalDate purchaseDate) {
        LocalDate sixMonthsAgo = purchaseDate.minusMonths(LOOKBACK_MONTHS);
        String filter = buildFilter(treasuryCurrencyName, sixMonthsAgo, purchaseDate);

        TreasuryApiResponse response = treasuryWebClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder
                            .queryParam("fields", FIELDS)
                            .queryParam("filter", filter)
                            .queryParam("sort", "-record_date")
                            .queryParam("page[size]", "1")
                            .build();
                    log.info("Treasury API --> GET {}", uri);
                    return uri;
                })
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, clientResponse -> {
                    log.warn("Treasury API <-- 404 NOT_FOUND (no data) filter='{}'", filter);
                    return clientResponse.releaseBody().then(Mono.error(
                            new NoExchangeRateFoundException("No exchange rate data for: " + filter)));
                })
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Treasury API <-- {} body: {}",
                                            clientResponse.statusCode(), body);
                                    return Mono.error(WebClientResponseException.create(
                                            clientResponse.statusCode().value(),
                                            clientResponse.statusCode().toString(),
                                            clientResponse.headers().asHttpHeaders(),
                                            body.getBytes(StandardCharsets.UTF_8),
                                            StandardCharsets.UTF_8));
                                })
                )
                .bodyToMono(TreasuryApiResponse.class)
                .doOnNext(r -> log.info("Treasury API <-- 200 OK body: {}", r))
                .onErrorResume(NoExchangeRateFoundException.class, e -> Mono.empty())
                .block();

        if (response == null) {
            return Optional.empty();
        }

        List<TreasuryApiResponse.ExchangeRateData> data = response.data();
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }

        TreasuryApiResponse.ExchangeRateData first = data.get(0);

        if (first.exchangeRate() == null || first.exchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Treasury API returned invalid exchange rate '{}' for filter: '{}'",
                    first.exchangeRate(), filter);
            return Optional.empty();
        }

        String currencyName = extractCurrencyName(first.countryCurrentDesc());
        return Optional.of(new ExchangeRateResult(first.exchangeRate(), first.recordDate(), currencyName));
    }

    private String buildFilter(String countryCurrentDesc, LocalDate from, LocalDate to) {
        return String.format(
                "country_currency_desc:eq:%s,record_date:gte:%s,record_date:lte:%s",
                countryCurrentDesc, from, to);
    }

    private String extractCurrencyName(String countryCurrentDesc) {
        if (countryCurrentDesc == null) {
            return "";
        }
        int dashIndex = countryCurrentDesc.indexOf('-');
        return dashIndex >= 0 ? countryCurrentDesc.substring(dashIndex + 1) : countryCurrentDesc;
    }
}

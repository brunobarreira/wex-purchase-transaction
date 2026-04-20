package com.wex.purchasetransaction.integration.external;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.wex.purchasetransaction.application.port.out.ExchangeRatePort;
import com.wex.purchasetransaction.infrastructure.external.treasury.TreasuryApiClient;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@WireMockTest(httpPort = 8089)
class TreasuryApiClientIT {

    @Autowired
    private TreasuryApiClient treasuryApiClient;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setup() {
        WireMock.reset();
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    void findRate_shouldReturnExchangeRate_whenDataAvailable() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Brazil-Real",
                                      "exchange_rate": "4.9700",
                                      "record_date": "2024-03-01"
                                    }
                                  ]
                                }
                                """)));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2024, 3, 15));

        assertThat(result).isPresent();
        assertThat(result.get().rate()).isEqualByComparingTo("4.97");
        assertThat(result.get().currencyName()).isEqualTo("Real");
        assertThat(result.get().recordDate()).isEqualTo(LocalDate.of(2024, 3, 1));
    }

    @Test
    void findRate_shouldExtractCurrencyNameFromCountryCurrentDesc() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Euro Zone-Euro",
                                      "exchange_rate": "0.9200",
                                      "record_date": "2024-03-01"
                                    }
                                  ]
                                }
                                """)));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Euro Zone-Euro", LocalDate.of(2024, 4, 1));

        assertThat(result).isPresent();
        assertThat(result.get().currencyName()).isEqualTo("Euro");
    }

    @Test
    void findRate_shouldReturnEmpty_whenNoDataAvailable() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                { "data": [] }
                                """)));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2020, 1, 1));

        assertThat(result).isEmpty();
    }

    @Test
    void findRate_shouldReturnEmpty_when404() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody("<html><body>Not Found</body></html>")));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2019, 6, 1));

        assertThat(result).isEmpty();
    }

    @Test
    void findRate_shouldReturnEmpty_whenExchangeRateIsZero() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Brazil-Real",
                                      "exchange_rate": "0.00",
                                      "record_date": "2024-03-01"
                                    }
                                  ]
                                }
                                """)));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2024, 6, 1));

        assertThat(result).isEmpty();
    }

    @Test
    void findRate_shouldReturnEmpty_whenExchangeRateIsNull() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Brazil-Real",
                                      "exchange_rate": null,
                                      "record_date": "2024-03-01"
                                    }
                                  ]
                                }
                                """)));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2024, 9, 1));

        assertThat(result).isEmpty();
    }

    @Test
    void findRate_shouldReturnEmpty_when500ServerError() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"error": "Internal Server Error"}
                                """)));

        assertThatThrownBy(() -> treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2024, 3, 15)))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void findRate_shouldReturnEmpty_when503ServiceUnavailable() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"error": "Service Unavailable"}
                                """)));

        assertThatThrownBy(() -> treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2024, 3, 15)))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void findRate_shouldHandleExchangeRateWithManyDecimalPlaces() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Brazil-Real",
                                      "exchange_rate": "5.123456789",
                                      "record_date": "2024-03-01"
                                    }
                                  ]
                                }
                                """)));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2024, 3, 15));

        assertThat(result).isPresent();
        assertThat(result.get().rate()).isEqualByComparingTo("5.123456789");
    }

    @Test
    void findRate_shouldReturnRate_whenRecordDateEqualsExactPurchaseDate() {
        LocalDate purchaseDate = LocalDate.of(2024, 3, 15);
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Brazil-Real",
                                      "exchange_rate": "4.97",
                                      "record_date": "2024-03-15"
                                    }
                                  ]
                                }
                                """)));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Brazil-Real", purchaseDate);

        assertThat(result).isPresent();
        assertThat(result.get().recordDate()).isEqualTo(purchaseDate);
    }

    @Test
    void findRate_shouldReturnRate_whenRecordDateIsExactly6MonthsBeforePurchase() {
        LocalDate purchaseDate = LocalDate.of(2024, 9, 15);
        LocalDate sixMonthsAgo = purchaseDate.minusMonths(6);
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.format("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Brazil-Real",
                                      "exchange_rate": "5.25",
                                      "record_date": "%s"
                                    }
                                  ]
                                }
                                """, sixMonthsAgo))));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Brazil-Real", purchaseDate);

        assertThat(result).isPresent();
        assertThat(result.get().recordDate()).isEqualTo(sixMonthsAgo);
    }

    @Test
    void findRate_shouldReturnEmpty_whenExchangeRateIsNegative() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Brazil-Real",
                                      "exchange_rate": "-1.50",
                                      "record_date": "2024-03-01"
                                    }
                                  ]
                                }
                                """)));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2024, 3, 15));

        assertThat(result).isEmpty();
    }

    @Test
    void findRate_shouldHandleTimeout() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withFixedDelay(16000)
                        .withBody("""
                                { "data": [] }
                                """)));

        assertThatThrownBy(() -> treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2024, 3, 15)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void findRate_shouldHandleMalformedJsonResponse() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ invalid json }")));

        assertThatThrownBy(() -> treasuryApiClient.findRate("Brazil-Real", LocalDate.of(2024, 3, 15)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void findRate_shouldExtractCurrencyNameWhenNoDashInDescription() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Dollar",
                                      "exchange_rate": "1.00",
                                      "record_date": "2024-03-01"
                                    }
                                  ]
                                }
                                """)));

        Optional<ExchangeRatePort.ExchangeRateResult> result =
                treasuryApiClient.findRate("Dollar", LocalDate.of(2024, 3, 15));

        assertThat(result).isPresent();
        assertThat(result.get().currencyName()).isEqualTo("Dollar");
    }
}

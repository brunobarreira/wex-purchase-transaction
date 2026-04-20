package com.wex.purchasetransaction.e2e;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@WireMockTest(httpPort = 8089)
class PurchaseTransactionEdgeCasesE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        WireMock.reset();
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    void shouldReturn400WhenCurrencyParameterIsMissing() {
        String transactionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Test missing currency",
                        "transactionDate", "2024-03-15",
                        "amountUsd", 50.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/api/v1/transactions/{id}", transactionId)
                .then()
                .statusCode(400);
    }

    @Test
    void shouldReturn400WhenIdIsNotValidUuid() {
        given()
                .param("currency", "BRL")
                .when()
                .get("/api/v1/transactions/{id}", "not-a-valid-uuid")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldStoreTransactionWithSpecialCharactersInDescription() {
        String transactionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Café & Résumé - 日本語",
                        "transactionDate", "2024-03-15",
                        "amountUsd", 25.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .body("description", equalTo("Café & Résumé - 日本語"))
                .extract().path("id");

        assertThat(transactionId).isNotNull();
    }

    @Test
    void shouldStoreTransactionWithMinimumAmount() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Minimum amount test",
                        "transactionDate", "2024-03-15",
                        "amountUsd", 0.01))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .body("amountUsd", equalTo(0.01f));
    }

    @Test
    void shouldConvertWithRateExactlyOnPurchaseDate() {
        LocalDate purchaseDate = LocalDate.of(2024, 3, 15);
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.format("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Brazil-Real",
                                      "exchange_rate": "4.85",
                                      "record_date": "%s"
                                    }
                                  ]
                                }
                                """, purchaseDate))));

        String transactionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Rate on exact date",
                        "transactionDate", purchaseDate.toString(),
                        "amountUsd", 100.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .param("currency", "BRL")
                .when()
                .get("/api/v1/transactions/{id}", transactionId)
                .then()
                .statusCode(200)
                .body("exchangeRate", equalTo(4.85f))
                .body("convertedAmount", equalTo(485.00f));
    }

    @Test
    void shouldConvertWithRateExactly6MonthsBeforePurchaseDate() {
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
                                      "exchange_rate": "5.10",
                                      "record_date": "%s"
                                    }
                                  ]
                                }
                                """, sixMonthsAgo))));

        String transactionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "6 months boundary",
                        "transactionDate", purchaseDate.toString(),
                        "amountUsd", 100.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .param("currency", "BRL")
                .when()
                .get("/api/v1/transactions/{id}", transactionId)
                .then()
                .statusCode(200)
                .body("exchangeRate", equalTo(5.10f))
                .body("convertedAmount", equalTo(510.00f));
    }

    @Test
    void shouldHandleHighPrecisionExchangeRate() {
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

        String transactionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "High precision rate",
                        "transactionDate", "2024-03-15",
                        "amountUsd", 100.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .param("currency", "BRL")
                .when()
                .get("/api/v1/transactions/{id}", transactionId)
                .then()
                .statusCode(200)
                .body("convertedAmount", equalTo(512.35f));
    }

    @Test
    void shouldReturn400WhenAmountIsNegative() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Negative amount",
                        "transactionDate", "2024-03-15",
                        "amountUsd", -10.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldReturn400WhenDescriptionIsMissing() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "transactionDate", "2024-03-15",
                        "amountUsd", 10.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldReturn400WhenAmountIsMissing() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Test",
                        "transactionDate", "2024-03-15"))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldNormalizeCurrencyCodeToUppercase() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Brazil-Real",
                                      "exchange_rate": "5.00",
                                      "record_date": "2024-03-01"
                                    }
                                  ]
                                }
                                """)));

        String transactionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Lowercase currency",
                        "transactionDate", "2024-03-15",
                        "amountUsd", 100.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .param("currency", "brl")
                .when()
                .get("/api/v1/transactions/{id}", transactionId)
                .then()
                .statusCode(200)
                .body("currencyCode", equalTo("BRL"));
    }
}

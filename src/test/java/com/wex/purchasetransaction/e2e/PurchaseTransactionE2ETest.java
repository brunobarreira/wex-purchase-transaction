package com.wex.purchasetransaction.e2e;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
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
class PurchaseTransactionE2ETest {

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
    void shouldStoreAndRetrieveTransactionWithCurrencyConversion() {
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
                        "description", "Conference ticket",
                        "transactionDate", "2024-03-15",
                        "amountUsd", 200.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("id", notNullValue())
                .body("description", equalTo("Conference ticket"))
                .body("amountUsd", equalTo(200.00f))
                .extract().path("id");

        given()
                .param("currency", "BRL")
                .when()
                .get("/api/v1/transactions/{id}", transactionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(transactionId))
                .body("amountUsd", equalTo(200.00f))
                .body("exchangeRate", equalTo(5.00f))
                .body("convertedAmount", equalTo(1000.00f))
                .body("currencyCode", equalTo("BRL"))
                .body("currencyName", equalTo("Real"));
    }

    @Test
    void shouldStoreAndRetrieveWithAmountRounding() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Japan-Yen",
                                      "exchange_rate": "149.75",
                                      "record_date": "2024-03-01"
                                    }
                                  ]
                                }
                                """)));

        String transactionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Tokyo hotel",
                        "transactionDate", "2024-03-15",
                        "amountUsd", 100.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .param("currency", "JPY")
                .when()
                .get("/api/v1/transactions/{id}", transactionId)
                .then()
                .statusCode(200)
                .body("convertedAmount", equalTo(14975.00f))
                .body("currencyCode", equalTo("JPY"))
                .body("currencyName", equalTo("Yen"));
    }

    @Test
    void shouldReturn400WhenTransactionDateIsInFuture() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Future purchase",
                        "transactionDate", LocalDate.now().plusDays(1).toString(),
                        "amountUsd", 10.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(400)
                .body("errors", notNullValue());
    }

    @Test
    void shouldReturn400WhenDescriptionExceeds50Chars() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "A".repeat(51),
                        "transactionDate", "2024-03-15",
                        "amountUsd", 10.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(400)
                .body("errors", notNullValue());
    }

    @Test
    void shouldReturn400WhenAmountIsZero() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Zero amount",
                        "transactionDate", "2024-03-15",
                        "amountUsd", 0.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldReturn400WhenCurrencyCodeIsUnsupported() {
        String transactionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Test",
                        "transactionDate", "2024-03-15",
                        "amountUsd", 50.00))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .param("currency", "INVALID")
                .when()
                .get("/api/v1/transactions/{id}", transactionId)
                .then()
                .statusCode(400);
    }

    @Test
    void shouldReturn422WhenNoExchangeRateWithin6Months() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                { "data": [] }
                                """)));

        String transactionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "description", "Old purchase",
                        "transactionDate", "2024-03-15",
                        "amountUsd", 99.99))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .param("currency", "EUR")
                .when()
                .get("/api/v1/transactions/{id}", transactionId)
                .then()
                .statusCode(422)
                .body("message", notNullValue());
    }

    @Test
    void shouldReturn404WhenTransactionDoesNotExist() {
        given()
                .param("currency", "BRL")
                .when()
                .get("/api/v1/transactions/{id}", UUID.randomUUID())
                .then()
                .statusCode(404);
    }
}

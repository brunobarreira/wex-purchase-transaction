package com.wex.purchasetransaction.unit.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.purchasetransaction.application.dto.ConvertedPurchaseTransactionResult;
import com.wex.purchasetransaction.application.dto.PurchaseTransactionResult;
import com.wex.purchasetransaction.application.port.in.RetrievePurchaseTransactionUseCase;
import com.wex.purchasetransaction.application.port.in.StorePurchaseTransactionUseCase;
import com.wex.purchasetransaction.domain.exception.CurrencyConversionException;
import com.wex.purchasetransaction.domain.exception.TransactionNotFoundException;
import com.wex.purchasetransaction.domain.exception.UnsupportedCurrencyException;
import com.wex.purchasetransaction.presentation.controller.PurchaseTransactionController;
import com.wex.purchasetransaction.presentation.exception.GlobalExceptionHandler;
import com.wex.purchasetransaction.presentation.mapper.PurchaseTransactionMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PurchaseTransactionController.class)
@Import({PurchaseTransactionMapper.class, GlobalExceptionHandler.class})
class PurchaseTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StorePurchaseTransactionUseCase storePurchaseTransactionUseCase;

    @MockBean
    private RetrievePurchaseTransactionUseCase retrievePurchaseTransactionUseCase;

    private static final UUID ID = UUID.randomUUID();

    @Test
    void store_shouldReturn201WhenValidRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "description", "Hotel stay",
                "transactionDate", "2024-03-15",
                "amountUsd", 123.45);

        PurchaseTransactionResult result = PurchaseTransactionResult.builder()
                .id(ID)
                .description("Hotel stay")
                .transactionDate(LocalDate.of(2024, 3, 15))
                .amountUsd(new BigDecimal("123.45"))
                .build();

        when(storePurchaseTransactionUseCase.store(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.description").value("Hotel stay"))
                .andExpect(jsonPath("$.amountUsd").value(123.45));
    }

    @Test
    void store_shouldAcceptTransactionDateAsToday() throws Exception {
        Map<String, Object> request = Map.of(
                "description", "Purchase today",
                "transactionDate", LocalDate.now().toString(),
                "amountUsd", 10.00);

        when(storePurchaseTransactionUseCase.store(any())).thenReturn(PurchaseTransactionResult.builder()
                .id(ID).description("Purchase today")
                .transactionDate(LocalDate.now()).amountUsd(new BigDecimal("10.00")).build());

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void store_shouldReturn400WhenTransactionDateIsInFuture() throws Exception {
        Map<String, Object> request = Map.of(
                "description", "Future purchase",
                "transactionDate", LocalDate.now().plusDays(1).toString(),
                "amountUsd", 10.00);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void store_shouldReturn400WhenTransactionDateIsMissing() throws Exception {
        Map<String, Object> request = Map.of(
                "description", "No date",
                "amountUsd", 10.00);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void store_shouldReturn400WhenDescriptionExceeds50Chars() throws Exception {
        Map<String, Object> request = Map.of(
                "description", "A".repeat(51),
                "transactionDate", "2024-03-15",
                "amountUsd", 10.00);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void store_shouldReturn400WhenDescriptionIsBlank() throws Exception {
        Map<String, Object> request = Map.of(
                "description", "   ",
                "transactionDate", "2024-03-15",
                "amountUsd", 10.00);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void store_shouldReturn400WhenAmountIsNegative() throws Exception {
        Map<String, Object> request = Map.of(
                "description", "Test",
                "transactionDate", "2024-03-15",
                "amountUsd", -1.00);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void store_shouldReturn400WhenAmountIsZero() throws Exception {
        Map<String, Object> request = Map.of(
                "description", "Test",
                "transactionDate", "2024-03-15",
                "amountUsd", 0.00);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void store_shouldReturn400WhenAmountBelowMinimum() throws Exception {
        String body = """
                {"description":"Test","transactionDate":"2024-03-15","amountUsd":0.001}
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retrieve_shouldReturn200WithConvertedAmount() throws Exception {
        ConvertedPurchaseTransactionResult result = ConvertedPurchaseTransactionResult.builder()
                .id(ID)
                .description("Hotel stay")
                .transactionDate(LocalDate.of(2024, 3, 15))
                .amountUsd(new BigDecimal("100.00"))
                .exchangeRate(new BigDecimal("4.97"))
                .convertedAmount(new BigDecimal("497.00"))
                .currencyCode("BRL")
                .currencyName("Real")
                .build();

        when(retrievePurchaseTransactionUseCase.retrieve(eq(ID), eq("BRL"))).thenReturn(result);

        mockMvc.perform(get("/api/v1/transactions/{id}", ID)
                        .param("currency", "BRL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convertedAmount").value(497.00))
                .andExpect(jsonPath("$.currencyCode").value("BRL"))
                .andExpect(jsonPath("$.currencyName").value("Real"))
                .andExpect(jsonPath("$.exchangeRate").value(4.97));
    }

    @Test
    void retrieve_shouldReturn404WhenTransactionNotFound() throws Exception {
        when(retrievePurchaseTransactionUseCase.retrieve(any(), any()))
                .thenThrow(new TransactionNotFoundException(ID));

        mockMvc.perform(get("/api/v1/transactions/{id}", ID).param("currency", "BRL"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retrieve_shouldReturn400WhenCurrencyUnsupported() throws Exception {
        when(retrievePurchaseTransactionUseCase.retrieve(any(), any()))
                .thenThrow(new UnsupportedCurrencyException("XYZ"));

        mockMvc.perform(get("/api/v1/transactions/{id}", ID).param("currency", "XYZ"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retrieve_shouldReturn422WhenNoRateAvailable() throws Exception {
        when(retrievePurchaseTransactionUseCase.retrieve(any(), any()))
                .thenThrow(new CurrencyConversionException("cannot be converted"));

        mockMvc.perform(get("/api/v1/transactions/{id}", ID).param("currency", "BRL"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void retrieve_shouldReturn400WhenCurrencyParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/{id}", ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retrieve_shouldReturn400WhenIdIsNotValidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/{id}", "not-a-valid-uuid")
                        .param("currency", "BRL"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retrieve_shouldReturn400WhenIdIsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/{id}", " ")
                        .param("currency", "BRL"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void store_shouldReturn400WhenContentTypeIsNotJson() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("description=Test&transactionDate=2024-03-15&amountUsd=10.00"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void store_shouldReturn400WhenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void store_shouldReturn400WhenBodyIsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void store_shouldReturn400WhenTransactionDateIsInvalidFormat() throws Exception {
        String body = """
                {"description":"Test","transactionDate":"15-03-2024","amountUsd":10.00}
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void store_shouldAcceptDescriptionWithSpecialCharacters() throws Exception {
        Map<String, Object> request = Map.of(
                "description", "Café & Résumé - 日本語",
                "transactionDate", "2024-03-15",
                "amountUsd", 25.00);

        when(storePurchaseTransactionUseCase.store(any())).thenReturn(PurchaseTransactionResult.builder()
                .id(ID)
                .description("Café & Résumé - 日本語")
                .transactionDate(LocalDate.of(2024, 3, 15))
                .amountUsd(new BigDecimal("25.00"))
                .build());

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Café & Résumé - 日本語"));
    }

    @Test
    void store_shouldAcceptMinimumValidAmount() throws Exception {
        Map<String, Object> request = Map.of(
                "description", "Minimum amount",
                "transactionDate", "2024-03-15",
                "amountUsd", 0.01);

        when(storePurchaseTransactionUseCase.store(any())).thenReturn(PurchaseTransactionResult.builder()
                .id(ID)
                .description("Minimum amount")
                .transactionDate(LocalDate.of(2024, 3, 15))
                .amountUsd(new BigDecimal("0.01"))
                .build());

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountUsd").value(0.01));
    }

    @Test
    void retrieve_shouldNormalizeCurrencyCodeToUppercase() throws Exception {
        ConvertedPurchaseTransactionResult result = ConvertedPurchaseTransactionResult.builder()
                .id(ID)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 3, 15))
                .amountUsd(new BigDecimal("100.00"))
                .exchangeRate(new BigDecimal("4.97"))
                .convertedAmount(new BigDecimal("497.00"))
                .currencyCode("BRL")
                .currencyName("Real")
                .build();

        when(retrievePurchaseTransactionUseCase.retrieve(eq(ID), eq("brl"))).thenReturn(result);

        mockMvc.perform(get("/api/v1/transactions/{id}", ID)
                        .param("currency", "brl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyCode").value("BRL"));
    }
}

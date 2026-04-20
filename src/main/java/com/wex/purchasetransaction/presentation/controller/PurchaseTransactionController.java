package com.wex.purchasetransaction.presentation.controller;

import com.wex.purchasetransaction.application.dto.ConvertedPurchaseTransactionResult;
import com.wex.purchasetransaction.application.dto.PurchaseTransactionResult;
import com.wex.purchasetransaction.application.dto.StorePurchaseTransactionCommand;
import com.wex.purchasetransaction.application.port.in.RetrievePurchaseTransactionUseCase;
import com.wex.purchasetransaction.application.port.in.StorePurchaseTransactionUseCase;
import com.wex.purchasetransaction.presentation.dto.request.StorePurchaseTransactionRequest;
import com.wex.purchasetransaction.presentation.dto.response.ConvertedPurchaseTransactionResponse;
import com.wex.purchasetransaction.presentation.dto.response.PurchaseTransactionResponse;
import com.wex.purchasetransaction.presentation.mapper.PurchaseTransactionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Purchase Transactions", description = "Endpoints for storing and retrieving purchase transactions")
public class PurchaseTransactionController {

    private final StorePurchaseTransactionUseCase storePurchaseTransactionUseCase;
    private final RetrievePurchaseTransactionUseCase retrievePurchaseTransactionUseCase;
    private final PurchaseTransactionMapper mapper;

    @Operation(summary = "Store a purchase transaction",
            description = "Persists a purchase transaction in USD and returns it with a unique identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction stored successfully",
                    content = @Content(schema = @Schema(implementation = PurchaseTransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PurchaseTransactionResponse> store(
            @Valid @RequestBody StorePurchaseTransactionRequest request) {
        StorePurchaseTransactionCommand command = mapper.toCommand(request);
        PurchaseTransactionResult result = storePurchaseTransactionUseCase.store(command);
        PurchaseTransactionResponse response = mapper.toResponse(result);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Retrieve a purchase transaction with currency conversion",
            description = "Returns the stored transaction with the amount converted to the target currency "
                    + "using the US Treasury exchange rate active at the time of the purchase. "
                    + "Rate must be available within 6 months prior to the transaction date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction retrieved and converted",
                    content = @Content(schema = @Schema(implementation = ConvertedPurchaseTransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Unsupported currency code", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content),
            @ApiResponse(responseCode = "422", description = "Exchange rate unavailable within 6 months",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ConvertedPurchaseTransactionResponse> retrieve(
            @Parameter(description = "Transaction unique identifier") @PathVariable UUID id,
            @Parameter(description = "ISO 4217 currency code (e.g. BRL, EUR, JPY)", required = true)
            @RequestParam String currency) {
        ConvertedPurchaseTransactionResult result = retrievePurchaseTransactionUseCase.retrieve(id, currency);
        return ResponseEntity.ok(mapper.toConvertedResponse(result));
    }
}

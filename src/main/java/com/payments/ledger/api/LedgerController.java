package com.payments.ledger.api;

import com.payments.ledger.dto.*;
import com.payments.ledger.models.Account;
import com.payments.ledger.models.Transaction;
import com.payments.ledger.services.AccountService;
import com.payments.ledger.services.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payments Ledger", description = "APIs for managing accounts and fund transfers")
public class LedgerController {
    private static final Logger logger = LoggerFactory.getLogger(LedgerController.class);
    private final AccountService accountService;
    private final TransferService transferService;

    public LedgerController(AccountService accountService, TransferService transferService) {
        this.accountService = accountService;
        this.transferService = transferService;
    }

    @Operation(summary = "Health check", description = "Check if the API is running")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create account", description = "Creates a new account with an initial balance")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Account created successfully",
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid initial balance",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        logger.info("Creating account with initial balance: {}", request.getInitialBalance());
        Account account = accountService.createAccount(request.getInitialBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @Operation(summary = "Get account", description = "Retrieves account details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account found",
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "Account ID", example = "acc_a1b2c3d4e5") @PathVariable String id) {
        logger.info("Getting account: {}", id);
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @Operation(summary = "Create transfer", description = "Transfers funds from one account to another")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transfer completed successfully",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Insufficient funds or invalid amount",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        logger.info("Creating transfer from {} to {} amount {}",
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        Transaction transaction = transferService.transfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(transaction));
    }

    @Operation(summary = "Get transaction", description = "Retrieves transaction details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction found",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Transaction not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @Parameter(description = "Transaction ID", example = "txn_k1l2m3n4o5") @PathVariable String id) {
        logger.info("Getting transaction: {}", id);
        Transaction transaction = transferService.getTransaction(id);
        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }
}

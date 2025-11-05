package com.payments.ledger.api;

import com.payments.ledger.api.dto.*;
import com.payments.ledger.models.Account;
import com.payments.ledger.models.Transaction;
import com.payments.ledger.services.AccountService;
import com.payments.ledger.services.TransferService;
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
public class LedgerController {
    private static final Logger logger = LoggerFactory.getLogger(LedgerController.class);
    private final AccountService accountService;
    private final TransferService transferService;

    public LedgerController(AccountService accountService, TransferService transferService) {
        this.accountService = accountService;
        this.transferService = transferService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        logger.info("Creating account with initial balance: {}", request.getInitialBalance());
        Account account = accountService.createAccount(request.getInitialBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String id) {
        logger.info("Getting account: {}", id);
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

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

    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String id) {
        logger.info("Getting transaction: {}", id);
        Transaction transaction = transferService.getTransaction(id);
        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }
}

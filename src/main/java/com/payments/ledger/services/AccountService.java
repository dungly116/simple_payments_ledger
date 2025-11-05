package com.payments.ledger.services;

import com.payments.ledger.exceptions.AccountNotFoundException;
import com.payments.ledger.exceptions.InvalidAmountException;
import com.payments.ledger.models.Account;
import com.payments.ledger.repositories.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(BigDecimal initialBalance) {
        validateBalance(initialBalance);

        Account account = accountRepository.create(initialBalance);
        logger.info("Created account: {} with balance: {}", account.getId(), initialBalance);
        return account;
    }

    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account " + accountId + " not found"));
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    private void validateBalance(BigDecimal balance) {
        if (balance == null) {
            throw new InvalidAmountException("Balance cannot be null");
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Balance cannot be negative");
        }
        if (balance.scale() > 2) {
            throw new InvalidAmountException("Balance cannot have more than 2 decimal places");
        }
    }
}

package com.payments.ledger.services;

import com.payments.ledger.exceptions.AccountNotFoundException;
import com.payments.ledger.exceptions.InsufficientFundsException;
import com.payments.ledger.exceptions.InvalidAmountException;
import com.payments.ledger.exceptions.InvalidTransferException;
import com.payments.ledger.models.Account;
import com.payments.ledger.models.Transaction;
import com.payments.ledger.repositories.AccountRepository;
import com.payments.ledger.repositories.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

@Service
public class TransferService {
    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public Transaction transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        validateTransferRequest(fromAccountId, toAccountId, amount);

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException("Account " + fromAccountId + " not found"));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException("Account " + toAccountId + " not found"));

        String transactionId = "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Transaction transaction = new Transaction(transactionId, fromAccountId, toAccountId, amount);
        transactionRepository.save(transaction);

        try {
            executeTransfer(fromAccount, toAccount, amount, transaction);
            logger.info("Transfer completed: {} from {} to {} amount {}",
                    transactionId, fromAccountId, toAccountId, amount);
            return transaction;
        } catch (InsufficientFundsException | InvalidTransferException e) {
            logger.error("Transfer failed: {}", e.getMessage());
            throw e;
        }
    }

    private void executeTransfer(Account fromAccount, Account toAccount, BigDecimal amount, Transaction transaction) {
        // Lock ordering prevents deadlock by ensuring consistent acquisition order
        Account firstAccount;
        Account secondAccount;

        if (fromAccount.getId().compareTo(toAccount.getId()) < 0) {
            firstAccount = fromAccount;
            secondAccount = toAccount;
        } else {
            firstAccount = toAccount;
            secondAccount = fromAccount;
        }

        Lock firstLock = firstAccount.getLock();
        Lock secondLock = secondAccount.getLock();

        firstLock.lock();
        try {
            secondLock.lock();
            try {
                // Balance check happens inside locked section to prevent race conditions
                if (fromAccount.getBalance().compareTo(amount) < 0) {
                    transaction.markFailed("Insufficient funds");
                    transactionRepository.save(transaction);
                    throw new InsufficientFundsException(
                            "Account " + fromAccount.getId() + " has insufficient funds. " +
                                    "Balance: " + fromAccount.getBalance() + ", Required: " + amount
                    );
                }

                // Atomic section: both operations happen together
                fromAccount.debit(amount);
                toAccount.credit(amount);

                transaction.markCompleted();
                transactionRepository.save(transaction);

                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);

            } finally {
                secondLock.unlock();
            }
        } finally {
            firstLock.unlock();
        }
    }

    private void validateTransferRequest(String fromAccountId, String toAccountId, BigDecimal amount) {
        if (fromAccountId == null || toAccountId == null) {
            throw new InvalidTransferException("Account IDs cannot be null");
        }

        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransferException("Cannot transfer to the same account");
        }

        if (amount == null) {
            throw new InvalidAmountException("Amount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }

        if (amount.scale() > 2) {
            throw new InvalidAmountException("Amount cannot have more than 2 decimal places");
        }
    }

    public Transaction getTransaction(String transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction " + transactionId + " not found"));
    }
}

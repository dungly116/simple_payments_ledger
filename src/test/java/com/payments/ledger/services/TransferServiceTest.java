package com.payments.ledger.services;

import com.payments.ledger.exceptions.AccountNotFoundException;
import com.payments.ledger.exceptions.InsufficientFundsException;
import com.payments.ledger.exceptions.InvalidAmountException;
import com.payments.ledger.exceptions.InvalidTransferException;
import com.payments.ledger.models.Account;
import com.payments.ledger.models.Transaction;
import com.payments.ledger.models.TransactionStatus;
import com.payments.ledger.repositories.AccountRepository;
import com.payments.ledger.repositories.InMemoryAccountRepository;
import com.payments.ledger.repositories.InMemoryTransactionRepository;
import com.payments.ledger.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        transactionRepository = new InMemoryTransactionRepository();
        transferService = new TransferService(accountRepository, transactionRepository);
    }

    @Test
    void testSuccessfulTransfer() {
        Account alice = accountRepository.create(new BigDecimal("1000.00"));
        Account bob = accountRepository.create(new BigDecimal("500.00"));

        Transaction transaction = transferService.transfer(
                alice.getId(),
                bob.getId(),
                new BigDecimal("200.00")
        );

        assertNotNull(transaction);
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        assertEquals(new BigDecimal("800.00"), accountRepository.findById(alice.getId()).get().getBalance());
        assertEquals(new BigDecimal("700.00"), accountRepository.findById(bob.getId()).get().getBalance());
    }

    @Test
    void testTransferWithInsufficientFunds() {
        Account alice = accountRepository.create(new BigDecimal("100.00"));
        Account bob = accountRepository.create(new BigDecimal("500.00"));

        assertThrows(InsufficientFundsException.class, () -> {
            transferService.transfer(alice.getId(), bob.getId(), new BigDecimal("200.00"));
        });

        assertEquals(new BigDecimal("100.00"), accountRepository.findById(alice.getId()).get().getBalance());
        assertEquals(new BigDecimal("500.00"), accountRepository.findById(bob.getId()).get().getBalance());
    }

    @Test
    void testTransferToNonExistentAccount() {
        Account alice = accountRepository.create(new BigDecimal("1000.00"));

        assertThrows(AccountNotFoundException.class, () -> {
            transferService.transfer(alice.getId(), "non_existent_account", new BigDecimal("200.00"));
        });

        assertEquals(new BigDecimal("1000.00"), accountRepository.findById(alice.getId()).get().getBalance());
    }

    @Test
    void testTransferFromNonExistentAccount() {
        Account bob = accountRepository.create(new BigDecimal("500.00"));

        assertThrows(AccountNotFoundException.class, () -> {
            transferService.transfer("non_existent_account", bob.getId(), new BigDecimal("200.00"));
        });

        assertEquals(new BigDecimal("500.00"), accountRepository.findById(bob.getId()).get().getBalance());
    }

    @Test
    void testTransferWithNegativeAmount() {
        Account alice = accountRepository.create(new BigDecimal("1000.00"));
        Account bob = accountRepository.create(new BigDecimal("500.00"));

        assertThrows(InvalidAmountException.class, () -> {
            transferService.transfer(alice.getId(), bob.getId(), new BigDecimal("-100.00"));
        });
    }

    @Test
    void testTransferWithZeroAmount() {
        Account alice = accountRepository.create(new BigDecimal("1000.00"));
        Account bob = accountRepository.create(new BigDecimal("500.00"));

        assertThrows(InvalidAmountException.class, () -> {
            transferService.transfer(alice.getId(), bob.getId(), BigDecimal.ZERO);
        });
    }

    @Test
    void testTransferWithMoreThan2DecimalPlaces() {
        Account alice = accountRepository.create(new BigDecimal("1000.00"));
        Account bob = accountRepository.create(new BigDecimal("500.00"));

        assertThrows(InvalidAmountException.class, () -> {
            transferService.transfer(alice.getId(), bob.getId(), new BigDecimal("100.123"));
        });
    }

    @Test
    void testTransferToSameAccount() {
        Account alice = accountRepository.create(new BigDecimal("1000.00"));

        assertThrows(InvalidTransferException.class, () -> {
            transferService.transfer(alice.getId(), alice.getId(), new BigDecimal("200.00"));
        });

        assertEquals(new BigDecimal("1000.00"), accountRepository.findById(alice.getId()).get().getBalance());
    }

    @Test
    void testExactBalanceTransfer() {
        Account alice = accountRepository.create(new BigDecimal("100.00"));
        Account bob = accountRepository.create(new BigDecimal("500.00"));

        Transaction transaction = transferService.transfer(
                alice.getId(),
                bob.getId(),
                new BigDecimal("100.00")
        );

        assertNotNull(transaction);
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        assertEquals(new BigDecimal("0.00"), accountRepository.findById(alice.getId()).get().getBalance());
        assertEquals(new BigDecimal("600.00"), accountRepository.findById(bob.getId()).get().getBalance());
    }
}

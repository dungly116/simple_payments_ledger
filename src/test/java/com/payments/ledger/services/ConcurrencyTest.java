package com.payments.ledger.services;

import com.payments.ledger.exceptions.InsufficientFundsException;
import com.payments.ledger.models.Account;
import com.payments.ledger.repositories.AccountRepository;
import com.payments.ledger.repositories.InMemoryAccountRepository;
import com.payments.ledger.repositories.InMemoryTransactionRepository;
import com.payments.ledger.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {
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
    void testConcurrentTransfersFromSameAccount() throws InterruptedException, ExecutionException {
        Account alice = accountRepository.create(new BigDecimal("1000.00"));
        Account bob = accountRepository.create(new BigDecimal("0.00"));

        int numThreads = 20;
        BigDecimal transferAmount = new BigDecimal("100.00");
        CyclicBarrier barrier = new CyclicBarrier(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            Future<?> future = executor.submit(() -> {
                try {
                    barrier.await();
                    transferService.transfer(alice.getId(), bob.getId(), transferAmount);
                    successCount.incrementAndGet();
                } catch (InsufficientFundsException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }
            });
            futures.add(future);
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        Account finalAlice = accountRepository.findById(alice.getId()).get();
        Account finalBob = accountRepository.findById(bob.getId()).get();

        assertEquals(10, successCount.get(), "Expected exactly 10 successful transfers");
        assertEquals(10, failureCount.get(), "Expected exactly 10 failed transfers");
        assertEquals(new BigDecimal("0.00"), finalAlice.getBalance(), "Alice should have 0 balance");
        assertEquals(new BigDecimal("1000.00"), finalBob.getBalance(), "Bob should have 1000 balance");
    }

    @Test
    void testDeadlockPrevention() throws InterruptedException, ExecutionException, TimeoutException {
        Account alice = accountRepository.create(new BigDecimal("1000.00"));
        Account bob = accountRepository.create(new BigDecimal("1000.00"));

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<?> future1 = executor.submit(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    transferService.transfer(alice.getId(), bob.getId(), new BigDecimal("10.00"));
                    Thread.sleep(1);
                } catch (InsufficientFundsException e) {
                    // Expected in some cases
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }
            }
        });

        Future<?> future2 = executor.submit(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    transferService.transfer(bob.getId(), alice.getId(), new BigDecimal("10.00"));
                    Thread.sleep(1);
                } catch (InsufficientFundsException e) {
                    // Expected in some cases
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }
            }
        });

        future1.get(10, TimeUnit.SECONDS);
        future2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Threads should complete without deadlock");
    }

    @Test
    void testMoneyConservation() throws InterruptedException, ExecutionException {
        Account account1 = accountRepository.create(new BigDecimal("1000.00"));
        Account account2 = accountRepository.create(new BigDecimal("1000.00"));
        Account account3 = accountRepository.create(new BigDecimal("1000.00"));
        Account account4 = accountRepository.create(new BigDecimal("1000.00"));
        Account account5 = accountRepository.create(new BigDecimal("1000.00"));

        BigDecimal initialTotalBalance = new BigDecimal("5000.00");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<?>> futures = new ArrayList<>();

        List<Account> accounts = List.of(account1, account2, account3, account4, account5);

        for (int i = 0; i < 100; i++) {
            int fromIndex = i % 5;
            int toIndex = (i + 1) % 5;

            Future<?> future = executor.submit(() -> {
                try {
                    transferService.transfer(
                            accounts.get(fromIndex).getId(),
                            accounts.get(toIndex).getId(),
                            new BigDecimal("50.00")
                    );
                } catch (InsufficientFundsException e) {
                    // Expected in some cases
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }
            });
            futures.add(future);
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        BigDecimal finalTotalBalance = accountRepository.findAll().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(initialTotalBalance, finalTotalBalance, "Total balance should remain constant");
    }

    @Test
    void testParallelExecution() throws InterruptedException {
        Account alice = accountRepository.create(new BigDecimal("1000.00"));
        Account bob = accountRepository.create(new BigDecimal("1000.00"));
        Account charlie = accountRepository.create(new BigDecimal("1000.00"));
        Account david = accountRepository.create(new BigDecimal("1000.00"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch finishLatch = new CountDownLatch(2);

        long[] startTime = new long[2];
        long[] endTime = new long[2];

        executor.submit(() -> {
            try {
                startTime[0] = System.nanoTime();
                startLatch.countDown();
                startLatch.await();

                for (int i = 0; i < 10; i++) {
                    transferService.transfer(alice.getId(), bob.getId(), new BigDecimal("10.00"));
                }

                endTime[0] = System.nanoTime();
                finishLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executor.submit(() -> {
            try {
                startTime[1] = System.nanoTime();
                startLatch.countDown();
                startLatch.await();

                for (int i = 0; i < 10; i++) {
                    transferService.transfer(charlie.getId(), david.getId(), new BigDecimal("10.00"));
                }

                endTime[1] = System.nanoTime();
                finishLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(finishLatch.await(5, TimeUnit.SECONDS), "Transfers should complete within timeout");
        executor.shutdown();

        long overlap = Math.min(endTime[0], endTime[1]) - Math.max(startTime[0], startTime[1]);
        assertTrue(overlap > 0, "Transfers on different accounts should execute in parallel");

        assertEquals(new BigDecimal("900.00"), alice.getBalance());
        assertEquals(new BigDecimal("1100.00"), bob.getBalance());
        assertEquals(new BigDecimal("900.00"), charlie.getBalance());
        assertEquals(new BigDecimal("1100.00"), david.getBalance());
    }
}

package com.payments.ledger.repositories;

import com.payments.ledger.models.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    Optional<Transaction> findById(String id);

    List<Transaction> findAll();

    List<Transaction> findByAccountId(String accountId);
}

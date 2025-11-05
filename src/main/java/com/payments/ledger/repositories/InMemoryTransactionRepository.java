package com.payments.ledger.repositories;

import com.payments.ledger.models.Transaction;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {
    private final Map<String, Transaction> storage = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        storage.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<Transaction> findByAccountId(String accountId) {
        return storage.values().stream()
                .filter(t -> t.getFromAccountId().equals(accountId) ||
                           t.getToAccountId().equals(accountId))
                .collect(Collectors.toList());
    }
}

package com.payments.ledger.repositories;

import com.payments.ledger.models.Account;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> storage = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        storage.put(account.getId(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void deleteById(String id) {
        storage.remove(id);
    }

    @Override
    public boolean existsById(String id) {
        return storage.containsKey(id);
    }

    @Override
    public Account create(BigDecimal initialBalance) {
        String id = "acc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Account account = new Account(id, initialBalance);
        return save(account);
    }
}

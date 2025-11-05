package com.payments.ledger.repositories;

import com.payments.ledger.models.Account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);

    Optional<Account> findById(String id);

    List<Account> findAll();

    void deleteById(String id);

    boolean existsById(String id);

    Account create(BigDecimal initialBalance);
}

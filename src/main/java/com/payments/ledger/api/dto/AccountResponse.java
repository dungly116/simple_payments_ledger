package com.payments.ledger.api.dto;

import com.payments.ledger.models.Account;

import java.math.BigDecimal;

public class AccountResponse {
    private String id;
    private BigDecimal balance;

    public AccountResponse() {
    }

    public AccountResponse(String id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getBalance());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}

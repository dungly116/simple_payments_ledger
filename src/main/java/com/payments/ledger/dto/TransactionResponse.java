package com.payments.ledger.dto;

import com.payments.ledger.models.Transaction;
import com.payments.ledger.models.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionResponse {
    private String id;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private TransactionStatus status;
    private Instant timestamp;
    private String failureReason;

    public TransactionResponse() {
    }

    public TransactionResponse(String id, String fromAccountId, String toAccountId,
                              BigDecimal amount, TransactionStatus status,
                              Instant timestamp, String failureReason) {
        this.id = id;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = status;
        this.timestamp = timestamp;
        this.failureReason = failureReason;
    }

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getTimestamp(),
                transaction.getFailureReason()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(String fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(String toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}

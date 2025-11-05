package com.payments.ledger.api.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateAccountRequest {
    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Initial balance can have maximum 2 decimal places")
    private BigDecimal initialBalance;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}

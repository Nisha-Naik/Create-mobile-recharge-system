package com.rechargeapp.model;

import com.rechargeapp.util.ValidationException;
import java.math.BigDecimal;

public class Payment {
    private final String paymentMethod;
    private final BigDecimal amount;
    private final BigDecimal openingBalance;
    private BigDecimal remainingBalance;

    public Payment(String paymentMethod, BigDecimal amount, BigDecimal openingBalance) {
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.openingBalance = openingBalance;
        this.remainingBalance = openingBalance;
    }

    public void process() {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Recharge amount must be positive.");
        }
        if (openingBalance.compareTo(amount) < 0) {
            throw new ValidationException("Current balance is not sufficient for this recharge.");
        }
        remainingBalance = openingBalance.subtract(amount);
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }
}

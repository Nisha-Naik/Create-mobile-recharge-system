package com.rechargeapp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Recharge {
    private final User user;
    private final Operator operator;
    private final BigDecimal rechargeAmount;
    private final String rechargePlan;
    private final String paymentMethod;
    private final String transactionId;
    private final LocalDateTime dateTime;
    private final BigDecimal remainingBalance;

    public Recharge(
            User user,
            Operator operator,
            BigDecimal rechargeAmount,
            String rechargePlan,
            String paymentMethod,
            String transactionId,
            LocalDateTime dateTime,
            BigDecimal remainingBalance
    ) {
        this.user = user;
        this.operator = operator;
        this.rechargeAmount = rechargeAmount;
        this.rechargePlan = rechargePlan;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.dateTime = dateTime;
        this.remainingBalance = remainingBalance;
    }

    public User getUser() {
        return user;
    }

    public Operator getOperator() {
        return operator;
    }

    public BigDecimal getRechargeAmount() {
        return rechargeAmount;
    }

    public String getRechargePlan() {
        return rechargePlan;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }
}

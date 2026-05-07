package com.rechargeapp.model;

import java.math.BigDecimal;

public class User {
    private final String mobileNumber;
    private final String customerName;
    private BigDecimal currentBalance;

    public User(String mobileNumber, String customerName, BigDecimal currentBalance) {
        this.mobileNumber = mobileNumber;
        this.customerName = customerName;
        this.currentBalance = currentBalance;
    }

    public void updateBalance(BigDecimal newBalance) {
        currentBalance = newBalance;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
}

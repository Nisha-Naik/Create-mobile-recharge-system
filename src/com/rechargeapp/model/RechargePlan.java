package com.rechargeapp.model;

import java.math.BigDecimal;

public class RechargePlan {
    private final String name;
    private final BigDecimal amount;
    private final String validity;
    private final String data;
    private final String badge;

    public RechargePlan(String name, BigDecimal amount, String validity, String data, String badge) {
        this.name = name;
        this.amount = amount;
        this.validity = validity;
        this.data = data;
        this.badge = badge;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getValidity() {
        return validity;
    }

    public String getData() {
        return data;
    }

    public String getBadge() {
        return badge;
    }
}

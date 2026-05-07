package com.rechargeapp.model;

public class RechargeRequest {
    private final String mobileNumber;
    private final String customerName;
    private final String operator;
    private final String rechargeAmount;
    private final String rechargePlan;
    private final String paymentMethod;
    private final String currentBalance;

    public RechargeRequest(
            String mobileNumber,
            String customerName,
            String operator,
            String rechargeAmount,
            String rechargePlan,
            String paymentMethod,
            String currentBalance
    ) {
        this.mobileNumber = mobileNumber;
        this.customerName = customerName;
        this.operator = operator;
        this.rechargeAmount = rechargeAmount;
        this.rechargePlan = rechargePlan;
        this.paymentMethod = paymentMethod;
        this.currentBalance = currentBalance;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getOperator() {
        return operator;
    }

    public String getRechargeAmount() {
        return rechargeAmount;
    }

    public String getRechargePlan() {
        return rechargePlan;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getCurrentBalance() {
        return currentBalance;
    }
}

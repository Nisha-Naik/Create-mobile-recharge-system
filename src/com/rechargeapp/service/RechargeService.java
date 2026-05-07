package com.rechargeapp.service;

import com.rechargeapp.model.Operator;
import com.rechargeapp.model.Payment;
import com.rechargeapp.model.Recharge;
import com.rechargeapp.model.RechargeHistory;
import com.rechargeapp.model.RechargePlan;
import com.rechargeapp.model.RechargeRequest;
import com.rechargeapp.model.User;
import com.rechargeapp.util.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RechargeService {
    private static final DateTimeFormatter TRANSACTION_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RechargeHistory rechargeHistory;
    private final SecureRandom random = new SecureRandom();
    private final Map<Operator, List<RechargePlan>> plans = new EnumMap<>(Operator.class);

    public RechargeService(Path historyFile) {
        this.rechargeHistory = new RechargeHistory(historyFile);
        seedPlans();
    }

    public Recharge processRecharge(RechargeRequest request) {
        validateRequired(request);

        Operator operator = Operator.fromDisplayName(request.getOperator());
        String mobileNumber = request.getMobileNumber().trim();
        String customerName = request.getCustomerName().trim();
        String rechargePlan = request.getRechargePlan().trim();
        String paymentMethod = request.getPaymentMethod().trim();
        BigDecimal amount = parseMoney(request.getRechargeAmount(), "Recharge amount");
        BigDecimal currentBalance = parseMoney(request.getCurrentBalance(), "Current balance");

        if (!mobileNumber.matches("\\d{10}")) {
            throw new ValidationException("Mobile number must contain exactly 10 digits.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Recharge amount must be positive.");
        }
        if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Current balance cannot be negative.");
        }
        if (currentBalance.compareTo(amount) < 0) {
            throw new ValidationException("Current balance is not sufficient for this recharge.");
        }

        User user = new User(mobileNumber, customerName, currentBalance);
        Payment payment = new Payment(paymentMethod, amount, currentBalance);
        payment.process();
        user.updateBalance(payment.getRemainingBalance());

        Recharge recharge = new Recharge(
                user,
                operator,
                amount,
                rechargePlan,
                payment.getPaymentMethod(),
                generateTransactionId(),
                LocalDateTime.now(),
                payment.getRemainingBalance()
        );

        rechargeHistory.addRecharge(recharge);
        return recharge;
    }

    public List<Recharge> getRechargeHistory() {
        return rechargeHistory.getAllRecharges();
    }

    public List<RechargePlan> getPlansForOperator(String operatorName) {
        Operator operator = Operator.fromDisplayName(operatorName);
        return new ArrayList<>(plans.get(operator));
    }

    private void validateRequired(RechargeRequest request) {
        if (request == null) {
            throw new ValidationException("Recharge details are required.");
        }
        require(request.getMobileNumber(), "Mobile number");
        require(request.getCustomerName(), "Customer name");
        require(request.getOperator(), "Operator");
        require(request.getRechargeAmount(), "Recharge amount");
        require(request.getRechargePlan(), "Recharge plan");
        require(request.getPaymentMethod(), "Payment method");
        require(request.getCurrentBalance(), "Current balance");
    }

    private void require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
    }

    private BigDecimal parseMoney(String value, String label) {
        try {
            return new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new ValidationException(label + " must be a valid number.");
        }
    }

    private String generateTransactionId() {
        String time = LocalDateTime.now().format(TRANSACTION_TIME);
        int suffix = random.nextInt(10_000);
        return "TXN" + time + String.format("%04d", suffix);
    }

    private void seedPlans() {
        plans.put(Operator.JIO, List.of(
                plan("Jio 5G Starter", "239", "28 days", "1.5GB/day", "Popular"),
                plan("Jio Unlimited Max", "399", "28 days", "2.5GB/day", "5G Ready"),
                plan("Jio Annual Value", "2999", "365 days", "2GB/day", "Best Value")
        ));
        plans.put(Operator.AIRTEL, List.of(
                plan("Airtel Smart Pack", "265", "28 days", "1GB/day", "Saver"),
                plan("Airtel Infinity", "349", "28 days", "2GB/day", "Popular"),
                plan("Airtel Prime Yearly", "3359", "365 days", "2.5GB/day", "Premium")
        ));
        plans.put(Operator.VI, List.of(
                plan("VI Weekend Data", "299", "28 days", "1.5GB/day", "Weekend"),
                plan("VI Hero Unlimited", "359", "28 days", "2GB/day", "Hero"),
                plan("VI Long Stay", "1799", "365 days", "24GB total", "Long Term")
        ));
        plans.put(Operator.BSNL, List.of(
                plan("BSNL Voice Plus", "199", "30 days", "2GB total", "Voice"),
                plan("BSNL Bharat Data", "347", "54 days", "2GB/day", "Value"),
                plan("BSNL Annual Freedom", "2399", "395 days", "2GB/day", "Annual")
        ));
    }

    private RechargePlan plan(String name, String amount, String validity, String data, String badge) {
        return new RechargePlan(name, new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP), validity, data, badge);
    }
}

package com.rechargeapp.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class RechargeHistory {
    private static final String HEADER =
            "transactionId,dateTime,mobileNumber,customerName,operator,amount,plan,paymentMethod,remainingBalance";

    private final List<Recharge> recharges = new ArrayList<>();
    private final Path storageFile;

    public RechargeHistory(Path storageFile) {
        this.storageFile = storageFile;
        load();
    }

    public synchronized void addRecharge(Recharge recharge) {
        recharges.add(0, recharge);
        try {
            save();
        } catch (IllegalStateException ex) {
            recharges.remove(recharge);
            throw ex;
        }
    }

    public synchronized List<Recharge> getAllRecharges() {
        return Collections.unmodifiableList(new ArrayList<>(recharges));
    }

    private void load() {
        if (!Files.exists(storageFile)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(storageFile, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.isBlank()) {
                    continue;
                }

                String[] values = line.split(",", -1);
                if (values.length != 9) {
                    continue;
                }

                User user = new User(values[2], decode(values[3]), new BigDecimal(values[8]));
                Recharge recharge = new Recharge(
                        user,
                        Operator.fromDisplayName(values[4]),
                        new BigDecimal(values[5]),
                        decode(values[6]),
                        decode(values[7]),
                        values[0],
                        LocalDateTime.parse(values[1]),
                        new BigDecimal(values[8])
                );
                recharges.add(recharge);
            }
        } catch (Exception ignored) {
            recharges.clear();
        }
    }

    private void save() {
        try {
            Files.createDirectories(storageFile.getParent());
            List<String> lines = new ArrayList<>();
            lines.add(HEADER);
            for (Recharge recharge : recharges) {
                lines.add(String.join(",",
                        recharge.getTransactionId(),
                        recharge.getDateTime().toString(),
                        recharge.getUser().getMobileNumber(),
                        encode(recharge.getUser().getCustomerName()),
                        recharge.getOperator().getDisplayName(),
                        recharge.getRechargeAmount().toPlainString(),
                        encode(recharge.getRechargePlan()),
                        encode(recharge.getPaymentMethod()),
                        recharge.getRemainingBalance().toPlainString()
                ));
            }
            Files.write(storageFile, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to save recharge history.", ex);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}

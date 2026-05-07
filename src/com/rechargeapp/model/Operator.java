package com.rechargeapp.model;

import com.rechargeapp.util.ValidationException;
import java.util.Locale;

public enum Operator {
    JIO("Jio"),
    AIRTEL("Airtel"),
    VI("VI"),
    BSNL("BSNL");

    private final String displayName;

    Operator(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Operator fromDisplayName(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Please select a mobile operator.");
        }

        String normalized = value.trim().replace(" ", "").toUpperCase(Locale.ROOT);
        if ("JIO".equals(normalized)) {
            return JIO;
        }
        if ("AIRTEL".equals(normalized)) {
            return AIRTEL;
        }
        if ("VI".equals(normalized) || "VODAFONEIDEA".equals(normalized)) {
            return VI;
        }
        if ("BSNL".equals(normalized)) {
            return BSNL;
        }

        throw new ValidationException("Selected operator is not supported.");
    }
}

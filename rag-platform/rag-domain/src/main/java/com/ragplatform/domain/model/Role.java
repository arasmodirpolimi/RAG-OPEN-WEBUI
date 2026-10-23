package com.ragplatform.domain.model;

import java.util.Locale;

public enum Role {
    SYSTEM,
    USER,
    ASSISTANT;

    public static Role fromWire(String value) {
        return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public String wire() {
        return name().toLowerCase(Locale.ROOT);
    }
}

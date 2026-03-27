package pt.unl.fct.di.adc.firstwebapp.model;

import java.util.Locale;

public enum Role {
    USER, BOFFICER, ADMIN;
    public static Role fromString(String raw) {
        if (raw == null)
            throw new IllegalArgumentException("null role");
        return Role.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}

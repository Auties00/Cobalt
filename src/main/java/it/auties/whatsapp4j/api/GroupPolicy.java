package it.auties.whatsapp4j.api;

import org.jetbrains.annotations.NotNull;

public enum GroupPolicy {
    ANYONE,
    ADMINS;

    public @NotNull String data() {
        return String.valueOf(this == ADMINS);
    }
}
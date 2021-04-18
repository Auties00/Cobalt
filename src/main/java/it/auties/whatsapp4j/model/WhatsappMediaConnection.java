package it.auties.whatsapp4j.model;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public final class WhatsappMediaConnection {
    private final @NotNull String auth;
    private final int ttl;

    public WhatsappMediaConnection(@NotNull String auth, int ttl) {
        this.auth = auth;
        this.ttl = ttl;
    }

    public @NotNull String auth() {
        return auth;
    }

    public int ttl() {
        return ttl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WhatsappMediaConnection) obj;
        return Objects.equals(this.auth, that.auth) &&
                this.ttl == that.ttl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(auth, ttl);
    }

    @Override
    public String toString() {
        return "WhatsappMediaConnection[" +
                "auth=" + auth + ", " +
                "ttl=" + ttl + ']';
    }


}
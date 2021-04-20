package it.auties.whatsapp4j.model;

import jakarta.validation.constraints.NotNull;

public record WhatsappMediaConnection(@NotNull String auth, int ttl) {
}
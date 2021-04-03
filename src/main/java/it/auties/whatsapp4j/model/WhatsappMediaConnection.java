package it.auties.whatsapp4j.model;

import org.jetbrains.annotations.NotNull;

public record WhatsappMediaConnection(@NotNull String auth, int ttl) {

}
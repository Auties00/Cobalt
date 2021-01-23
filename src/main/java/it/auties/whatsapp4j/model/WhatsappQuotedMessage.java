package it.auties.whatsapp4j.model;

import org.jetbrains.annotations.NotNull;

public record WhatsappQuotedMessage(@NotNull String text, @NotNull String sender) {
}

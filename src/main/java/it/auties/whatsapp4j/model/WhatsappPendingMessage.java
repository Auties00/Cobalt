package it.auties.whatsapp4j.model;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public record WhatsappPendingMessage(@NotNull WhatsappMessage message, @NotNull Consumer<Integer> callback) {
}

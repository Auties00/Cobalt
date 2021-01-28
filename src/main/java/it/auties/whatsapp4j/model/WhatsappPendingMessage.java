package it.auties.whatsapp4j.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

@RecordBuilder
@ToString
public record WhatsappPendingMessage(@NotNull WhatsappMessage message, @NotNull BiConsumer<WhatsappMessage, Integer> callback) {
}

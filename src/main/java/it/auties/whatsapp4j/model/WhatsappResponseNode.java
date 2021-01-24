package it.auties.whatsapp4j.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import it.auties.whatsapp4j.response.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RecordBuilder
public record WhatsappResponseNode(@NotNull String tag, @Nullable String description, @NotNull Response data) {

}

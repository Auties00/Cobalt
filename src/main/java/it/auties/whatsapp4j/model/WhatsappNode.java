package it.auties.whatsapp4j.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@RecordBuilder
@ToString(exclude = "content")
public record WhatsappNode(@NotNull String description, @NotNull Map<String, String> attrs, @Nullable Object content) {

}

package it.auties.whatsapp4j.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@RecordBuilder
public record WhatsappNode(@NotNull String description, @NotNull Map<String, String> attrs, @Nullable Object content) {
    @Override
    public String toString() {
        return "WhatsappNode{description='" + description + '\'' +
                ", attrs=" + attrs +
                "}";
    }
}

package it.auties.whatsapp4j.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record Node(@NotNull String description, @NotNull Map<String, String> attributes, @Nullable Object content) {
}

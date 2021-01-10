package it.auties.whatsapp4j.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record WhatsappNode(@NotNull String tag, @NotNull String description, @NotNull String attrs, @Nullable String content) {

}

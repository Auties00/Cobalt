package it.auties.whatsapp4j.response;

import io.soabase.recordbuilder.core.RecordBuilder;
import it.auties.whatsapp4j.response.Response;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RecordBuilder
@ToString
public record ResponseNode(@NotNull String tag, @Nullable String description, @NotNull Response data) {

}

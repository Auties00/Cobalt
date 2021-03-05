package it.auties.whatsapp4j.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RecordBuilder
@ToString
public record WhatsappNode(@NotNull String description, @NotNull Map<String, String> attrs, @Nullable Object content) {
    public static @NotNull List<WhatsappNode> fromGenericList(@NotNull List<?> list){
        return list.stream()
                .filter(entry -> entry instanceof WhatsappNode)
                .map(WhatsappNode.class::cast)
                .collect(Collectors.toUnmodifiableList());
    }
}

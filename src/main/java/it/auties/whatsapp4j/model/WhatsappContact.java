package it.auties.whatsapp4j.model;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
@ToString
public class WhatsappContact {
    private final @NotNull String jid;
    private final @Nullable String chosenName;
    private final @Nullable String name;
    private final @Nullable String shortName;
    private @Nullable WhatsappContactStatus lastKnownPresence;
    private @Nullable ZonedDateTime lastSeen;
    public static @NotNull WhatsappContact fromAttributes(@NotNull Map<String, String> attrs){
        return WhatsappContact.builder()
                .jid(attrs.get("jid"))
                .name(attrs.get("name"))
                .chosenName(attrs.get("notify"))
                .shortName(attrs.get("short"))
                .build();
    }

    public @NotNull Optional<String> bestName() {
        return Optional.ofNullable(name != null ? name : chosenName);
    }

    public @NotNull String bestName(@NotNull String orElse) {
        return bestName().orElse(orElse);
    }


    public @NotNull Optional<WhatsappContactStatus> lastKnownPresence(){
        return Optional.ofNullable(lastKnownPresence);
    }

    public @NotNull Optional<ZonedDateTime> lastSeen(){
        return Optional.ofNullable(lastSeen);
    }
}

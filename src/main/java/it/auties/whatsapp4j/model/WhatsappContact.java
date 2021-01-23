package it.auties.whatsapp4j.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@AllArgsConstructor
@Builder
@Data
@Accessors(fluent = true)
@ToString
public class WhatsappContact{
    private @NotNull String jid;
    private @Nullable String chosenName;
    private @Nullable String name;
    private @Nullable String shortName;
    private @Nullable String profilePicture;

    public @Nullable String bestName(){
        return name != null ? name : shortName != null ? shortName : chosenName != null ? chosenName : null;
    }
}

package it.auties.whatsapp4j.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@AllArgsConstructor
@Builder
@Data
@Accessors(fluent = true)
public class WhatsappContact{
    private @NotNull String jid;
    private @Nullable String chosenName;
    private @Nullable String name;
    private @Nullable String shortName;
    private @Nullable String profilePicture;

    public @Nullable String bestName(){
        if(name != null) return name;
        if(shortName != null) return shortName;
        return chosenName != null ? chosenName : null;
    }

    public @NotNull String bestName(@NotNull String def){
        return Optional.ofNullable(bestName()).orElse(def);
    }

    @Override
    public String toString() {
        return "WhatsappContact{" +
                "jid='" + jid + '\'' +
                ", chosenName='" + chosenName + '\'' +
                ", name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                '}';
    }
}

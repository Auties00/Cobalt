package it.auties.whatsapp.protobuf.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents the wallpaper of a chat.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class ChatWallpaper{
    /**
     * The name of the file used as wallpaper
     */
    @JsonProperty("1")
    @JsonPropertyDescription("string")
    private String filename;

    /**
     * The opacity of the wallpaper
     */
    @JsonProperty("2")
    @JsonPropertyDescription("uint32")
    private int opacity;
}

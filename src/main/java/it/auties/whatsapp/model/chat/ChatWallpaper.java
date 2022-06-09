package it.auties.whatsapp.model.chat;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

/**
 * A model class that represents the wallpaper of a chat.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ChatWallpaper implements ProtobufMessage {
    /**
     * The name of the file used as wallpaper
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String filename;

    /**
     * The opacity of the wallpaper
     */
    @ProtobufProperty(index = 2, type = UINT32)
    private int opacity;
}

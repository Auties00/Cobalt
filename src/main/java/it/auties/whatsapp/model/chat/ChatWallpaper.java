package it.auties.whatsapp.model.chat;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT32;

/**
 * A model class that represents the wallpaper of a chat.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ChatWallpaper
        implements ProtobufMessage {
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

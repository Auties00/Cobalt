package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.chat.ChatMediaVisibility;
import it.auties.whatsapp.model.chat.ChatWallpaper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class GlobalSettings implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = ChatWallpaper.class)
    private ChatWallpaper lightThemeWallpaper;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ChatMediaVisibility.class)
    private ChatMediaVisibility mediaVisibility;

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ChatWallpaper.class)
    private ChatWallpaper darkThemeWallpaper;
}

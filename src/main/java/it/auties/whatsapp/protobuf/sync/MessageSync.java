package it.auties.whatsapp.protobuf.sync;

import lombok.NonNull;

public record MessageSync(String chatJid, String messageId, boolean fromMe) {
    public static MessageSync ofJson(@NonNull String json){
        throw new UnsupportedOperationException(json);
    }
}

package it.auties.whatsapp4j.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

@AllArgsConstructor
@Accessors(fluent = true)
public enum MessageType {
    IMAGE("WhatsApp Image Keys"),
    STICKER("WhatsApp Image Keys"),
    VIDEO("WhatsApp Video Keys"),
    AUDIO("WhatsApp Audio Keys"),
    DOCUMENT("WhatsApp Document Keys");

    @Getter
    private final String data;

    public static Optional<MessageType> forName(@NotNull String tag){
        return Arrays.stream(values()).filter(entry -> entry.name().equals(tag)).findAny();
    }
}

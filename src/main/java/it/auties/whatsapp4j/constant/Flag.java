package it.auties.whatsapp4j.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

@AllArgsConstructor
@Accessors(fluent = true)
public enum Flag {
    IGNORE(1 << 7),
    ACK_REQUEST(1 << 6),
    AVAILABLE(1 << 5),
    NOT_AVAILABLE(1 << 4),
    EXPIRES(1 << 3),
    SKIP_OFFLINE(1 << 2);

    @Getter
    private final int data;

    public static Optional<Flag> forName(@NotNull String tag){
        return Arrays.stream(values()).filter(entry -> entry.name().equals(tag)).findAny();
    }
}

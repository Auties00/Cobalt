package it.auties.whatsapp4j.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp4j.binary.BinaryFlag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@AllArgsConstructor
@Accessors(fluent = true)
public enum WhatsappContactStatus {
    AVAILABLE(BinaryFlag.AVAILABLE),
    UNAVAILABLE(BinaryFlag.UNAVAILABLE),
    COMPOSING(BinaryFlag.COMPOSING),
    RECORDING(BinaryFlag.RECORDING),
    PAUSED(BinaryFlag.PAUSED);

    @Getter
    private final BinaryFlag flag;

    public @NotNull String data() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static WhatsappContactStatus forValue(@NotNull String jsonValue) {
        return Arrays.stream(values()).filter(entry -> entry.name().equalsIgnoreCase(jsonValue)).findFirst().orElseThrow();
    }
}

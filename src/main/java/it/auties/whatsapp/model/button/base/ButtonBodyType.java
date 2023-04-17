package it.auties.whatsapp.model.button.base;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@Accessors(fluent = true)
@ProtobufName("HydratedButtonType")
public enum ButtonBodyType implements ProtobufMessage {
    UNKNOWN(0),
    TEXT(1),
    NATIVE_FLOW(2);

    @Getter
    private final int index;

    @JsonCreator
    public static ButtonBodyType of(int index) {
        return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
    }
}

package it.auties.whatsapp.util;

import it.auties.protobuf.annotation.ProtobufConverter;
import it.auties.protobuf.annotation.ProtobufMixin;

import java.util.Objects;
import java.util.UUID;

public class ProtobufUuidMixin implements ProtobufMixin {
    @ProtobufConverter
    public static UUID of(String uuid) {
        return UUID.fromString(uuid);
    }

    @ProtobufConverter
    public static String toValue(UUID uuid) {
        return Objects.toString(uuid);
    }
}

package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.util.Objects;

public record SessionAddress(String name, int id) {
    @JsonCreator
    @ProtobufDeserializer
    public static SessionAddress of(String serialized) {
        var split = serialized.split(":", 2);
        if (split.length != 2) {
            throw new IllegalArgumentException("Malformed address: " + serialized);
        }
        return new SessionAddress(split[0], Integer.parseInt(split[1]));
    }

    @JsonValue
    @ProtobufSerializer
    @Override
    public String toString() {
        return "%s:%s".formatted(name(), id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
}

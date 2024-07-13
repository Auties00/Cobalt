package it.auties.whatsapp.model.signal.sender;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.whatsapp.model.signal.session.SessionAddress;

public record SenderKeyName(String groupId, SessionAddress sender) {
    @JsonCreator
    @ProtobufDeserializer
    public static SenderKeyName of(String serialized) {
        var split = serialized.split("::", 3);
        var address = new SessionAddress(split[1], Integer.parseUnsignedInt(split[2]));
        return new SenderKeyName(split[0], address);
    }

    @JsonValue
    @ProtobufSerializer
    @Override
    public String toString() {
        return "%s::%s::%s".formatted(groupId(), sender().name(), sender().id());
    }
}

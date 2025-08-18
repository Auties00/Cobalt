package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.signal.session.SessionAddress;

public record SenderKeyName(Jid groupJid, SessionAddress sender) {
    @ProtobufDeserializer
    public static SenderKeyName of(String serialized) {
        var split = serialized.split("::", 3);
        var groupJid = Jid.of(split[0]);
        var address = new SessionAddress(split[1], Integer.parseUnsignedInt(split[2]));
        return new SenderKeyName(groupJid, address);
    }

    @ProtobufSerializer
    @Override
    public String toString() {
        return "%s::%s::%s".formatted(groupJid(), sender().name(), sender().id());
    }
}

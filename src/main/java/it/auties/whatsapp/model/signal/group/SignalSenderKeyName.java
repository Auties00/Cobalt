package it.auties.whatsapp.model.signal.group;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.signal.SignalAddress;

public record SignalSenderKeyName(Jid groupJid, SignalAddress sender) {
    @ProtobufDeserializer
    public static SignalSenderKeyName of(String serialized) {
        var split = serialized.split("::", 3);
        var groupJid = Jid.of(split[0]);
        var address = new SignalAddress(split[1], Integer.parseUnsignedInt(split[2]));
        return new SignalSenderKeyName(groupJid, address);
    }

    @ProtobufSerializer
    @Override
    public String toString() {
        return "%s::%s::%s".formatted(groupJid, sender.name(), sender.id());
    }
}

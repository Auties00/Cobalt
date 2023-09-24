package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.BOOL;
import static it.auties.protobuf.model.ProtobufType.OBJECT;

@ProtobufMessageName("ClientPayload.DNSSource")
public record DNSSource(@ProtobufProperty(index = 15, type = OBJECT) ResolutionMethod dnsMethod,
                        @ProtobufProperty(index = 16, type = BOOL) boolean appCached) implements ProtobufMessage {

    @ProtobufMessageName("ClientPayload.DNSSource.DNSResolutionMethod")
    public enum ResolutionMethod implements ProtobufEnum {

        SYSTEM(0),
        GOOGLE(1),
        HARDCODED(2),
        OVERRIDE(3),
        FALLBACK(4);

        ResolutionMethod(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }
}

package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.BOOL;
import static it.auties.protobuf.model.ProtobufType.ENUM;

@ProtobufMessage(name = "ClientPayload.DNSSource")
public record DNSSource(@ProtobufProperty(index = 15, type = ENUM) ResolutionMethod dnsMethod,
                        @ProtobufProperty(index = 16, type = BOOL) boolean appCached) {

    @ProtobufEnum(name = "ClientPayload.DNSSource.DNSResolutionMethod")
    public enum ResolutionMethod {
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

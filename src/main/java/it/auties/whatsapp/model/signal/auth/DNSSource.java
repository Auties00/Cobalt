package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.BOOL;
import static it.auties.protobuf.model.ProtobufType.OBJECT;

public record DNSSource(@ProtobufProperty(index = 15, type = OBJECT) DNSSourceDNSResolutionMethod dnsMethod,
                        @ProtobufProperty(index = 16, type = BOOL) boolean appCached) implements ProtobufMessage {

    public enum DNSSourceDNSResolutionMethod implements ProtobufEnum {

        SYSTEM(0),
        GOOGLE(1),
        HARDCODED(2),
        OVERRIDE(3),
        FALLBACK(4);

        DNSSourceDNSResolutionMethod(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }
}

package it.auties.whatsapp.model.newsletter;

import io.avaje.jsonb.Json;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

@ProtobufEnum
public enum NewsletterViewerRole {
    UNKNOWN(0),
    OWNER(1),
    SUBSCRIBER(2),
    ADMIN(3),
    GUEST(4);

    final int index;

    NewsletterViewerRole(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    @Json.Value
    @Override
    public String toString() {
        return name();
    }
}

package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

import java.util.Arrays;

public enum NewsletterViewerRole implements ProtobufEnum {
    UNKNOWN(0),
    OWNER(1),
    SUBSCRIBER(2),
    ADMIN(3),
    GUEST(4);

    final int index;
    NewsletterViewerRole(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public static NewsletterViewerRole of(int index) {
        return index >= values().length ? UNKNOWN : values()[index];
    }

    public static NewsletterViewerRole of(String name) {
        return Arrays.stream(values())
                .filter(entry -> entry.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
    }

    @JsonValue
    @Override
    public String toString() {
        return name();
    }
}

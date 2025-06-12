package it.auties.whatsapp.model.newsletter;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@ProtobufEnum
public enum NewsletterViewerRole {
    UNKNOWN(0),
    OWNER(1),
    SUBSCRIBER(2),
    ADMIN(3),
    GUEST(4);

    final int index;

    private static final Map<String, NewsletterViewerRole> BY_NAME = Arrays.stream(NewsletterViewerRole.values())
            .collect(Collectors.toMap(entry -> entry.name().toLowerCase(), role -> role));

    private static final Map<Integer, NewsletterViewerRole> BY_INDEX = Arrays.stream(NewsletterViewerRole.values())
            .collect(Collectors.toMap(entry -> entry.index, role -> role));

    static NewsletterViewerRole of(String name) {
        return name == null ? UNKNOWN : BY_NAME.getOrDefault(name.toLowerCase(), UNKNOWN);
    }

    static NewsletterViewerRole of(Integer index) {
        return index == null ? UNKNOWN : BY_INDEX.getOrDefault(index, UNKNOWN);
    }

    NewsletterViewerRole(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return name();
    }
}

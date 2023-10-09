package it.auties.whatsapp.model.newsletter;

import java.util.Arrays;

public enum NewsletterViewerRole {
    UNKNOWN,
    OWNER,
    SUBSCRIBER,
    ADMIN,
    GUEST;

    public static NewsletterViewerRole of(String name) {
        return Arrays.stream(values())
                .filter(entry -> entry.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
    }
}

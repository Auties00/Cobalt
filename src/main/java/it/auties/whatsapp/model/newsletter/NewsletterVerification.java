package it.auties.whatsapp.model.newsletter;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.util.Optional;

public class NewsletterVerification {
    private static final String ENABLED_JSON_VALUE = "ON";
    private static final String DISABLED_JSON_VALUE = "OFF";

    private static final NewsletterVerification ENABLED = new NewsletterVerification(true);
    private static final NewsletterVerification DISABLED = new NewsletterVerification(false);

    private final boolean verified;

    NewsletterVerification(boolean verified) {
        this.verified = verified;
    }

    public static NewsletterVerification enabled() {
        return ENABLED;
    }

    public static NewsletterVerification disabled() {
        return DISABLED;
    }

    static Optional<NewsletterVerification> ofJson(String value) {
        if(value == null) {
            return Optional.empty();
        }

        var result = ENABLED_JSON_VALUE.equals(value) ? ENABLED : DISABLED;
        return Optional.of(result);
    }

    @ProtobufDeserializer
    static NewsletterVerification deserialize(String value) {
        return ENABLED_JSON_VALUE.equals(value) ? ENABLED : DISABLED;
    }

    @ProtobufSerializer
    String serialize() {
        return verified ? ENABLED_JSON_VALUE : DISABLED_JSON_VALUE;
    }

    public boolean verified() {
        return verified;
    }
}

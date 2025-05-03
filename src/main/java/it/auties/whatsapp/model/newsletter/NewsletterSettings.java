package it.auties.whatsapp.model.newsletter;

import io.avaje.jsonb.Json;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage
@Json
public final class NewsletterSettings {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    @Json.Property("reaction_codes")
    final NewsletterReactionSettings reactionCodes;

    NewsletterSettings(NewsletterReactionSettings reactionCodes) {
        this.reactionCodes = Objects.requireNonNull(reactionCodes, "reactionCodes cannot be null");
    }

    public NewsletterReactionSettings reactionCodes() {
        return reactionCodes;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterSettings that
                && Objects.equals(reactionCodes, that.reactionCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reactionCodes);
    }

    @Override
    public String toString() {
        return "NewsletterSettings[" +
                "reactionCodes=" + reactionCodes +
                ']';
    }
}
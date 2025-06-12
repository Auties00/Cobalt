package it.auties.whatsapp.model.newsletter;

import com.alibaba.fastjson2.JSONObject;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public final class NewsletterSettings {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final NewsletterReactionSettings reactionCodes;

    NewsletterSettings(NewsletterReactionSettings reactionCodes) {
        this.reactionCodes = Objects.requireNonNull(reactionCodes, "reactionCodes cannot be null");
    }

    public static Optional<NewsletterSettings> ofJson(JSONObject jsonObject) {
        if(jsonObject == null) {
            return Optional.empty();
        }

        var reactionCodes = NewsletterReactionSettings.ofJson(jsonObject.getJSONObject("reaction_codes"));
        if(reactionCodes.isEmpty()) {
            return Optional.empty();
        }

        var result = new NewsletterSettings(reactionCodes.get());
        return Optional.of(result);
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
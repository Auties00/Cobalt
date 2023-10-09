package it.auties.whatsapp.model.info;

import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.newsletter.NewsletterReaction;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Json;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;

public record NewsletterMessageInfo(String id, OptionalLong timestampSeconds, OptionalLong views, Collection<NewsletterReaction> reactions, MessageContainer container) {
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds.orElse(0L));
    }

    /**
     * Converts this message to a json. Useful when debugging.
     *
     * @return a non-null string
     */
    public String toJson() {
        return Json.writeValueAsString(this, true);
    }
}

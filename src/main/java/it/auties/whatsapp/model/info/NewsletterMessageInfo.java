package it.auties.whatsapp.model.info;

import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.newsletter.NewsletterReaction;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;

public record NewsletterMessageInfo(String id, long timestampSeconds, long views, Collection<NewsletterReaction> reactions, MessageContainer container) {
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }
}

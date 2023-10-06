package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.util.ConcurrentDoublyLinkedList;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public record Newsletter(
        Jid jid,
        NewsletterState state,
        NewsletterMetadata metadata,
        Optional<NewsletterViewerMetadata> viewerMetadata,
        @JsonIgnore Collection<NewsletterMessageInfo> messages
) implements JidProvider {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Newsletter(
            @JsonProperty("id") Jid jid,
            @JsonProperty("state") NewsletterState state,
            @JsonProperty("thread_metadata") NewsletterMetadata metadata,
            @JsonProperty("viewer_metadata") NewsletterViewerMetadata viewerMetadata,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = ConcurrentDoublyLinkedList.class) Collection<NewsletterMessageInfo> messages
    ) {
        this(jid, state, metadata, Optional.ofNullable(viewerMetadata), Objects.requireNonNullElseGet(messages, ConcurrentDoublyLinkedList::new));
    }

    @Override
    @NonNull
    public Jid toJid() {
        return jid;
    }

    public void addMessages(Collection<NewsletterMessageInfo> messages) {
        this.messages.addAll(messages);
    }

    @Override
    public Collection<NewsletterMessageInfo> messages() {
        return Collections.unmodifiableCollection(messages);
    }
}
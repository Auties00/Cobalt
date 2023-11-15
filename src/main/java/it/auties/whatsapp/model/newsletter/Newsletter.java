package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.util.ConcurrentDoublyLinkedHashedDequeue;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public final class Newsletter implements JidProvider {
    private final Jid jid;
    private NewsletterState state;
    private NewsletterMetadata metadata;
    private final NewsletterViewerMetadata viewerMetadata;
    private final ConcurrentDoublyLinkedHashedDequeue<NewsletterMessageInfo> messages;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    Newsletter(
            @JsonProperty("id")
            Jid jid,
            @JsonProperty("state")
            NewsletterState state,
            @JsonProperty("thread_metadata")
            NewsletterMetadata metadata,
            @JsonProperty("viewer_metadata")
            NewsletterViewerMetadata viewerMetadata,
            @JsonProperty("messages")
            ConcurrentDoublyLinkedHashedDequeue<NewsletterMessageInfo> messages
    ) {
        this.jid = jid;
        this.state = state;
        this.metadata = metadata;
        this.viewerMetadata = viewerMetadata;
        this.messages = Objects.requireNonNullElseGet(messages, ConcurrentDoublyLinkedHashedDequeue::new);
    }

    public Newsletter(Jid jid, NewsletterState state, NewsletterMetadata metadata, NewsletterViewerMetadata viewerMetadata) {
        this.jid = jid;
        this.state = state;
        this.metadata = metadata;
        this.viewerMetadata = viewerMetadata;
        this.messages = new ConcurrentDoublyLinkedHashedDequeue<>();
    }

    public void addMessage(NewsletterMessageInfo message) {
        this.messages.add(message);
    }

    public boolean removeMessage(NewsletterMessageInfo message) {
        return this.messages.remove(message);
    }

    public void addMessages(Collection<NewsletterMessageInfo> messages) {
        this.messages.addAll(messages);
    }

    public Collection<NewsletterMessageInfo> messages() {
        return Collections.unmodifiableCollection(messages);
    }

    public Optional<NewsletterMessageInfo> oldestMessage() {
        return Optional.ofNullable(messages.peekFirst());
    }

    public Optional<NewsletterMessageInfo> newestMessage() {
        return Optional.ofNullable(messages.peekLast());
    }

    @Override
    public Jid toJid() {
        return jid;
    }

    public Jid jid() {
        return jid;
    }

    public Optional<NewsletterState> state() {
        return Optional.ofNullable(state);
    }

    public Newsletter setState(NewsletterState state) {
        this.state = state;
        return this;
    }

    public Newsletter setMetadata(NewsletterMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public NewsletterMetadata metadata() {
        return metadata;
    }

    public Optional<NewsletterViewerMetadata> viewerMetadata() {
        return Optional.ofNullable(viewerMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Newsletter that && Objects.equals(this.jid(), that.jid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid);
    }
}
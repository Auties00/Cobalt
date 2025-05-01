package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.util.ConcurrentLinkedSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public final class Newsletter implements JidProvider {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final Jid jid;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private NewsletterState state;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private NewsletterMetadata metadata;
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private final NewsletterViewerMetadata viewerMetadata;
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    private final ConcurrentLinkedSet<NewsletterMessageInfo> messages;

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
            ConcurrentLinkedSet<NewsletterMessageInfo> messages
    ) {
        this.jid = jid;
        this.state = state;
        this.metadata = metadata;
        this.viewerMetadata = viewerMetadata;
        this.messages = Objects.requireNonNullElseGet(messages, ConcurrentLinkedSet::new);
    }

    public Newsletter(Jid jid, NewsletterState state, NewsletterMetadata metadata, NewsletterViewerMetadata viewerMetadata) {
        this.jid = jid;
        this.state = state;
        this.metadata = metadata;
        this.viewerMetadata = viewerMetadata;
        this.messages = new ConcurrentLinkedSet<>();
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

    public Optional<NewsletterMetadata> metadata() {
        return Optional.ofNullable(metadata);
    }

    public Optional<NewsletterViewerMetadata> viewerMetadata() {
        return Optional.ofNullable(viewerMetadata);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Newsletter that &&
                Objects.equals(jid, that.jid) &&
                Objects.equals(state, that.state) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(viewerMetadata, that.viewerMetadata) &&
                Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, state, metadata, viewerMetadata, messages);
    }

    @Override
    public String toString() {
        return "Newsletter{" +
                "jid=" + jid +
                ", state=" + state +
                ", metadata=" + metadata +
                ", viewerMetadata=" + viewerMetadata +
                ", messages=" + messages +
                '}';
    }
}
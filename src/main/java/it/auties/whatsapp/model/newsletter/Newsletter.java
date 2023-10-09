package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.util.MessagesSet;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public final class Newsletter implements JidProvider {
    private final Jid jid;
    private final NewsletterState state;
    private final NewsletterMetadata metadata;
    private final NewsletterViewerMetadata viewerMetadata;
    private final MessagesSet<NewsletterMessageInfo> messages;

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
            MessagesSet<NewsletterMessageInfo> messages
    ) {
        this.jid = jid;
        this.state = state;
        this.metadata = metadata;
        this.viewerMetadata = viewerMetadata;
        this.messages = Objects.requireNonNullElseGet(messages, MessagesSet::new);
    }

    public Newsletter(Jid jid, NewsletterState state, NewsletterMetadata metadata, NewsletterViewerMetadata viewerMetadata) {
        this.jid = jid;
        this.state = state;
        this.metadata = metadata;
        this.viewerMetadata = viewerMetadata;
        this.messages = new MessagesSet<>();
    }

    public void addMessage(NewsletterMessageInfo message) {
        this.messages.add(message);
    }

    public void addMessages(Collection<NewsletterMessageInfo> messages) {
        this.messages.addAll(messages);
    }

    public Collection<NewsletterMessageInfo> messages() {
        return Collections.unmodifiableCollection(messages);
    }

    @Override
    @NonNull
    public Jid toJid() {
        return jid;
    }

    public Jid jid() {
        return jid;
    }

    public Optional<NewsletterState> state() {
        return Optional.ofNullable(state);
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
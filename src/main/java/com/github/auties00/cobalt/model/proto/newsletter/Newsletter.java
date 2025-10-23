package com.github.auties00.cobalt.model.proto.newsletter;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.model.proto.info.MessageInfoParent;
import com.github.auties00.cobalt.model.proto.info.NewsletterMessageInfo;
import com.github.auties00.cobalt.model.proto.jid.Jid;
import com.github.auties00.collections.ConcurrentLinkedHashMap;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;

// TODO: Add unreadMessagesCount and timestamp
@ProtobufMessage
public final class Newsletter implements MessageInfoParent {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    NewsletterState state;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    NewsletterMetadata metadata;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final NewsletterViewerMetadata viewerMetadata;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final ConcurrentLinkedHashMap<String, NewsletterMessageInfo> messages;

    Newsletter(Jid jid, NewsletterState state, NewsletterMetadata metadata, NewsletterViewerMetadata viewerMetadata, ConcurrentLinkedHashMap<String, NewsletterMessageInfo> messages) {
        this.jid = Objects.requireNonNull(jid, "value cannot be null");
        this.state = state;
        this.metadata = metadata;
        this.viewerMetadata = viewerMetadata;
        this.messages = Objects.requireNonNullElseGet(messages, ConcurrentLinkedHashMap::new);
    }

    public static Optional<Newsletter> ofJson(JSONObject newsletter) {
        if(newsletter == null) {
            return Optional.empty();
        }

        var jidValue = newsletter.getString("id");
        if(jidValue == null) {
            return Optional.empty();
        }

        var jid = Jid.of(jidValue);
        var stateJsonObject = newsletter.getJSONObject("state");
        var state = NewsletterState.ofJson(stateJsonObject)
                .orElse(null);
        var metadataJsonObject = newsletter.getJSONObject("thread_metadata");
        var metadata = NewsletterMetadata.ofJson(metadataJsonObject)
                .orElse(null);
        var viewerMetadataJsonObject = newsletter.getJSONObject("viewer_metadata");
        var viewerMetadata = NewsletterViewerMetadata.ofJson(viewerMetadataJsonObject)
                .orElse(null);
        var messagesJsonObjects = newsletter.getJSONArray("messages");
        var messages = new ConcurrentLinkedHashMap<String, NewsletterMessageInfo>();
        if(messagesJsonObjects != null) {
            for (var i = 0; i < messagesJsonObjects.size(); i++) {
                var messageJsonObject = messagesJsonObjects.getJSONObject(i);
                var message = NewsletterMessageInfo.ofJson(messageJsonObject);
                message.ifPresent(newsletterMessageInfo -> messages.put(newsletterMessageInfo.id(), newsletterMessageInfo));
            }
        }
        var result = new Newsletter(jid, state, metadata, viewerMetadata, messages);
        return Optional.of(result);
    }

    public void addMessage(NewsletterMessageInfo info) {
        Objects.requireNonNull(info, "info cannot be null");
        messages.put(info.id(), info);
    }

    @Override
    public boolean removeMessage(String messageId) {
        return messages.remove(messageId) != null;
    }

    @Override
    public void removeMessages() {
        messages.clear();
    }

    @Override
    public SequencedCollection<NewsletterMessageInfo> messages() {
        return messages.sequencedValues();
    }

    @Override
    public Optional<NewsletterMessageInfo> oldestMessage() {
        return Optional.ofNullable(messages.pollFirstEntry())
                .map(Map.Entry::getValue);
    }

    @Override
    public Optional<NewsletterMessageInfo> newestMessage() {
        return Optional.ofNullable(messages.pollLastEntry())
                .map(Map.Entry::getValue);
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

    public void setState(NewsletterState state) {
        this.state = state;
    }

    public void setMetadata(NewsletterMetadata metadata) {
        this.metadata = metadata;
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
                "value=" + jid +
                ", state=" + state +
                ", metadata=" + metadata +
                ", viewerMetadata=" + viewerMetadata +
                ", messages=" + messages +
                '}';
    }
}
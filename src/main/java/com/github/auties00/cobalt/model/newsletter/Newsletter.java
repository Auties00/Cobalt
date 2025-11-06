package com.github.auties00.cobalt.model.newsletter;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.cobalt.model.info.MessageInfoParent;
import com.github.auties00.cobalt.model.info.NewsletterMessageInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.collections.ConcurrentLinkedHashMap;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.*;

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
    final Messages messages;

    Newsletter(Jid jid, NewsletterState state, NewsletterMetadata metadata, NewsletterViewerMetadata viewerMetadata, Messages messages) {
        this.jid = Objects.requireNonNull(jid, "value cannot be null");
        this.state = state;
        this.metadata = metadata;
        this.viewerMetadata = viewerMetadata;
        this.messages = messages;
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
        var messages = new Messages();
        if(messagesJsonObjects != null) {
            for (var i = 0; i < messagesJsonObjects.size(); i++) {
                var messageJsonObject = messagesJsonObjects.getJSONObject(i);
                var message = NewsletterMessageInfo.ofJson(messageJsonObject);
                message.ifPresent(messages::add);
            }
        }
        var result = new Newsletter(jid, state, metadata, viewerMetadata, messages);
        return Optional.of(result);
    }

    public void addMessage(NewsletterMessageInfo info) {
        messages.add(info);
    }

    @Override
    public boolean removeMessage(String messageId) {
        return messages.removeById(messageId);
    }

    @Override
    public void removeMessages() {
        messages.clear();
    }

    @Override
    public SequencedCollection<NewsletterMessageInfo> messages() {
        return Collections.unmodifiableSequencedCollection(messages);
    }

    @Override
    public Optional<NewsletterMessageInfo> getMessageById(String messageId) {
        return messages.getById(messageId);
    }

    @Override
    public Optional<NewsletterMessageInfo> oldestMessage() {
        try {
            return Optional.of(messages.getFirst());
        }catch (NoSuchElementException e){
            return Optional.empty();
        }
    }

    @Override
    public Optional<NewsletterMessageInfo> newestMessage() {
        try {
            return Optional.of(messages.getLast());
        }catch (NoSuchElementException e){
            return Optional.empty();
        }
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

    static final class Messages implements SequencedCollection<NewsletterMessageInfo> {
        private final ConcurrentLinkedHashMap<String, NewsletterMessageInfo> backing;

        public Messages() {
            this.backing = new ConcurrentLinkedHashMap<>();
        }

        public Optional<NewsletterMessageInfo> getById(String id) {
            return Optional.ofNullable(backing.get(id));
        }

        public boolean removeById(String id) {
            return backing.remove(id) != null;
        }

        @Override
        public SequencedCollection<NewsletterMessageInfo> reversed() {
            return backing.sequencedValues()
                    .reversed();
        }

        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public boolean isEmpty() {
            return backing.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return o instanceof MessageInfo messageInfo
                   && backing.containsKey(messageInfo.id());
        }

        @Override
        public Iterator<NewsletterMessageInfo> iterator() {
            return backing.sequencedValues()
                    .iterator();
        }

        @Override
        public Object[] toArray() {
            return backing.sequencedValues()
                    .toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return backing.sequencedValues()
                    .toArray(a);
        }

        @Override
        public boolean add(NewsletterMessageInfo messageInfo) {
            Objects.requireNonNull(messageInfo);
            backing.put(messageInfo.id(), messageInfo);
            return true;
        }

        @Override
        public boolean remove(Object o) {
            return o instanceof MessageInfo messageInfo
                   && backing.remove(messageInfo.id()) != null;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            Objects.requireNonNull(collection);
            for(var entry : collection) {
                if (!(entry instanceof MessageInfo messageInfo)) {
                    return false;
                }

                if (!backing.containsKey(messageInfo.id())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends NewsletterMessageInfo> collection) {
            Objects.requireNonNull(collection);
            for(var entry : collection) {
                backing.put(entry.id(), entry);
            }
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            Objects.requireNonNull(collection);
            var result = true;
            for(var entry : collection) {
                if (!(entry instanceof MessageInfo messageInfo) || backing.remove(messageInfo.id()) == null) {
                    result = false;
                }
            }
            return result;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return false;
        }

        @Override
        public void clear() {
            backing.clear();
        }
    }
}
package com.github.auties00.cobalt.store.linked.protobuf.temporary;

import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.collections.ConcurrentLinkedHashMap;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The in-memory {@link Chat} subtype used as the value type of {@link TemporaryLinkedWhatsAppChatStore#chats}.
 *
 * @apiNote
 * Cobalt embedders never construct this directly; {@link TemporaryLinkedWhatsAppChatStore#addNewChat(Jid)} returns
 * one as a {@link Chat}. Every message accessor reads or mutates a single
 * {@link ConcurrentLinkedHashMap} held in this instance, so the chat history is lost the moment
 * the JVM exits.
 *
 * @implNote
 * This implementation keys messages by their message-id string and preserves insertion order via
 * the sequenced-map contract, so {@link #oldestMessage()} and {@link #newestMessage()} reduce to
 * {@link ConcurrentLinkedHashMap#firstEntry()} and
 * {@link ConcurrentLinkedHashMap#lastEntry()}.
 */
final class TemporaryChat extends Chat {
    /**
     * The in-memory message store, keyed by message id and preserving insertion order.
     */
    private final ConcurrentLinkedHashMap<String, ChatMessageInfo> messages;

    /**
     * Constructs an in-memory chat keyed under {@code jid} with every other metadata field
     * defaulted to {@code null}.
     *
     * @apiNote
     * Package-private; called by {@link TemporaryLinkedWhatsAppChatStore#addNewChat(Jid)}. The defaulted metadata
     * scalars are populated on demand as the receiver path observes the corresponding stanzas.
     *
     * @param jid the chat JID
     */
    TemporaryChat(Jid jid) {
        super(jid, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        this.messages = new ConcurrentLinkedHashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation streams the sequenced values directly; the stream does not own any
     * native resources and need not be closed explicitly, unlike the persistent variant.
     */
    @Override
    public Stream<ChatMessageInfo> messages() {
        return messages.sequencedValues().stream();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link ConcurrentLinkedHashMap#size()} which is {@code O(1)}.
     */
    @Override
    public int messageCount() {
        return messages.size();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation silently drops messages with no key id because the underlying map
     * requires a non-null key; production code paths always populate the id before insertion.
     */
    @Override
    public void addMessage(ChatMessageInfo info) {
        Objects.requireNonNull(info, "info cannot be null");
        info.key().id().ifPresent(id -> messages.put(id, info));
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@code false} on a {@code null} id rather than letting the
     * underlying map throw.
     */
    @Override
    public boolean removeMessage(String id) {
        return id != null && messages.remove(id) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation delegates to {@link ConcurrentLinkedHashMap#clear()}.
     */
    @Override
    public void removeMessages() {
        messages.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} on a {@code null} id.
     */
    @Override
    public Optional<ChatMessageInfo> getMessageById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(messages.get(id));
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the last entry of the sequenced map, which is the most
     * recently inserted message.
     */
    @Override
    public Optional<ChatMessageInfo> newestMessage() {
        var entry = messages.lastEntry();
        return entry == null ? Optional.empty() : Optional.of(entry.getValue());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the first entry of the sequenced map, which is the earliest
     * inserted message still present.
     */
    @Override
    public Optional<ChatMessageInfo> oldestMessage() {
        var entry = messages.firstEntry();
        return entry == null ? Optional.empty() : Optional.of(entry.getValue());
    }
}

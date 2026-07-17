package com.github.auties00.cobalt.store.linked.protobuf.temporary;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.newsletter.Newsletter;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.collections.ConcurrentLinkedHashMap;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The in-memory {@link Newsletter} subtype used as the value type of
 * {@link TemporaryLinkedWhatsAppChatStore#newsletters}.
 *
 * @apiNote
 * Cobalt embedders never construct this directly; {@link TemporaryLinkedWhatsAppChatStore#addNewNewsletter(Jid)}
 * returns one as a {@link Newsletter}. Mirrors {@link TemporaryChat} and keys the underlying map by the
 * {@link NewsletterMessageInfo} server id rendered as a string, the same server-id-string identifier the
 * abstract message API hands around; the persistent variant accepts the identical string but re-encodes it to a
 * fixed-width big-endian key for memcmp-ordered range scans.
 *
 * @implNote
 * This implementation keys by the server-id string and preserves insertion order via the
 * sequenced-map contract, so {@link #oldestMessage()} and {@link #newestMessage()} reduce to
 * sequenced-map first/last lookups.
 */
final class TemporaryNewsletter extends Newsletter {
    /**
     * The in-memory message store, keyed by the server id rendered as a string and preserving insertion order.
     */
    private final ConcurrentLinkedHashMap<String, NewsletterMessageInfo> messages;

    /**
     * Constructs an in-memory newsletter keyed under {@code jid} with every other metadata
     * field defaulted.
     *
     * @apiNote
     * Package-private; called by {@link TemporaryLinkedWhatsAppChatStore#addNewNewsletter(Jid)}. The defaulted
     * metadata scalars are populated on demand as the receiver path observes the corresponding
     * stanzas.
     *
     * @param jid the newsletter JID
     */
    TemporaryNewsletter(Jid jid) {
        super(jid, null, null, null, 0, null);
        this.messages = new ConcurrentLinkedHashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation keys by the {@code serverId} rendered as a string, so a re-put under the same server
     * id (an edit or add-on update) overwrites in place and {@link #messageCount()} counts distinct server ids
     * without inflation, matching the persistent variant.
     */
    @Override
    public void addMessage(NewsletterMessageInfo info) {
        Objects.requireNonNull(info, "info cannot be null");
        messages.put(String.valueOf(info.serverId()), info);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation removes by the server-id string key and returns {@code false} on a {@code null}
     * input; unlike the persistent variant no numeric parsing is required, because the in-memory map is keyed
     * by the same server-id string the abstract API passes rather than a fixed-width numeric key.
     */
    @Override
    public boolean removeMessage(String messageId) {
        return messageId != null && messages.remove(messageId) != null;
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
     * This implementation streams the sequenced values directly; the stream owns no native
     * resources.
     */
    @Override
    public Stream<NewsletterMessageInfo> messages() {
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
     * This implementation returns {@link Optional#empty()} on a {@code null} id and otherwise does a direct
     * hash lookup on the server-id string key; unlike the persistent variant no numeric parsing is required.
     */
    @Override
    public Optional<NewsletterMessageInfo> getMessageById(String messageId) {
        if (messageId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(messages.get(messageId));
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the first entry of the sequenced map, which is the earliest
     * inserted message still present.
     */
    @Override
    public Optional<NewsletterMessageInfo> oldestMessage() {
        var entry = messages.firstEntry();
        return entry == null ? Optional.empty() : Optional.of(entry.getValue());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the last entry of the sequenced map, which is the most
     * recently inserted message.
     */
    @Override
    public Optional<NewsletterMessageInfo> newestMessage() {
        var entry = messages.lastEntry();
        return entry == null ? Optional.empty() : Optional.of(entry.getValue());
    }
}

package com.github.auties00.cobalt.store.linked.protobuf.persistent;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.newsletter.Newsletter;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMetadata;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterState;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterViewerMetadata;
import it.auties.protobuf.annotation.ProtobufMessage;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * The metadata-only {@link Newsletter} subtype used as the value type of
 * {@link PersistentLinkedWhatsAppChatStore#newsletters}.
 *
 * @apiNote
 * Cobalt embedders never construct this directly; {@link PersistentLinkedWhatsAppChatStore#addNewNewsletter(Jid)}
 * returns one as a {@link Newsletter} and the protobuf builder produces one on deserialisation.
 * Every message accessor delegates to the owning store's {@link PersistentMessageStore} so that
 * newsletter bodies stay out of the protobuf snapshot and live in the MVStore instead.
 *
 * @implNote
 * This implementation mirrors {@link PersistentChat} but keys messages by {@code serverId} rather
 * than message-id; the message-id strings the abstract API accepts are parsed as numeric server
 * ids and a {@link NumberFormatException} surfaces as empty/{@code false}.
 */
@ProtobufMessage
final class PersistentNewsletter extends Newsletter {
    /**
     * The logger for {@link PersistentNewsletter}.
     */
    private static final System.Logger LOGGER = Log.get(PersistentNewsletter.class);

    /**
     * The MVStore facade backing every message accessor.
     *
     * @implNote
     * This implementation is wired by {@link #attach(PersistentMessageStore)} after construction
     * and is never serialised.
     */
    private volatile PersistentMessageStore messageStore;

    /**
     * The cached message count for this newsletter.
     *
     * @implNote
     * This implementation is seeded once by {@link #attach(PersistentMessageStore)} and maintained
     * incrementally on every add and successful remove.
     */
    private final AtomicInteger messageCount;

    /**
     * Constructs a metadata-only newsletter with the given protobuf-decoded fields.
     *
     * @apiNote
     * This constructor is package-private and intended for the generated
     * {@code PersistentNewsletterBuilder} and the protobuf deserialiser. Callers obtain instances
     * via {@link PersistentLinkedWhatsAppChatStore#addNewNewsletter(Jid)}.
     *
     * @param jid                  the newsletter JID
     * @param state                the newsletter state
     * @param metadata             the newsletter metadata block
     * @param viewerMetadata       the viewer-side metadata block
     * @param unreadMessagesCount  the cached unread message count
     * @param timestamp            the newsletter sort timestamp
     */
    PersistentNewsletter(Jid jid, NewsletterState state, NewsletterMetadata metadata, NewsletterViewerMetadata viewerMetadata, int unreadMessagesCount, Instant timestamp) {
        super(jid, state, metadata, viewerMetadata, unreadMessagesCount, timestamp);
        this.messageCount = new AtomicInteger();
    }

    /**
     * Sets the given MVStore facade on this newsletter and reseeds the cached message count.
     *
     * @apiNote
     * Invoked by {@link PersistentStore#setMessageStore(PersistentMessageStore)} immediately
     * after construction or deserialisation. Until this call returns, every message accessor
     * throws because {@link #messageStore} is still {@code null}.
     *
     * @implNote
     * This implementation walks {@link PersistentMessageStore#countNewsletterMessages(Jid)} once
     * so subsequent {@link #messageCount()} calls answer in {@code O(1)}.
     *
     * @param messageStore the MVStore facade owned by the parent store
     */
    void setMessageStore(PersistentMessageStore messageStore) {
        this.messageStore = messageStore;
        var count = messageStore.countNewsletterMessages(jid());
        this.messageCount.set(count);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "newsletter {0} message store set with {1} cached messages", jid(), count);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation delegates to
     * {@link PersistentMessageStore#putNewsletterMessage(Jid, NewsletterMessageInfo)} and increments
     * {@link #messageCount} only when the put inserted a new {@code serverId} key; a re-put under an existing
     * server id (an edit or add-on update) overwrites in place and leaves the count unchanged. The MVStore key
     * is {@code newsletterJid + 0x00 + info.serverId()}.
     */
    @Override
    public void addMessage(NewsletterMessageInfo info) {
        Objects.requireNonNull(info, "info cannot be null");
        if (messageStore.putNewsletterMessage(jid(), info)) {
            messageCount.incrementAndGet();
            if (Log.TRACE) LOGGER.log(Level.TRACE, "added newsletter message serverId={0} for {1}", info.serverId(), jid());
        } else if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "overwrote newsletter message serverId={0} for {1}", info.serverId(), jid());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation parses {@code messageId} as the numeric server id used as the MVStore key
     * suffix; a {@link NumberFormatException} returns {@code false} so callers that pass message
     * id strings (rather than server ids) get a clean no-op instead of an unchecked failure.
     */
    @Override
    public boolean removeMessage(String messageId) {
        if (messageId == null) {
            return false;
        }
        try {
            var serverId = Integer.parseInt(messageId);
            var removed = messageStore.removeNewsletterMessageByServerId(jid(), serverId);
            if (removed) {
                messageCount.decrementAndGet();
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "remove newsletter message {0} for {1} -> removed {2}", serverId, jid(), removed);
            return removed;
        } catch (NumberFormatException _) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "non-numeric newsletter message id {0}, ignoring remove", messageId);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation deletes every entry in the MVStore range
     * {@code [newsletterJid + 0x00, newsletterJid + 0x01)} and resets the cached counter to zero.
     */
    @Override
    public void removeMessages() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removing all newsletter messages for {0}", jid());
        messageStore.removeNewsletterMessages(jid());
        messageCount.set(0);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote
     * The returned stream walks a consistent MVStore snapshot and holds no native resources; callers
     * continue to consume it inside a try-with-resources block for parity with the rest of the message
     * API.
     */
    @Override
    public Stream<NewsletterMessageInfo> messages() {
        return messageStore.streamNewsletterMessages(jid());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the cached counter rather than re-walking MVStore.
     */
    @Override
    public int messageCount() {
        return messageCount.get();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation parses {@code messageId} as a numeric server id; a
     * {@link NumberFormatException} returns {@link Optional#empty()}.
     */
    @Override
    public Optional<NewsletterMessageInfo> getMessageById(String messageId) {
        if (messageId == null) {
            return Optional.empty();
        }
        try {
            return messageStore.getNewsletterMessageByServerId(jid(), Integer.parseInt(messageId));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the first entry in the per-newsletter MVStore range, which is the
     * lowest {@code serverId} because the big-endian encoding aligns memcmp order with numeric
     * order.
     */
    @Override
    public Optional<NewsletterMessageInfo> oldestMessage() {
        return messageStore.oldestNewsletterMessage(jid());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the last entry in the per-newsletter MVStore range, which is the
     * highest {@code serverId}.
     */
    @Override
    public Optional<NewsletterMessageInfo> newestMessage() {
        return messageStore.newestNewsletterMessage(jid());
    }
}

package com.github.auties00.cobalt.wire.cloud;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;

import java.time.Instant;
import java.util.Optional;

/**
 * The Cloud transport's message-info envelope.
 *
 * <p>This is the Cloud-native counterpart to the Linked transport's {@code LinkedMessageInfo}. It
 * carries the metadata a Cloud webhook exposes for a received or acknowledged message: the message
 * {@link MessageKey key}, the {@link CloudMessageContainer content envelope}, the delivery
 * {@link MessageStatus status}, the timestamp, and the sender identity and push name. It deliberately
 * omits the WhatsApp-Web-only fields the Linked variant carries (starred state, per-recipient
 * receipts, stub types, ephemeral and revoke bookkeeping), which the Cloud wire never delivers.
 */
public final class CloudMessageInfo implements MessageInfo {
    /**
     * The key identifying this message; never {@code null}.
     */
    private final MessageKey key;

    /**
     * The content envelope of this message; never {@code null}.
     */
    private final CloudMessageContainer message;

    /**
     * The delivery status, or {@code null} when the webhook reported none.
     */
    private final MessageStatus status;

    /**
     * The send or receive instant, or {@code null} when unknown.
     */
    private final Instant timestamp;

    /**
     * The sender JID, or {@code null} when not applicable (for example status updates keyed only by id).
     */
    private final Jid senderJid;

    /**
     * The sender's WhatsApp push name, or {@code null} when the webhook omitted it.
     */
    private final String pushName;

    /**
     * Constructs a Cloud message-info envelope.
     *
     * @param key       the message key
     * @param message   the content envelope, or {@code null} for an empty envelope
     * @param status    the delivery status, or {@code null}
     * @param timestamp the send or receive instant, or {@code null}
     * @param senderJid the sender JID, or {@code null}
     * @param pushName  the sender push name, or {@code null}
     */
    public CloudMessageInfo(MessageKey key, CloudMessageContainer message, MessageStatus status, Instant timestamp, Jid senderJid, String pushName) {
        this.key = key;
        this.message = message == null ? CloudMessageContainer.empty() : message;
        this.status = status;
        this.timestamp = timestamp;
        this.senderJid = senderJid;
        this.pushName = pushName;
    }

    @Override
    public MessageKey key() {
        return key;
    }

    @Override
    public MessageContainer message() {
        return message;
    }

    @Override
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    @Override
    public Optional<MessageStatus> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the sender JID, when the webhook carried one.
     *
     * @return an {@link Optional} holding the sender JID, or empty when none applies
     */
    public Optional<Jid> senderJid() {
        return Optional.ofNullable(senderJid);
    }

    /**
     * Returns the sender's WhatsApp push name, when the webhook carried one.
     *
     * @return an {@link Optional} holding the push name, or empty when the webhook omitted it
     */
    public Optional<String> pushName() {
        return Optional.ofNullable(pushName);
    }
}

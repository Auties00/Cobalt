package com.github.auties00.cobalt.wire.linked.message;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Per-recipient delivery, read, and play receipt information for a message.
 *
 * <p>Every time a recipient acknowledges a message, WhatsApp emits a
 * receipt that updates the sender about the current state of the
 * delivery for that recipient. In a one-to-one chat a message
 * typically accumulates a single {@code MessageReceipt} describing the
 * remote party's behaviour; in group chats one receipt is produced per
 * participant, and each receipt tracks every individual device the
 * participant owns.
 *
 * <p>A receipt records three independent milestones for each recipient:
 * <ul>
 *   <li>{@link #receiptTimestamp()} marks the moment the message was
 *       delivered (two grey checkmarks in the UI)</li>
 *   <li>{@link #readTimestamp()} marks the moment the recipient opened
 *       the chat and saw the message (two blue checkmarks in the UI)</li>
 *   <li>{@link #playedTimestamp()} marks the moment the recipient
 *       played a voice note or view-once media (applies only to audio
 *       and view-once content)</li>
 * </ul>
 *
 * <p>Because multi-device support is now the default on WhatsApp, the
 * receipt also tracks which of the recipient's devices are still
 * pending delivery ({@link #pendingDeviceJid()}) and which have
 * confirmed it ({@link #deliveredDeviceJid()}).
 */
@ProtobufMessage(name = "UserReceipt")
public final class MessageReceipt {
    /**
     * The JID of the recipient the receipt belongs to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid userJid;

    /**
     * The instant at which the recipient's primary device confirmed
     * delivery of the message, if any.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant receiptTimestamp;

    /**
     * The instant at which the recipient opened the chat and saw the
     * message, if any.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant readTimestamp;

    /**
     * The instant at which the recipient played a voice note or
     * view-once media attached to the message, if any.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant playedTimestamp;

    /**
     * The JIDs of the recipient's devices that have not yet confirmed
     * delivery of the message.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    List<Jid> pendingDeviceJid;

    /**
     * The JIDs of the recipient's devices that have confirmed delivery
     * of the message.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    List<Jid> deliveredDeviceJid;

    /**
     * Constructs a new {@code MessageReceipt} for the given recipient.
     *
     * <p>The constructor is package-private; use
     * {@code MessageReceiptBuilder} to instantiate new receipts.
     *
     * @param userJid            the recipient JID (required)
     * @param receiptTimestamp   the delivery timestamp, or {@code null}
     * @param readTimestamp      the read timestamp, or {@code null}
     * @param playedTimestamp    the played timestamp, or {@code null}
     * @param pendingDeviceJid   devices still awaiting delivery, or
     *                           {@code null}
     * @param deliveredDeviceJid devices that have confirmed delivery,
     *                           or {@code null}
     * @throws NullPointerException if {@code userJid} is {@code null}
     */
    MessageReceipt(Jid userJid, Instant receiptTimestamp, Instant readTimestamp, Instant playedTimestamp, List<Jid> pendingDeviceJid, List<Jid> deliveredDeviceJid) {
        this.userJid = Objects.requireNonNull(userJid);
        this.receiptTimestamp = receiptTimestamp;
        this.readTimestamp = readTimestamp;
        this.playedTimestamp = playedTimestamp;
        this.pendingDeviceJid = pendingDeviceJid;
        this.deliveredDeviceJid = deliveredDeviceJid;
    }

    /**
     * Returns the JID of the recipient this receipt belongs to.
     *
     * @return the non-{@code null} recipient JID
     */
    public Jid userJid() {
        return userJid;
    }

    /**
     * Returns the instant at which delivery was confirmed, if known.
     *
     * @return an {@link Optional} holding the delivery timestamp, or
     *         empty if no delivery has been confirmed yet
     */
    public Optional<Instant> receiptTimestamp() {
        return Optional.ofNullable(receiptTimestamp);
    }

    /**
     * Returns the instant at which the recipient read the message,
     * if known.
     *
     * @return an {@link Optional} holding the read timestamp, or empty
     *         if the message has not been read yet
     */
    public Optional<Instant> readTimestamp() {
        return Optional.ofNullable(readTimestamp);
    }

    /**
     * Returns the instant at which the recipient played a voice note
     * or view-once media, if known.
     *
     * @return an {@link Optional} holding the played timestamp, or
     *         empty if no playback has been confirmed yet
     */
    public Optional<Instant> playedTimestamp() {
        return Optional.ofNullable(playedTimestamp);
    }

    /**
     * Returns the JIDs of the recipient's devices that have not yet
     * confirmed delivery.
     *
     * @return an unmodifiable list of pending device JIDs, never
     *         {@code null}
     */
    public List<Jid> pendingDeviceJid() {
        return pendingDeviceJid == null ? List.of() : Collections.unmodifiableList(pendingDeviceJid);
    }

    /**
     * Returns the JIDs of the recipient's devices that have confirmed
     * delivery.
     *
     * @return an unmodifiable list of delivered device JIDs, never
     *         {@code null}
     */
    public List<Jid> deliveredDeviceJid() {
        return deliveredDeviceJid == null ? List.of() : Collections.unmodifiableList(deliveredDeviceJid);
    }

    /**
     * Updates the JID of the recipient.
     *
     * @param userJid the new recipient JID
     */
    public void setUserJid(Jid userJid) {
        this.userJid = userJid;
    }

    /**
     * Updates the delivery timestamp.
     *
     * @param receiptTimestamp the new delivery timestamp, or
     *                         {@code null} to clear
     */
    public void setReceiptTimestamp(Instant receiptTimestamp) {
        this.receiptTimestamp = receiptTimestamp;
    }

    /**
     * Updates the read timestamp.
     *
     * @param readTimestamp the new read timestamp, or {@code null}
     *                      to clear
     */
    public void setReadTimestamp(Instant readTimestamp) {
        this.readTimestamp = readTimestamp;
    }

    /**
     * Updates the played timestamp.
     *
     * @param playedTimestamp the new played timestamp, or {@code null}
     *                        to clear
     */
    public void setPlayedTimestamp(Instant playedTimestamp) {
        this.playedTimestamp = playedTimestamp;
    }

    /**
     * Updates the list of devices still awaiting delivery.
     *
     * @param pendingDeviceJid the new list, or {@code null} to clear
     */
    public void setPendingDeviceJid(List<Jid> pendingDeviceJid) {
        this.pendingDeviceJid = pendingDeviceJid;
    }

    /**
     * Updates the list of devices that have confirmed delivery.
     *
     * @param deliveredDeviceJid the new list, or {@code null} to clear
     */
    public void setDeliveredDeviceJid(List<Jid> deliveredDeviceJid) {
        this.deliveredDeviceJid = deliveredDeviceJid;
    }
}

package com.github.auties00.cobalt.wire.linked.bot;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model representing whether the welcome message has been requested for
 * a single AI bot conversation.
 *
 * <p>WhatsApp shows a one-shot welcome blurb the first time the user opens
 * a chat with an AI bot. Whether the welcome request has already been sent
 * is tracked per bot, so the blurb is shown exactly once per bot per
 * device. Each entry pairs the bot's {@linkplain #botJid() JID} with the
 * {@linkplain #requested() requested flag}.
 *
 * <p>Cobalt persists each entry independently so callers can resolve a
 * single bot's welcome state without iterating the whole map.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class BotWelcomeRequestState {
    /**
     * The non-{@code null} JID of the AI bot whose welcome state this entry
     * tracks. Used as the primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid botJid;

    /**
     * Whether the welcome message has already been requested for this bot.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean requested;

    /**
     * Constructs a new bot welcome-request state with the given bot JID and
     * requested flag.
     *
     * @param botJid    the non-{@code null} bot JID
     * @param requested whether the welcome message has been requested
     */
    BotWelcomeRequestState(Jid botJid, boolean requested) {
        this.botJid = Objects.requireNonNull(botJid, "botJid cannot be null");
        this.requested = requested;
    }

    /**
     * Returns the non-{@code null} JID of the bot whose welcome state this
     * entry tracks.
     *
     * @return the bot JID
     */
    public Jid botJid() {
        return botJid;
    }

    /**
     * Returns whether the welcome message has already been requested for
     * this bot.
     *
     * @return {@code true} if the welcome message has been requested
     */
    public boolean requested() {
        return requested;
    }

    /**
     * Updates the requested flag of this welcome state.
     *
     * @param requested the new requested flag
     * @return this welcome state instance for method chaining
     */
    public BotWelcomeRequestState setRequested(boolean requested) {
        this.requested = requested;
        return this;
    }

    /**
     * Returns a hash code derived from this state's
     * {@linkplain #botJid() bot JID}.
     *
     * @return the hash code of the bot JID
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(botJid);
    }

    /**
     * Returns whether this welcome state is equal to the given object.
     *
     * <p>Two welcome states are considered equal when they share the same
     * {@linkplain #botJid() bot JID}, regardless of the requested flag.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code BotWelcomeRequestState}
     *         with the same bot JID
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BotWelcomeRequestState that && Objects.equals(this.botJid, that.botJid);
    }
}

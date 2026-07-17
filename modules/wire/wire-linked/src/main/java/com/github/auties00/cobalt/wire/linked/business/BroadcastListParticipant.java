package com.github.auties00.cobalt.wire.linked.business;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model representing a single participant of a WhatsApp Business
 * broadcast list.
 *
 * <p>A {@linkplain BusinessBroadcastList broadcast list} carries an ordered
 * roster of participant entries describing who receives the broadcast.
 * Each entry pairs the participant's privacy-preserving
 * {@linkplain #lidJid() LID JID} — the form WhatsApp uses to address the
 * recipient inside broadcast flows — with the participant's
 * {@linkplain #pnJid() phone-number JID}, when the LID-to-phone mapping
 * is locally known.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class BroadcastListParticipant {
    /**
     * The non-{@code null} LID JID identifying the participant inside the
     * broadcast list.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid lidJid;

    /**
     * The participant's phone-number JID, or {@code null} when the
     * LID-to-phone mapping is not locally known.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    Jid pnJid;

    /**
     * Constructs a new broadcast-list participant with the given JIDs.
     *
     * @param lidJid the non-{@code null} LID JID
     * @param pnJid  the phone-number JID, or {@code null}
     */
    BroadcastListParticipant(Jid lidJid, Jid pnJid) {
        this.lidJid = Objects.requireNonNull(lidJid, "lidJid cannot be null");
        this.pnJid = pnJid;
    }

    /**
     * Returns the non-{@code null} LID JID of this participant.
     *
     * @return the LID JID
     */
    public Jid lidJid() {
        return lidJid;
    }

    /**
     * Returns the phone-number JID of this participant.
     *
     * @return an {@code Optional} containing the phone-number JID, or
     *         empty when the LID-to-phone mapping is not known
     */
    public Optional<Jid> pnJid() {
        return Optional.ofNullable(pnJid);
    }

    /**
     * Updates the phone-number JID of this participant.
     *
     * @param pnJid the new phone-number JID, or {@code null} to clear
     * @return this participant instance for method chaining
     */
    public BroadcastListParticipant setPnJid(Jid pnJid) {
        this.pnJid = pnJid;
        return this;
    }

    /**
     * Returns a hash code derived from this participant's
     * {@linkplain #lidJid() LID JID}.
     *
     * @return the hash code of the LID JID
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(lidJid);
    }

    /**
     * Returns whether this participant is equal to the given object.
     *
     * <p>Two participants are considered equal when they share the same
     * {@linkplain #lidJid() LID JID}, regardless of the phone-number
     * mapping.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code BroadcastListParticipant}
     *         with the same LID JID
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BroadcastListParticipant that && Objects.equals(this.lidJid, that.lidJid);
    }
}

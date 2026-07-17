package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A sync action entry describing a single participant of a business broadcast list.
 *
 * <p>Each participant record pairs the LID JID (the anonymised WhatsApp identifier used
 * to address the recipient in broadcast flows) with the participant's phone-number JID
 * when it is known. Instances of this action are embedded inside {@link BusinessBroadcastListAction}
 * to describe the full membership of a business broadcast list and are replicated to
 * linked devices so that every device sees the same set of recipients.
 */
@ProtobufMessage(name = "SyncActionValue.BroadcastListParticipant")
public final class BroadcastListParticipantAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The canonical action name used when encoding this action inside a sync patch index.
     */
    public static final String ACTION_NAME = "broadcast_list_participant";

    /**
     * The action version negotiated with the server for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version for this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The LID JID of the participant, identifying the recipient within the broadcast list.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid lidJid;

    /**
     * The participant's phone-number JID, when the mapping from LID to phone number is known.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    Jid pnJid;


    /**
     * Creates an empty participant action, used by the protobuf deserializer and builder.
     */
    public BroadcastListParticipantAction() {

    }

    /**
     * Creates a participant action with the supplied JIDs.
     *
     * @param lidJid the LID JID of the participant, must not be {@code null}
     * @param pnJid  the optional phone-number JID of the participant, may be {@code null}
     * @throws NullPointerException if {@code lidJid} is {@code null}
     */
    BroadcastListParticipantAction(Jid lidJid, Jid pnJid) {
        this.lidJid = Objects.requireNonNull(lidJid);
        this.pnJid = pnJid;
    }

    /**
     * Returns the LID JID of this participant.
     *
     * @return the LID JID
     */
    public Jid lidJid() {
        return lidJid;
    }

    /**
     * Returns the phone-number JID of this participant when available.
     *
     * @return an {@link Optional} containing the phone-number JID, or {@link Optional#empty()}
     *         if the mapping is not known
     */
    public Optional<Jid> pnJid() {
        return Optional.ofNullable(pnJid);
    }

    /**
     * Updates the LID JID of this participant.
     *
     * @param lidJid the new LID JID
     */
    public void setLidJid(Jid lidJid) {
        this.lidJid = lidJid;
    }

    /**
     * Updates the phone-number JID of this participant.
     *
     * @param pnJid the new phone-number JID, or {@code null} to clear it
     */
    public void setPnJid(Jid pnJid) {
        this.pnJid = pnJid;
    }
}

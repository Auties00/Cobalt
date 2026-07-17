package com.github.auties00.cobalt.calls.signaling;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.group.DestinationStanza;
import com.github.auties00.cobalt.calls.signaling.group.GroupInfoStanza;
import com.github.auties00.cobalt.calls.signaling.group.GroupUpdateStanza;
import com.github.auties00.cobalt.calls.signaling.group.HeartbeatStanza;
import com.github.auties00.cobalt.calls.signaling.incall.InCallActionStanza;
import com.github.auties00.cobalt.calls.signaling.incall.RaiseHandStanza;
import com.github.auties00.cobalt.calls.signaling.incall.RekeyStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkCreateStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkEditStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkJoinStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkQueryStanza;
import com.github.auties00.cobalt.calls.signaling.relay.RelayLatencyStanza;
import com.github.auties00.cobalt.calls.signaling.session.AcceptStanza;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.calls.signaling.session.PreacceptStanza;
import com.github.auties00.cobalt.calls.signaling.session.RejectStanza;
import com.github.auties00.cobalt.calls.signaling.session.RingingStanza;
import com.github.auties00.cobalt.calls.signaling.session.TerminateStanza;
import com.github.auties00.cobalt.calls.signaling.session.TransportStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomAdmitStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomDenyStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomLeaveStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomToggleStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomUpdateStanza;

/**
 * Represents one decoded action element of the WhatsApp {@code <call>} signaling plane.
 *
 * <p>Every call action travels as exactly one child element inside a top level {@code <call>} stanza.
 * This sealed interface is the common type of every such action: each implementing record is the typed,
 * decoded form of one child element, holds the action's attributes and nested tree as Java fields, and
 * knows both how to render itself back to a {@link Stanza} ({@link #toStanza()}) and which taxonomy entry it
 * belongs to ({@link #type()}). The {@link CallStanza} factory wraps an instance in the
 * {@code <call>} envelope on the send side and dispatches an inbound child element to the matching
 * record on the receive side.
 *
 * <p>The hierarchy has two tiers. The signaling messages that carry a call setup action (the offer,
 * accept, preaccept, reject, terminate, transport, relay latency, group and rekey messages, the call
 * link control plane, and the waiting room control plane) implement this interface directly. The
 * control actions that mutate a connected call rather than drive its setup are grouped under the
 * {@link InCallActionStanza} interface, which is itself one of the permitted subtypes; an
 * {@link InCallActionStanza} is therefore also a {@link CallMessage} and flows through the same envelope
 * codec and the same receiver sink.
 *
 * <p>{@link #type()} is the projection onto the {@link SignalingType} taxonomy, but it is deliberately
 * nullable: a handful of action elements ({@link RingingStanza}, {@link RaiseHandStanza}) name a
 * {@code <call>} child element yet carry no entry in the numeric {@code voip_signaling_message_type}
 * table, so they report {@code null} and are routed by their wire tag alone. Inbound dispatch in
 * {@link CallStanza#parse(Stanza)} therefore keys on the child element tag through
 * {@link Stanza#description()}, not on {@link #type()}, and {@link #type()} is a convenience for callers
 * that classify an already decoded message.
 *
 * @implNote This implementation keeps the typed decoded form as one record per action rather than a
 * single flat struct, and reuses the binary stanza codec verbatim for the wire bytes, so this interface
 * carries only the two operations every action needs: {@link #toStanza()} to serialize the element over
 * its common {@code call-id} and {@code call-creator} header, and {@link #type()} to project onto the
 * {@code voip_signaling_message_type} taxonomy.
 *
 * @see CallStanza
 * @see SignalingType
 * @see InCallActionStanza
 */
public sealed interface CallMessage permits
        AcceptStanza,
        DestinationStanza,
        GroupInfoStanza,
        GroupUpdateStanza,
        HeartbeatStanza,
        InCallActionStanza,
        LinkCreateStanza,
        LinkEditStanza,
        LinkJoinStanza,
        LinkQueryStanza,
        OfferStanza,
        PreacceptStanza,
        RejectStanza,
        RekeyStanza,
        RelayLatencyStanza,
        RingingStanza,
        TerminateStanza,
        TransportStanza,
        WaitingRoomAdmitStanza,
        WaitingRoomDenyStanza,
        WaitingRoomLeaveStanza,
        WaitingRoomToggleStanza,
        WaitingRoomUpdateStanza {
    /**
     * Returns the signaling taxonomy entry this message projects onto.
     *
     * <p>The result is the {@link SignalingType} whose numeric id the engine dispatches on for this
     * action, or {@code null} for the few action elements that name a {@code <call>} child element but
     * carry no entry in the numeric {@code voip_signaling_message_type} table (notably
     * {@link RingingStanza} and {@link RaiseHandStanza}). Callers that route inbound messages must key
     * on the wire child tag through {@link CallStanza#parse(Stanza)} rather than on this projection,
     * because a {@code null} result is not routable; this accessor exists for callers that classify or
     * log an already decoded message.
     *
     * @return the matching {@link SignalingType}, or {@code null} when this message carries no
     *         taxonomy ordinal
     */
    SignalingType type();

    /**
     * Returns the call identifier this message's {@code call-id} header carries, if any.
     *
     * <p>Every signaling element that carries a call setup action stamps a universal {@code call-id}; this
     * returns it read straight off the decoded message, so a caller need not re render the element with
     * {@link #toStanza()} and parse the attribute back off the produced tree. The result is
     * {@linkplain Optional#empty() empty} for the few {@code <call>} children that carry no call id: the
     * {@code <to>} fanout ({@link DestinationStanza}) and {@code <group>} descriptor
     * ({@link GroupInfoStanza}) nested elements, and the token keyed call link edit, join, and query
     * operations ({@link LinkEditStanza}, {@link LinkJoinStanza}, {@link LinkQueryStanza}); a
     * {@link LinkCreateStanza} carries one only when the link is generated from a call already in flight.
     *
     * @return the call identifier, or empty when this message carries no {@code call-id} header
     */
    Optional<String> callId();

    /**
     * Returns the call creator device JID this message's {@code call-creator} header carries, if any.
     *
     * <p>The creator is the device that originated the call, stamped alongside {@link #callId()} on every
     * action bearing element and read straight off the decoded message here. The result is
     * {@linkplain Optional#empty() empty} for the same headerless children {@link #callId()} returns empty
     * for.
     *
     * @return the call creator's device JID, or empty when this message carries no {@code call-creator}
     *         header
     */
    Optional<Jid> callCreator();

    /**
     * Renders this message to its {@code <call>} child element {@link Stanza}.
     *
     * <p>The returned stanza is the action element only: it carries the action's wire tag, its universal
     * {@code call-id} and {@code call-creator} header, its type specific attributes, and its nested tree,
     * but not the surrounding {@code <call>} envelope. Wrapping it in the envelope with the recipient and
     * the dispatcher assigned stanza id is {@link CallStanza#toCall(CallMessage, Jid, String)}'s
     * responsibility, so the same action stanza can be addressed to different recipients without rebuilding
     * it.
     *
     * @return the action element stanza; never {@code null}
     */
    Stanza toStanza();
}

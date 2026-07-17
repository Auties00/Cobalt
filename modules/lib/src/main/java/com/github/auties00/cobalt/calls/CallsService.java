package com.github.auties00.cobalt.calls;

import com.github.auties00.cobalt.calls.stream.AudioInput;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.VideoInput;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.calls.signaling.receive.CallAckOutcome;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.session.OfferNoticeStanza;
import com.github.auties00.cobalt.calls.signaling.session.TerminateStanza;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.call.Call;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.linked.call.CallInteraction;
import com.github.auties00.cobalt.wire.linked.call.CallLink;
import com.github.auties00.cobalt.wire.linked.call.CallLinkMedia;
import com.github.auties00.cobalt.wire.linked.call.CallState;
import com.github.auties00.cobalt.wire.linked.call.IncomingCall;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Set;

/**
 * Coordinates one client's call activity over the wa-voip engine.
 *
 * <p>This is the host API boundary that sits above the wa-voip call engine: it is the service the
 * {@link LinkedWhatsAppClient} delegates every call control method to, and the inbound seam the call
 * signaling receivers forward every decoded action to. It owns the registry of in flight
 * {@link CallRuntime} sessions and translates the application's place, accept, reject, terminate, and
 * in call control requests onto the engine's lifecycle controller, and routes the receivers' decoded
 * {@link CallMessage} stream into that controller, which in turn fans the host facing events out to the
 * registered listeners. There is exactly one instance per {@link LinkedWhatsAppClient}.
 *
 * <p>The surface has two halves. The outbound control half mirrors the public call API the
 * {@link LinkedWhatsAppClient} exposes, so the client's call methods are thin delegators onto it:
 * {@link #placeCall(Jid, AudioOutput, AudioInput, VideoOutput, VideoInput)} and
 * {@link #placeGroupCall(Set, Jid, AudioOutput, AudioInput, VideoOutput, VideoInput)}
 * start a call,
 * {@link #accept(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}
 * and {@link #reject(IncomingCall, CallEndReason)} answer an inbound offer,
 * {@link #terminate(String, CallEndReason)} ends a call, and the
 * {@code send*} and {@code start*} methods drive the mid call controls (mute, video state, screen share,
 * interactions). The inbound half is the receiver seam:
 * {@link #handleInbound(CallMessage, Jid)} forwards a decoded signaling action,
 * {@link #handleInboundTerminate(TerminateStanza, Jid)} forwards a bare terminate, and
 * {@link #callExists(String)} reports whether a call object already exists so the receiver can decide
 * whether a payload is processed now or buffered.
 *
 * <p>This interface is sealed and permits only its production implementation; the {@code sealed} plus
 * {@code Live*} convention applies. It is deliberately not promoted onto
 * {@link com.github.auties00.cobalt.client.WhatsAppClient}, because the call surface is Linked only (the
 * Cloud transport has no calls).
 *
 * @implSpec
 * An implementation tracks each placed or accepted call in a registry keyed by call id, keys the public
 * {@link Call} view a control method addresses to that registry, and forwards every inbound
 * {@link CallMessage} to the engine lifecycle controller for its per type dispatch (offer ringing,
 * preaccept, accept, reject, terminate, transport). It must keep a call failure non fatal so it never
 * tears the messaging socket down: a media or transport bring up that cannot start surfaces as a non fatal
 * {@link com.github.auties00.cobalt.exception.linked.WhatsAppCallException}, and a peer driven call end is a
 * listener event rather than a thrown exception.
 */
public sealed interface CallsService permits LiveCallsService {
    /**
     * Places an outbound one to one call to {@code peer} carrying the given media streams and returns its
     * live session.
     *
     * <p>A fresh call identifier is generated, a {@link CallRuntime} is registered in the in flight
     * registry with the supplied streams along with a caller side telemetry accumulator, the call key is
     * minted and fanned out per peer device inside the offer, and the offer is sent. The call is a video
     * call when {@code videoOut} is non-{@code null} and audio only otherwise. The session is parked in
     * {@link CallState#RINGING} until the peer answers; the caller's media plane is brought up only once
     * the peer's accept arrives, so this method returns with the call ringing rather than connected.
     *
     * <p>This is the one to one entry point; {@code peer} must be a user JID. A group or community JID is
     * rejected, since a group call must be placed through
     * {@link #placeGroupCall(Set, Jid, AudioOutput, AudioInput, VideoOutput, VideoInput)}.
     *
     * @implSpec
     * Implementations must register the session before delegating the offer build and send to the engine
     * lifecycle controller.
     *
     * @param peer     the {@link Jid} of the callee; must be a user JID
     * @param audioOut the source the engine drains local audio from for transmission
     * @param audioIn  the sink the engine fills with received remote audio
     * @param videoOut the source the engine drains local video from for transmission, or {@code null} for
     *                 an audio only call
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} for an
     *                 audio only call
     * @return the live session
     * @throws NullPointerException     if {@code peer}, {@code audioOut}, or {@code audioIn} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code peer} is a group or community JID
     * @throws IllegalStateException    if the client is not logged in
     */
    Call placeCall(Jid peer, AudioOutput audioOut, AudioInput audioIn,
                   VideoOutput videoOut, VideoInput videoIn);

    /**
     * Places an outbound group call into {@code groupJid}, fanning the offer out to every {@code peer} in
     * {@code peers}, and returns its live session.
     *
     * <p>A fresh call identifier is generated and one {@link CallRuntime} is registered for the whole
     * call with the supplied streams. The call is a video call when {@code videoOut} is non-{@code null}
     * and audio only otherwise. The session is parked in {@link CallState#CONNECTING} until the group join
     * completes; the group call ships no call key in the offer, since the per participant key arrives
     * post join through a rekey.
     *
     * @implSpec
     * Implementations must register one session keyed by the generated call id and delegate the group
     * offer build and send to the engine lifecycle controller before returning. The session's
     * {@link Call#chatJid()} is the group.
     *
     * @param peers    the user JIDs of every other group participant (the local user is excluded)
     * @param groupJid the group {@link Jid}
     * @param audioOut the source the engine drains local audio from for transmission
     * @param audioIn  the sink the engine fills with received remote audio
     * @param videoOut the source the engine drains local video from for transmission, or {@code null} for
     *                 an audio only call
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} for an
     *                 audio only call
     * @return the live session
     * @throws NullPointerException     if {@code peers}, {@code groupJid}, {@code audioOut}, or
     *                                  {@code audioIn} is {@code null}
     * @throws IllegalArgumentException if {@code peers} is empty
     * @throws IllegalStateException    if the client is not logged in
     */
    Call placeGroupCall(Set<Jid> peers, Jid groupJid, AudioOutput audioOut, AudioInput audioIn,
                        VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins a call through a call link token, carrying the given media streams, and returns its live
     * session.
     *
     * <p>A fresh call identifier is generated and one {@link CallRuntime} is registered with the supplied
     * streams and a caller side telemetry accumulator. The link token is resolved through a preview query
     * and a join request, the joined call's lobby state and relay are taken from the join acknowledgement,
     * and the call is answered. The local user joins with video when {@code videoOut} is non-{@code null}
     * and audio only otherwise; {@code media} is the link's configured media kind, carried on the link
     * query and join so the relay can confirm it matches the link's configuration. The session is parked in
     * {@link CallState#CONNECTING} until the join completes and the media plane is brought up, so this method
     * returns with the call connecting rather than connected. The whole path is gated on the server call link
     * feature flag; a build whose flags disable call links refuses the join.
     *
     * @implSpec
     * Implementations must register the session before delegating the join handshake to the engine
     * lifecycle controller, and must surface a disabled call link feature flag as a deny rather than running
     * the handshake.
     *
     * @param token    the call link token to join, the path segment of a {@code call.whatsapp.com} link
     * @param media    the link's configured media kind, carried on the link query and join
     * @param audioOut the source the engine drains local audio from for transmission
     * @param audioIn  the sink the engine fills with received remote audio
     * @param videoOut the source the engine drains local video from for transmission, or {@code null} to
     *                 join audio only
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} to join
     *                 audio only
     * @return the live session
     * @throws NullPointerException  if {@code token}, {@code media}, {@code audioOut}, or {@code audioIn} is
     *                               {@code null}
     * @throws IllegalStateException if the client is not logged in, or call links are disabled for this
     *                               account by the server feature gate
     */
    Call joinCallLink(String token, CallLinkMedia media, AudioOutput audioOut, AudioInput audioIn,
                      VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins the in progress group call described by an offer this device already received, carrying the given
     * media streams, and returns its live session.
     *
     * <p>Late joining an ongoing group call is not a fresh offer: the device was rung with the call's
     * {@code <offer>} (which the engine tracks with its relay), and joining it enters the call's lobby and
     * answers. One {@link CallRuntime} is registered with the supplied streams and a callee side telemetry
     * accumulator, an empty {@code <lobby>} is sent to the call's target, and the {@code <accept>} is shipped;
     * the media plane comes up from the relay the subsequent {@code <group_update>} delivers. The local user
     * joins with video when {@code videoOut} is non-{@code null} and audio only otherwise. The session is parked
     * in {@link CallState#CONNECTING} until the media plane is brought up.
     *
     * <p>This differs from {@link #accept(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)} only
     * in the leading {@code <lobby>}: accept answers a ringing offer directly, while this joins the same offer
     * from the ongoing call banner. It applies only to a group call.
     *
     * @implSpec
     * Implementations must register the session before delegating the join to the engine lifecycle controller,
     * which requires the call to be tracked from its inbound offer.
     *
     * @param offer    the received group offer to join; must describe a group call the engine tracks
     * @param audioOut the source the engine drains local audio from for transmission
     * @param audioIn  the sink the engine fills with received remote audio
     * @param videoOut the source the engine drains local video from for transmission, or {@code null} to join
     *                 audio only
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} to join audio only
     * @return the live session
     * @throws NullPointerException     if {@code offer}, {@code audioOut}, or {@code audioIn} is {@code null}
     * @throws IllegalArgumentException if {@code offer} does not name a tracked group call
     * @throws IllegalStateException    if the client is not logged in, or the call is not in an answerable state
     */
    Call joinGroupCall(IncomingCall offer, AudioOutput audioOut, AudioInput audioIn,
                       VideoOutput videoOut, VideoInput videoIn);

    /**
     * Accepts an inbound call offer with the given media streams and returns its live session.
     *
     * <p>A {@link CallRuntime} is registered with the supplied streams along with a callee side
     * telemetry accumulator, the engine brings up the media plane from the relay block the offer or a later
     * group update carried, the accept is sent, and the session is parked in {@link CallState#CONNECTING}.
     * The answered leg is a video call when {@code videoOut} is non-{@code null} and audio only otherwise.
     *
     * @implSpec
     * Implementations must register the session before delegating the accept to the engine lifecycle
     * controller.
     *
     * @param offer    the offer being accepted
     * @param audioOut the source the engine drains local audio from for transmission
     * @param audioIn  the sink the engine fills with received remote audio
     * @param videoOut the source the engine drains local video from for transmission, or {@code null} to
     *                 answer audio only
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} to answer
     *                 audio only
     * @return the live session
     * @throws NullPointerException if {@code offer}, {@code audioOut}, or {@code audioIn} is {@code null}
     */
    Call accept(IncomingCall offer, AudioOutput audioOut, AudioInput audioIn,
                VideoOutput videoOut, VideoInput videoIn);

    /**
     * Rejects an inbound call offer with the given reason.
     *
     * <p>The reject is sent to the peer through the engine lifecycle controller, which tears down the
     * offered call and its credentials, and {@code onCallEnded} is fired on every listener.
     *
     * @implSpec
     * Implementations must notify listeners of the end after sending the reject.
     *
     * @param offer  the offer being rejected
     * @param reason the {@link CallEndReason} to communicate to the peer
     * @throws NullPointerException if {@code offer} or {@code reason} is {@code null}
     */
    void reject(IncomingCall offer, CallEndReason reason);

    /**
     * Sends a call termination action and tears down the local call runtime.
     *
     * <p>Sends the terminate to the peer through the engine lifecycle controller, then ends the matching
     * runtime so its media streams and media plane session are released. A no op when no call is tracked
     * for {@code callId}.
     *
     * @param callId the call identifier
     * @param reason the {@link CallEndReason} to communicate
     * @throws NullPointerException if {@code reason} is {@code null}
     */
    void terminate(String callId, CallEndReason reason);

    /**
     * Sends an early ring acknowledgement for an inbound offer before the user answers.
     *
     * <p>A callee device emits a preaccept after the offer and before the user answers, so the caller learns
     * the device is alerting and can begin early media preparation. This does not change the call state; the
     * call stays ringing until the user accepts or rejects. A no op when no call is tracked for
     * {@code callId}.
     *
     * @implSpec
     * Implementations must delegate to the engine lifecycle controller's preaccept and tolerate an unknown
     * {@code callId} without throwing.
     *
     * @param callId the identifier of the inbound call to acknowledge
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    void preaccept(String callId);

    /**
     * Sends a mid call group membership update adding or removing participants on an in progress group call.
     *
     * <p>The update is a {@code <group_update>} carrying the call header and a {@code <group_info>} roster of
     * the affected participants with the add or remove action, shipped fire and forget to the group call
     * target.
     *
     * @implSpec
     * Implementations must build the update with the call signaling stanza builders and ship it through the
     * client transport; {@code participants} must be non empty.
     *
     * @param callId       the group call identifier
     * @param target       the group call {@link Jid} the update is addressed to
     * @param creator      the call creator {@link Jid} stamped on the update
     * @param participants the participants to add or remove
     * @param added        {@code true} to add the participants, {@code false} to remove them
     * @throws NullPointerException     if {@code callId}, {@code target}, {@code creator}, or
     *                                  {@code participants} is {@code null}
     * @throws IllegalArgumentException if {@code participants} is empty
     */
    void sendGroupParticipants(String callId, Jid target, Jid creator, java.util.List<Jid> participants,
                               boolean added);

    /**
     * Starts the local camera video track on a connected call, used by an audio to video upgrade.
     *
     * <p>A no op when the call is not tracked or has no media session yet.
     *
     * @param callId the call identifier
     */
    void startLocalVideo(String callId);

    /**
     * Starts a screen share video track on a connected call.
     *
     * <p>A no op when the call is not tracked or has no media session yet.
     *
     * @param callId the call identifier
     */
    void startScreenShare(String callId);

    /**
     * Stops the screen share video track on a connected call.
     *
     * <p>A no op when the call is not tracked or has no screen share track running.
     *
     * @param callId the call identifier
     */
    void stopScreenShare(String callId);

    /**
     * Announces that the local microphone was muted or unmuted to the peer of an in progress call.
     *
     * @param peer    the peer {@link Jid}
     * @param creator the call creator {@link Jid}
     * @param callId  the call identifier
     * @param muted   {@code true} if the microphone is now muted
     */
    void sendMute(Jid peer, Jid creator, String callId, boolean muted);

    /**
     * Announces that local video was enabled or disabled to the peer of an in progress call.
     *
     * @implSpec
     * Implementations should route {@link #sendVideoUpgradeRequest(Jid, Jid, String)} and
     * {@link #sendVideoUpgradeReject(Jid, Jid, String)} through this method.
     *
     * @param peer    the peer {@link Jid}
     * @param creator the call creator {@link Jid}
     * @param callId  the call identifier
     * @param enabled {@code true} if local video is now on
     */
    void sendVideoState(Jid peer, Jid creator, String callId, boolean enabled);

    /**
     * Sends the request leg of a mid call video upgrade.
     *
     * <p>The request is expressed as an enabling video state announce.
     *
     * @param peer    the peer {@link Jid}
     * @param creator the call creator {@link Jid}
     * @param callId  the call identifier
     */
    void sendVideoUpgradeRequest(Jid peer, Jid creator, String callId);

    /**
     * Sends the peer side rejection of a mid call video upgrade.
     *
     * <p>The rejection is expressed as a disabling video state announce and keeps the call audio only.
     *
     * @param peer    the peer {@link Jid}
     * @param creator the call creator {@link Jid}
     * @param callId  the call identifier
     */
    void sendVideoUpgradeReject(Jid peer, Jid creator, String callId);

    /**
     * Sends an in call interaction over the plane the wa-voip engine uses for it.
     *
     * <p>The raise hand, lower hand, and peer mute interactions are server relayed {@code <call>}
     * signaling actions; the reaction and key frame interactions ride the media plane (the reaction as a
     * data channel message, the key frame request as an RTCP picture loss indication). The send is a no op
     * when no call is registered for {@code callId} or when the required plane is unavailable.
     *
     * @implSpec
     * Implementations must be a no op when the call or its required plane is unavailable, never throwing on
     * a missing media plane.
     *
     * @param peer        the peer {@link Jid}
     * @param creator     the call creator {@link Jid}
     * @param callId      the call identifier
     * @param interaction the interaction payload
     * @throws NullPointerException if {@code callId} or {@code interaction} is {@code null}
     */
    void sendInteraction(Jid peer, Jid creator, String callId, CallInteraction interaction);

    /**
     * Mints a fresh shareable call link and returns its resolved metadata.
     *
     * <p>The link is created through a blocking {@code link_create} request to the {@code call} service; the
     * relay answers with the minted token, which is composed into the returned {@link CallLink}. The link is
     * standalone: it allocates no call and can be shared for others to join through
     * {@link #joinCallLink(String, CallLinkMedia, AudioOutput, AudioInput, VideoOutput, VideoInput)}. The
     * request is gated on the server call link feature flag; a build whose flags disable call links refuses
     * the create.
     *
     * @implSpec
     * Implementations must delegate the create to the engine lifecycle controller and surface a disabled
     * call link feature flag as a refusal rather than running the request.
     *
     * @param media              the media kind the link is created with
     * @param waitingRoomEnabled {@code true} to request the link's waiting room gate at creation time
     * @return the minted call link
     * @throws NullPointerException  if {@code media} is {@code null}
     * @throws IllegalStateException if the client is not logged in, or call links are disabled for this
     *                               account by the server feature gate
     */
    CallLink createCallLink(CallLinkMedia media, boolean waitingRoomEnabled);

    /**
     * Enables or disables a call's waiting room gate as the call host.
     *
     * <p>A no op when the call is not tracked or was not joined through a call link, so it carries no
     * waiting room controller. The applied gate state is surfaced to listeners through the waiting room
     * toggle acknowledgement event.
     *
     * @param callId  the identifier of the call whose waiting room gate is toggled
     * @param enabled {@code true} to enable the waiting room, {@code false} to disable it
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    void setWaitingRoomEnabled(String callId, boolean enabled);

    /**
     * Admits a single queued participant into a call from its waiting room lobby as the call host.
     *
     * <p>A no op when the call is not tracked or was not joined through a call link. The admitted
     * participants are surfaced to listeners through the waiting room admit acknowledgement event.
     *
     * @param callId  the identifier of the call the participant is admitted into
     * @param userJid the device JID of the participant to admit
     * @throws NullPointerException if {@code callId} or {@code userJid} is {@code null}
     */
    void admitWaitingRoomParticipant(String callId, Jid userJid);

    /**
     * Admits every queued participant into a call from its waiting room lobby at once as the call host.
     *
     * <p>A no op when the call is not tracked or was not joined through a call link. The admitted
     * participants are surfaced to listeners through the waiting room admit acknowledgement event.
     *
     * @param callId the identifier of the call whose lobby is drained
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    void admitAllWaitingRoomParticipants(String callId);

    /**
     * Denies a single queued participant admission to a call from its waiting room lobby as the call host.
     *
     * <p>A no op when the call is not tracked or was not joined through a call link. The denied participants
     * are surfaced to listeners through the waiting room deny acknowledgement event.
     *
     * @param callId  the identifier of the call the participant is denied from
     * @param userJid the device JID of the participant to deny
     * @throws NullPointerException if {@code callId} or {@code userJid} is {@code null}
     */
    void denyWaitingRoomParticipant(String callId, Jid userJid);

    /**
     * Returns the {@link CallRuntime} tracked under the given identifier, or {@code null} if none is
     * tracked.
     *
     * <p>A {@code null} argument yields {@code null}. This lookup lets a caller route a call scoped
     * operation to the matching session.
     *
     * @implSpec
     * Implementations must return {@code null} for a {@code null} argument and for an unknown id.
     *
     * @param callId the call's unique identifier
     * @return the matching session, or {@code null}
     */
    CallRuntime find(String callId);

    /**
     * Returns whether a call is currently tracked under the given identifier.
     *
     * <p>This is the existence predicate the call signaling receiver consults to decide whether an inbound
     * payload is processed against a live call or buffered for later replay.
     *
     * @implSpec
     * Implementations must return {@code false} for a {@code null} argument and for an unknown id.
     *
     * @param callId the call's unique identifier
     * @return {@code true} when a call is tracked for {@code callId}
     */
    boolean callExists(String callId);

    /**
     * Forwards one decoded inbound signaling action to the engine lifecycle controller.
     *
     * <p>This is the inbound seam the call signaling receiver forwards every routable {@link CallMessage}
     * to, after the receiver has validated the header, classified the message, and emitted its
     * acknowledgement. The implementation hands the message to the engine controller, which dispatches on
     * the message type to the matching phase transition: an offer rings a new call, a preaccept marks the
     * peer alerting, an accept starts the local bring up, a reject or terminate ends the call, a transport
     * message advances the media plane.
     *
     * @implSpec
     * Implementations must forward the message to the engine lifecycle controller with {@code senderJid} as
     * the decryption sender and the peer signaling device, and must tolerate a message for a call the
     * controller does not track without throwing.
     *
     * @param message   the decoded inbound action
     * @param senderJid the device {@link Jid} that authored the message envelope
     * @throws NullPointerException if {@code message} or {@code senderJid} is {@code null}
     */
    void handleInbound(CallMessage message, Jid senderJid);

    /**
     * Handles an inbound {@code <offer_notice>}: the server's notice that a call was offered to this
     * device while it was offline.
     *
     * <p>An offer notice is informational and is handled outside the call engine: it neither rings a live
     * call nor creates an engine call object. The implementation drops a notice older than the
     * WhatsApp imposed staleness window, records a fresh one in the call history as an offline offer, and
     * fans it out to the registered {@code onCallOfferNotice} listeners so the application can surface the
     * missed call.
     *
     * @implSpec
     * Implementations must drop a notice whose {@link OfferNoticeStanza#offerTime()} is older than the
     * staleness window without recording or surfacing it, and must tolerate a notice for a call the engine
     * does not track without throwing.
     *
     * @param notice the decoded inbound offer notice
     * @throws NullPointerException if {@code notice} is {@code null}
     */
    void handleOfferNotice(OfferNoticeStanza notice);

    /**
     * Forwards an inbound bare {@code <terminate>} stanza to the engine lifecycle controller.
     *
     * <p>A bare terminate arrives at the {@code "terminate"} stream tag rather than inside a {@code <call>}
     * envelope; this routes it to the same peer terminate handling an envelope wrapped terminate takes, so
     * the matching call ends and {@code onCallEnded} fires.
     *
     * @implSpec
     * Implementations must route the terminate to the engine lifecycle controller and tolerate a terminate
     * for a call the controller does not track without throwing.
     *
     * @param terminate the decoded inbound terminate
     * @param senderJid the device {@link Jid} that authored the terminate envelope
     * @throws NullPointerException if {@code terminate} or {@code senderJid} is {@code null}
     */
    void handleInboundTerminate(TerminateStanza terminate, Jid senderJid);

    /**
     * Forwards an inbound asynchronous {@code <ack class="call">} to the engine lifecycle controller.
     *
     * <p>The offer's ack is the synchronous reply to the offer send, but the accept is shipped
     * fire and forget, so its ack arrives later as a top level {@code <ack>} with no pending reply to
     * correlate it; this is the seam that delivers that decoded {@link CallAckOutcome} to the controller,
     * which abandons the answered call on an accept NACK and otherwise lets the connected call proceed.
     *
     * @implSpec
     * Implementations must route the outcome to the engine lifecycle controller, keyed by its
     * {@link CallAckOutcome#id() call identifier}, and tolerate an ack for a call the controller does not
     * track without throwing.
     *
     * @param outcome the decoded inbound call ack
     * @throws NullPointerException if {@code outcome} is {@code null}
     */
    void handleInboundAck(CallAckOutcome outcome);

    /**
     * Removes a session from the registry and emits its end of call telemetry.
     *
     * <p>This is invoked when a call reaches {@link CallState#ENDED}: the session is removed from the
     * registry, the call is removed from the store, and its accumulated telemetry is drained into a WAM
     * Call event.
     *
     * @implSpec
     * Implementations must remove the session from the registry and tolerate an unknown {@code callId}.
     *
     * @param callId the call identifier
     */
    void unregister(String callId);

    /**
     * Notifies all registered listeners that a call ended.
     *
     * <p>The wire reason is parsed into a typed {@link CallEndReason} via
     * {@link CallEndReason#fromWireValue(String)}, so an unrecognized or absent literal surfaces as
     * {@link CallEndReason#UNKNOWN}.
     *
     * @implSpec
     * Implementations must surface an unrecognized or {@code null} wire reason as
     * {@link CallEndReason#UNKNOWN} and must dispatch each listener off the calling thread so a slow or
     * throwing listener cannot stall the engine.
     *
     * @param callId     the call identifier
     * @param fromJid    the {@link Jid} of the party that ended the call, or {@code null} when unknown
     * @param wireReason the wire level reason literal, or {@code null}
     */
    void notifyEnded(String callId, Jid fromJid, String wireReason);
}

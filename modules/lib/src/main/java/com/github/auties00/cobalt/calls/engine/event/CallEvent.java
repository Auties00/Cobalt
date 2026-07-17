package com.github.auties00.cobalt.calls.engine.event;

import com.github.auties00.cobalt.calls.engine.participant.CallParticipantState;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.linked.call.CallInteraction;
import com.github.auties00.cobalt.wire.linked.call.CallPeerState;
import com.github.auties00.cobalt.wire.linked.call.CallState;
import com.github.auties00.cobalt.wire.linked.call.IncomingCall;
import com.github.auties00.cobalt.wire.linked.call.JoinableCallLink;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.state.CallLinkState;

/**
 * Represents the typed, decoded payload of one voip event raised during an active call.
 *
 * <p>The native engine routes every state change through a single generic dispatcher that posts an
 * {@code (eventId, payload)} pair to the host. This sealed interface is the Java counterpart of that
 * pair: each permitted record is the decoded form of one event family the host observes, holding the
 * fields the family carries as immutable Java components, and every event reports the
 * {@linkplain #type() event type} that selected it (the dispatch key the engine branched on) and the
 * {@linkplain #callId() call} it belongs to. {@link LiveCallEventBus} accepts a {@code CallEvent},
 * applies the emit gate to its {@link #type()}, and fans the survivors out to the registered listeners;
 * the other call engine units construct these records and publish them through that bus.
 *
 * <p>The hierarchy is keyed by event family, not one to one with the 172 {@link CallEventType}
 * constants: a single record serves a family that a one to one or group call actually emits, several
 * engine ids fold onto the same record where they describe the same change the host observes (the self,
 * peer, and unified video state ids all decode to {@link VideoStateChanged}; the terminate and fatal ids
 * both decode to {@link Ended}), and every remaining id the host observes that has no dedicated typed
 * shape is carried verbatim by {@link Generic} so no admitted event is ever lost. The records here are
 * exactly the families with a public {@code LinkedCall*Listener} mapping plus the few internal families
 * the lifecycle controller consumes directly; the engine's diagnostic only ids never reach a record
 * because the bus gate drops them before construction.
 *
 * <p>The permitted variants are exhaustive, so a {@code switch} over a {@code CallEvent} needs no
 * default branch. Every record validates at construction the components that must not be {@code null}; a
 * component that is genuinely optional on the wire (a peer JID on a change the local device originated, a
 * reason on a clean hangup) is modeled as a nullable component with an {@link Optional} accessor rather
 * than rejected.
 *
 * @implNote This implementation does not model the complete engine payload struct for each event id; the
 * native dispatcher exposes only a decoded subset of fields, so every record carries just the fields its
 * host callback consumes rather than the full layout.
 */
public sealed interface CallEvent
        permits CallEvent.IncomingOffer,
        CallEvent.Preaccept,
        CallEvent.StateChanged,
        CallEvent.PeerStateChanged,
        CallEvent.MuteChanged,
        CallEvent.VideoStateChanged,
        CallEvent.VideoUpgradeRequest,
        CallEvent.PeerVideoPermissionChanged,
        CallEvent.ScreenShareChanged,
        CallEvent.Reaction,
        CallEvent.RaiseHand,
        CallEvent.Transcript,
        CallEvent.CallLinkStateChanged,
        CallEvent.WaitingRoomJoinRequest,
        CallEvent.WaitingRoomAdmitted,
        CallEvent.WaitingRoomDenied,
        CallEvent.ParticipantsChanged,
        CallEvent.Ended,
        CallEvent.Generic {
    /**
     * Returns the event type that selected this event.
     *
     * <p>The value is the native dispatch id the engine branched on, decoded into a
     * {@link CallEventType}. Several ids can map to the same record family, so distinct events of one
     * record subtype may report distinct types; the type is therefore the authoritative discriminator
     * for the emit gate and for listener routing, finer grained than the Java record class.
     *
     * @return the event type, never {@code null}
     */
    CallEventType type();

    /**
     * Returns the identifier of the call this event belongs to.
     *
     * <p>The call id is the sixteen character hexadecimal token minted when the call context was
     * created; it keys the event to one of the two call contexts and is the value the public listener
     * callbacks receive.
     *
     * @return the call id, never {@code null}
     */
    String callId();

    /**
     * Signals that an inbound call offer has been received and the call is ringing locally.
     *
     * <p>This is the typed form of the {@code Call offer received} event ({@code 0x02}); it carries the
     * fully parsed {@link IncomingCall} the engine assembled from the offer stanza, which the bus hands
     * to the public inbound call callback so the application can accept or reject within the offer
     * timeout.
     *
     * @param incoming the parsed inbound offer
     * @see CallEventType#CALL_OFFER_RECEIVED
     */
    record IncomingOffer(IncomingCall incoming) implements CallEvent {
        /**
         * Validates the offer component.
         *
         * @throws NullPointerException if {@code incoming} is {@code null}
         */
        public IncomingOffer {
            Objects.requireNonNull(incoming, "incoming cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#CALL_OFFER_RECEIVED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.CALL_OFFER_RECEIVED;
        }

        /**
         * {@inheritDoc}
         *
         * @return the call id of the inbound offer
         */
        @Override
        public String callId() {
            return incoming.callId();
        }
    }

    /**
     * Signals that the preacceptance phase of an outbound call has begun: the peer device is alerting
     * its user but the user has not yet answered.
     *
     * <p>This is the typed form of the {@code Call preaccept received} event ({@code 0x09}); the bus
     * maps it to the public preaccept callback.
     *
     * @param callId  the identifier of the call
     * @param peerJid the JID of the peer whose device began alerting
     * @see CallEventType#CALL_PREACCEPT_RECEIVED
     */
    record Preaccept(String callId, Jid peerJid) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId} or {@code peerJid} is {@code null}
         */
        public Preaccept {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(peerJid, "peerJid cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#CALL_PREACCEPT_RECEIVED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.CALL_PREACCEPT_RECEIVED;
        }
    }

    /**
     * Signals that the call has moved to a new lifecycle state.
     *
     * <p>This is the typed form of the {@code Call state changed} event ({@code 0x10}), the event the
     * engine fires from its state setter after every accepted transition. It carries both the engine's
     * fifteen {@linkplain CallLifecycleState internal states} and its projection onto the public five
     * phase {@link CallState}; the bus does not map this directly to a single public callback, because
     * the state change is consumed by the lifecycle controller, which decides which user visible event
     * (an end, a reconnect) the transition implies.
     *
     * @param callId   the identifier of the call
     * @param previous the state the call left, or {@code null} when this is the first observed state
     * @param current  the state the call entered
     * @see CallEventType#CALL_STATE_CHANGED
     * @see CallLifecycleState
     */
    record StateChanged(String callId, CallLifecycleState previous, CallLifecycleState current) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId} or {@code current} is {@code null}
         */
        public StateChanged {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(current, "current cannot be null");
        }

        /**
         * Returns the state the call left, if a prior state was observed.
         *
         * @return an {@link Optional} over the previous state, or empty for the first observed state
         */
        public Optional<CallLifecycleState> previousState() {
            return Optional.ofNullable(previous);
        }

        /**
         * Returns the public lifecycle phase the call now presents.
         *
         * <p>This projects the {@linkplain #current() current} internal state through
         * {@link CallLifecycleState#toPublic()}.
         *
         * @return the projected public phase, never {@code null}
         */
        public CallState publicState() {
            return current.toPublic();
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#CALL_STATE_CHANGED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.CALL_STATE_CHANGED;
        }
    }

    /**
     * Signals that a peer advertised a transient state during the call through a peer state payload.
     *
     * <p>This is the typed form of the engine's peer state egress: it carries the JID of the peer and
     * the parsed {@link CallPeerState}, which the bus maps to the public peer state changed callback.
     * Unrecognized wire literals surface as {@link CallPeerState#UNKNOWN}.
     *
     * @param callId  the identifier of the call
     * @param peerJid the JID of the peer whose advertised state changed
     * @param state   the parsed peer state
     * @see CallPeerState
     */
    record PeerStateChanged(String callId, Jid peerJid, CallPeerState state) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId}, {@code peerJid}, or {@code state} is
         *                              {@code null}
         */
        public PeerStateChanged {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(peerJid, "peerJid cannot be null");
            Objects.requireNonNull(state, "state cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * <p>The peer state change is driven by an inbound peer state signaling message rather than by a
         * distinct event id; it reports {@link CallEventType#CALL_STATE_CHANGED} as the closest dispatch
         * key, while the {@link PeerStateChanged} class is the precise discriminator.
         *
         * @return {@link CallEventType#CALL_STATE_CHANGED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.CALL_STATE_CHANGED;
        }
    }

    /**
     * Signals that a participant muted or unmuted their microphone.
     *
     * <p>This is the typed form of the {@code Mute state changed} event ({@code 0x49}) and of the
     * {@code Mute by another participant} event ({@code 0x66}): both describe a change of mic state,
     * with {@link #byAnotherParticipant()} distinguishing a change the participant made from a mute an
     * admin forced. The bus maps it to the public mute changed callback, and a mute an admin forced
     * additionally surfaces as a peer mute interaction.
     *
     * @param callId               the identifier of the call
     * @param participantJid       the JID of the participant whose mic state changed
     * @param muted                {@code true} for a mute, {@code false} for an unmute
     * @param byAnotherParticipant {@code true} when another participant (an admin) forced the change,
     *                             {@code false} when the participant made the change
     * @see CallEventType#MUTE_STATE_CHANGED
     * @see CallEventType#MUTE_BY_ANOTHER_PARTICIPANT
     */
    record MuteChanged(String callId, Jid participantJid, boolean muted, boolean byAnotherParticipant)
            implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId} or {@code participantJid} is {@code null}
         */
        public MuteChanged {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(participantJid, "participantJid cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * <p>A mute an admin forced reports {@link CallEventType#MUTE_BY_ANOTHER_PARTICIPANT}; a change
         * the participant made reports {@link CallEventType#MUTE_STATE_CHANGED}.
         *
         * @return the matching mute event type
         */
        @Override
        public CallEventType type() {
            return byAnotherParticipant
                    ? CallEventType.MUTE_BY_ANOTHER_PARTICIPANT
                    : CallEventType.MUTE_STATE_CHANGED;
        }
    }

    /**
     * Signals that a participant turned their camera on or off.
     *
     * <p>This is the typed form of the unified {@code Video state changed} event ({@code 0x92}) and of
     * the legacy self ({@code 0x33}) and peer ({@code 0x34}) video state events: all three describe a
     * participant toggling video, which the bus maps to the public video state changed callback. A
     * change the local device originated may carry no peer JID, in which case {@link #participantJid()}
     * is empty.
     *
     * @param callId         the identifier of the call
     * @param participantJid the JID of the participant whose video state changed, or {@code null} for a
     *                       change the local device originated with no peer attribution
     * @param enabled        {@code true} when video is on, {@code false} when video is off
     * @see CallEventType#VIDEO_STATE_CHANGED
     */
    record VideoStateChanged(String callId, Jid participantJid, boolean enabled) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId} is {@code null}
         */
        public VideoStateChanged {
            Objects.requireNonNull(callId, "callId cannot be null");
        }

        /**
         * Returns the JID of the participant whose video state changed, when attributed.
         *
         * @return an {@link Optional} over the participant JID, or empty for an unattributed change the
         *         local device originated
         */
        public Optional<Jid> participant() {
            return Optional.ofNullable(participantJid);
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#VIDEO_STATE_CHANGED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.VIDEO_STATE_CHANGED;
        }
    }

    /**
     * Signals that a peer asked to upgrade an audio only call to audio plus video.
     *
     * <p>This is the decoded form of the inbound {@code <video>} action carrying the
     * {@code UPGRADE_REQUEST} (or {@code UPGRADE_REQUEST_V2}) state: a notification the host observes
     * distinct from the camera toggle {@link VideoStateChanged}, since the application must answer it by
     * accepting or rejecting the upgrade rather than simply rendering a peer's camera toggle. It rides
     * the same native video state changed dispatch id, so it reports
     * {@link CallEventType#VIDEO_STATE_CHANGED}; the bus routes it to the dedicated video upgrade request
     * callback by record class.
     *
     * @param callId  the identifier of the call
     * @param peerJid the JID of the peer requesting the upgrade
     * @see CallEventType#VIDEO_STATE_CHANGED
     */
    record VideoUpgradeRequest(String callId, Jid peerJid) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId} or {@code peerJid} is {@code null}
         */
        public VideoUpgradeRequest {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(peerJid, "peerJid cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#VIDEO_STATE_CHANGED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.VIDEO_STATE_CHANGED;
        }
    }

    /**
     * Signals that a peer's permission to send video changed.
     *
     * <p>This is the typed form of the {@code Peer video permission changed} event ({@code 0x93}),
     * raised in a group call when the host grants or revokes a participant's ability to share video.
     *
     * @param callId    the identifier of the call
     * @param peerJid   the JID of the peer whose video permission changed
     * @param permitted {@code true} when the peer may now send video, {@code false} when revoked
     * @see CallEventType#PEER_VIDEO_PERMISSION_CHANGED
     */
    record PeerVideoPermissionChanged(String callId, Jid peerJid, boolean permitted) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId} or {@code peerJid} is {@code null}
         */
        public PeerVideoPermissionChanged {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(peerJid, "peerJid cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#PEER_VIDEO_PERMISSION_CHANGED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.PEER_VIDEO_PERMISSION_CHANGED;
        }
    }

    /**
     * Signals that a participant started, stopped, or failed a screen share.
     *
     * <p>This is the typed form of the {@code Screen Share} event ({@code 0x74}); the screen share
     * state is carried as one of the three engine codes exposed by the nested {@link State} enum.
     *
     * @param callId         the identifier of the call
     * @param participantJid the JID of the participant whose screen share state changed
     * @param state          the new screen share state
     * @see CallEventType#SCREEN_SHARE
     */
    record ScreenShareChanged(String callId, Jid participantJid, State state) implements CallEvent {
        /**
         * Enumerates the three states the engine reports for a screen share.
         *
         * @implNote This implementation mirrors the screen share state codes the engine carries on the
         * {@code Screen Share} event ({@code 0x74}): {@code 1} start, {@code 2} stopped, {@code 3}
         * failed, the same triple modeled by the screen share stanza in the signaling layer.
         */
        public enum State {
            /**
             * Indicates a screen share started.
             */
            STARTED,

            /**
             * Indicates a screen share stopped.
             */
            STOPPED,

            /**
             * Indicates a screen share failed to start or to continue.
             */
            FAILED
        }

        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId}, {@code participantJid}, or {@code state} is
         *                              {@code null}
         */
        public ScreenShareChanged {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(participantJid, "participantJid cannot be null");
            Objects.requireNonNull(state, "state cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#SCREEN_SHARE}
         */
        @Override
        public CallEventType type() {
            return CallEventType.SCREEN_SHARE;
        }
    }

    /**
     * Signals that a participant broadcast an emoji reaction.
     *
     * <p>This is the typed form of the {@code Reaction State Changed} event ({@code 0x91}); reactions
     * travel on the SCTP data channel rather than over signaling, and the engine raises this event once
     * the decoded reaction is applied. The bus maps it to the public call interaction callback as a
     * {@link CallInteraction.Reaction}.
     *
     * @param callId         the identifier of the call
     * @param participantJid the JID of the participant who reacted
     * @param emoji          the reaction emoji, typically a single grapheme
     * @see CallEventType#REACTION_STATE_CHANGED
     */
    record Reaction(String callId, Jid participantJid, String emoji) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException     if {@code callId}, {@code participantJid}, or {@code emoji}
         *                                  is {@code null}
         * @throws IllegalArgumentException if {@code emoji} is empty
         */
        public Reaction {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(participantJid, "participantJid cannot be null");
            Objects.requireNonNull(emoji, "emoji cannot be null");
            if (emoji.isEmpty()) {
                throw new IllegalArgumentException("emoji cannot be empty");
            }
        }

        /**
         * Returns this reaction as the public interaction payload the call interaction callback carries.
         *
         * @return a {@link CallInteraction.Reaction} over {@link #emoji()}
         */
        public CallInteraction.Reaction toInteraction() {
            return new CallInteraction.Reaction(emoji);
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#REACTION_STATE_CHANGED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.REACTION_STATE_CHANGED;
        }
    }

    /**
     * Signals that a participant raised or lowered their hand.
     *
     * <p>This is the typed form of the {@code Raise Hand State Changed} event ({@code 0x94}); the raise
     * hand signal travels over signaling. The bus maps it to the public call interaction callback as a
     * {@link CallInteraction.RaiseHand} or {@link CallInteraction.LowerHand}.
     *
     * @param callId         the identifier of the call
     * @param participantJid the JID of the participant whose hand state changed
     * @param raised         {@code true} when the hand was raised, {@code false} when lowered
     * @see CallEventType#RAISE_HAND_STATE_CHANGED
     */
    record RaiseHand(String callId, Jid participantJid, boolean raised) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId} or {@code participantJid} is {@code null}
         */
        public RaiseHand {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(participantJid, "participantJid cannot be null");
        }

        /**
         * Returns this hand state change as the public interaction payload the call interaction callback
         * carries.
         *
         * @return a {@link CallInteraction.RaiseHand} when {@link #raised()} is {@code true}, otherwise
         *         a {@link CallInteraction.LowerHand}
         */
        public CallInteraction toInteraction() {
            return raised ? new CallInteraction.RaiseHand() : new CallInteraction.LowerHand();
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#RAISE_HAND_STATE_CHANGED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.RAISE_HAND_STATE_CHANGED;
        }
    }

    /**
     * Signals that a live transcription fragment was received for a participant's speech.
     *
     * <p>This is the typed form of the {@code Transcript Received} event ({@code 0x9b}); transcripts
     * arrive over the SCTP data channel and carry the speaker JID, the transcribed text, its language
     * tag, and a monotonic sequence number per speaker that lets a consumer order and deduplicate
     * fragments.
     *
     * @param callId   the identifier of the call
     * @param speakerJid the JID of the participant whose speech was transcribed
     * @param text     the transcribed text fragment
     * @param language the {@code BCP-47} language tag of the text, or {@code null} when the engine did
     *                 not supply one
     * @param sequence the monotonic sequence number of this fragment within the speaker's stream
     * @see CallEventType#TRANSCRIPT_RECEIVED
     */
    record Transcript(String callId, Jid speakerJid, String text, String language, long sequence)
            implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId}, {@code speakerJid}, or {@code text} is
         *                              {@code null}
         */
        public Transcript {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(speakerJid, "speakerJid cannot be null");
            Objects.requireNonNull(text, "text cannot be null");
        }

        /**
         * Returns the language tag of the transcribed text, if the engine supplied one.
         *
         * @return an {@link Optional} over the language tag, or empty when none was supplied
         */
        public Optional<String> languageTag() {
            return Optional.ofNullable(language);
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#TRANSCRIPT_RECEIVED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.TRANSCRIPT_RECEIVED;
        }
    }

    /**
     * Signals that the link join substate of a group call advanced.
     *
     * <p>This is the typed form of the {@code Call link state changed} event ({@code 0x6a}), fired while
     * the call is in the {@link CallLifecycleState#LINK} phase as the query and join legs of joining a call
     * via a call link complete. It carries the prior and new {@link CallLinkState} substates.
     *
     * @param callId   the identifier of the call
     * @param previous the link substate the call left, or {@code null} when first observed
     * @param current  the link substate the call entered
     * @see CallEventType#CALL_LINK_STATE_CHANGED
     * @see CallLinkState
     */
    record CallLinkStateChanged(String callId, CallLinkState previous, CallLinkState current)
            implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId} or {@code current} is {@code null}
         */
        public CallLinkStateChanged {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(current, "current cannot be null");
        }

        /**
         * Returns the link substate the call left, if a prior substate was observed.
         *
         * @return an {@link Optional} over the previous substate, or empty for the first observed
         *         substate
         */
        public Optional<CallLinkState> previousState() {
            return Optional.ofNullable(previous);
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#CALL_LINK_STATE_CHANGED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.CALL_LINK_STATE_CHANGED;
        }
    }

    /**
     * Signals that a joiner is waiting in the lobby of a call link the local user hosts.
     *
     * <p>This is the typed form of the {@code Waiting room state changed} event ({@code 0x9e}) on the
     * host side, raised when someone clicks a call link the local user owns and lands in the waiting
     * room; the bus maps it to the public lobby join request callback so the host can admit or deny.
     *
     * @param link the link the joiner is waiting on
     * @param peer the JID of the joiner waiting in the lobby
     * @see CallEventType#WAITING_ROOM_STATE_CHANGED
     */
    record WaitingRoomJoinRequest(JoinableCallLink link, Jid peer) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code link} or {@code peer} is {@code null}
         */
        public WaitingRoomJoinRequest {
            Objects.requireNonNull(link, "link cannot be null");
            Objects.requireNonNull(peer, "peer cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#WAITING_ROOM_STATE_CHANGED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.WAITING_ROOM_STATE_CHANGED;
        }

        /**
         * {@inheritDoc}
         *
         * <p>A lobby join request predates the call session, so the link carries no call id yet; the
         * link token, which is stable across the lobby and the call it admits into, is used as the
         * identifier, falling through to the link's own call id on the off chance it is already set.
         *
         * @return the link's call id when set, otherwise the link token
         */
        @Override
        public String callId() {
            return link.callId().orElseGet(link::token);
        }
    }

    /**
     * Signals that the host of a call link admitted the local user out of the lobby.
     *
     * <p>This is the typed form of the local user's admit acknowledgement: the host let the local user
     * in and the call is now starting. The bus maps it to the public link admitted callback; a regular
     * call event follows once the underlying call session is created.
     *
     * @param link the link that was admitted
     * @see CallEventType#WAITING_ROOM_ADMIT_ACKED
     */
    record WaitingRoomAdmitted(JoinableCallLink link) implements CallEvent {
        /**
         * Validates the link component.
         *
         * @throws NullPointerException if {@code link} is {@code null}
         */
        public WaitingRoomAdmitted {
            Objects.requireNonNull(link, "link cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#WAITING_ROOM_ADMIT_ACKED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.WAITING_ROOM_ADMIT_ACKED;
        }

        /**
         * {@inheritDoc}
         *
         * <p>The link's own call id is used once admitted, falling through to the stable link token when
         * the call session has not yet stamped its id onto the link.
         *
         * @return the link's call id when set, otherwise the link token
         */
        @Override
        public String callId() {
            return link.callId().orElseGet(link::token);
        }
    }

    /**
     * Signals that the host of a call link declined the local user's join request.
     *
     * <p>This is the typed form of the {@code Waiting room denied} event ({@code 0x9d}); it is terminal
     * for that link attempt. The bus maps it to the public link denied callback.
     *
     * @param link the link that was denied
     * @see CallEventType#WAITING_ROOM_DENIED
     */
    record WaitingRoomDenied(JoinableCallLink link) implements CallEvent {
        /**
         * Validates the link component.
         *
         * @throws NullPointerException if {@code link} is {@code null}
         */
        public WaitingRoomDenied {
            Objects.requireNonNull(link, "link cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link CallEventType#WAITING_ROOM_DENIED}
         */
        @Override
        public CallEventType type() {
            return CallEventType.WAITING_ROOM_DENIED;
        }

        /**
         * {@inheritDoc}
         *
         * <p>The link's own call id is used when set, falling through to the stable link token, since a
         * denied joiner never receives a call session.
         *
         * @return the link's call id when set, otherwise the link token
         */
        @Override
        public String callId() {
            return link.callId().orElseGet(link::token);
        }
    }

    /**
     * Signals that participants were added to or removed from a group call in progress.
     *
     * <p>This is the typed form of the {@code Group info changed} event ({@code 0x46}) and of the
     * {@code User removed} event ({@code 0x73}): both describe a change to the participant roster. It
     * carries the owning group JID, the affected participant JIDs, whether they joined or left, and,
     * when the change came from a removal, the {@linkplain CallParticipantState membership state} the
     * removed participants were moved to. The bus maps it to the public participants changed callback.
     *
     * @param callId       the identifier of the group call
     * @param groupJid     the group JID that owns the call
     * @param participants the participants that were added or removed
     * @param added        {@code true} when the participants were added, {@code false} when removed
     * @param newState     the membership state removed participants were moved to, or {@code null} for
     *                     an addition
     * @see CallEventType#GROUP_INFO_CHANGED
     * @see CallEventType#USER_REMOVED
     */
    record ParticipantsChanged(String callId, Jid groupJid, List<Jid> participants, boolean added,
                               CallParticipantState newState) implements CallEvent {
        /**
         * Copies the participant list defensively and validates the components.
         *
         * @throws NullPointerException if {@code callId}, {@code groupJid}, {@code participants}, or any
         *                              participant element is {@code null}
         */
        public ParticipantsChanged {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(groupJid, "groupJid cannot be null");
            Objects.requireNonNull(participants, "participants cannot be null");
            participants = List.copyOf(participants);
        }

        /**
         * Returns the membership state removed participants were moved to, when this is a removal.
         *
         * @return an {@link Optional} over the new membership state, or empty for an addition
         */
        public Optional<CallParticipantState> newMembershipState() {
            return Optional.ofNullable(newState);
        }

        /**
         * {@inheritDoc}
         *
         * <p>A removal reports {@link CallEventType#USER_REMOVED}; a roster change that added
         * participants reports {@link CallEventType#GROUP_INFO_CHANGED}.
         *
         * @return the matching roster event type
         */
        @Override
        public CallEventType type() {
            return added ? CallEventType.GROUP_INFO_CHANGED : CallEventType.USER_REMOVED;
        }
    }

    /**
     * Signals that the call has ended.
     *
     * <p>This is the typed form of the {@code Call terminate received} event ({@code 0x0a}) and of the
     * {@code Call Fatal} event ({@code 0x5c}): both terminate the call, with {@link #fatal()}
     * distinguishing a teardown the peer drove from a local fatal failure. It carries the JID of the
     * party that ended the call and the parsed {@link CallEndReason}; the bus maps it to the public call
     * ended callback.
     *
     * @param callId  the identifier of the call that ended
     * @param fromJid the JID of the party that ended the call, or {@code null} for a local fatal end
     *                with no peer attribution
     * @param reason  the parsed end reason; {@link CallEndReason#UNKNOWN} when none was supplied or the
     *                wire literal was unrecognized
     * @param fatal   {@code true} for a local fatal failure, {@code false} for a terminate the peer
     *                drove
     * @see CallEventType#CALL_TERMINATE_RECEIVED
     * @see CallEventType#CALL_FATAL
     */
    record Ended(String callId, Jid fromJid, CallEndReason reason, boolean fatal) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code callId} or {@code reason} is {@code null}
         */
        public Ended {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(reason, "reason cannot be null");
        }

        /**
         * Returns the JID of the party that ended the call, when attributed.
         *
         * @return an {@link Optional} over the ending party's JID, or empty for an unattributed local
         *         fatal end
         */
        public Optional<Jid> from() {
            return Optional.ofNullable(fromJid);
        }

        /**
         * {@inheritDoc}
         *
         * <p>A local fatal failure reports {@link CallEventType#CALL_FATAL}; a terminate the peer drove
         * reports {@link CallEventType#CALL_TERMINATE_RECEIVED}.
         *
         * @return the matching end event type
         */
        @Override
        public CallEventType type() {
            return fatal ? CallEventType.CALL_FATAL : CallEventType.CALL_TERMINATE_RECEIVED;
        }
    }

    /**
     * Carries an event the host observes that has no dedicated typed payload record.
     *
     * <p>The engine emits 172 event ids; the records above cover the families a one to one or group call
     * routinely surfaces to a listener. This record preserves every other admitted event so it is
     * observable through the raw event egress rather than silently dropped, even though its typed payload
     * byte layout is open. It carries the event {@link CallEventType} verbatim and the owning call id.
     *
     * @param eventType the event type that the engine selected
     * @param callId    the identifier of the call the event belongs to
     * @see LiveCallEventBus
     */
    record Generic(CallEventType eventType, String callId) implements CallEvent {
        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code eventType} or {@code callId} is {@code null}
         */
        public Generic {
            Objects.requireNonNull(eventType, "eventType cannot be null");
            Objects.requireNonNull(callId, "callId cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return the carried event type
         */
        @Override
        public CallEventType type() {
            return eventType;
        }
    }
}

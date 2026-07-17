package com.github.auties00.cobalt.calls.signaling;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.github.auties00.cobalt.calls.signaling.relay.RelayInfo;
import com.github.auties00.cobalt.calls.signaling.session.CallTransportSubType;

/**
 * Enumerates the eighty five signaling message types of the wa-voip {@code <call>} plane.
 *
 * <p>Every call action is exactly one child element inside a top level {@code <call>} stanza, and the
 * wa-voip engine flattens each parsed action into a call message whose first word is a numeric message
 * id. Each constant binds the numeric id the engine dispatches on ({@link #index()}), the delivery
 * {@link #mechanism()} that selects whether the action rides its own {@code <call>} child element, a
 * shared {@code <ack>}/{@code <receipt>} envelope, or no wire form at all, the lowercase wire child tag
 * that names the action inside the {@code <call>} envelope when one exists ({@link #wireTag()}), and the
 * fixed header byte length the inbound validator expects for it ({@link #fixedHeaderLength()}). The five
 * blank slots in the native table (ids 40, 41, 43, 44, 64) are not represented.
 *
 * <p>Dispatch is tag keyed and id keyed, never ordinal keyed: the Java {@link Enum#ordinal()} of these
 * constants is deliberately meaningless for protocol purposes because the native ids contain gaps and
 * do not correspond to declaration order. Inbound classification of an action bearing message keys on
 * the single child element tag through {@link #ofWireTag(String)}; id based lookups (for example,
 * validating a flattened message against its own header) go through {@link #ofIndex(int)}. Both lookups
 * are constant time.
 *
 * <p>Not every type names a {@code <call>} child element. The acknowledgement and receipt legs do not:
 * {@link Mechanism#ACK} legs ride a shared {@code <ack>} envelope and {@link Mechanism#RECEIPT} legs
 * ride a shared {@code <receipt>} envelope, both classified by the host stanza layer through the
 * envelope {@code type} attribute and the echoed request stanza name rather than through a dedicated
 * child tag. For these legs {@link #wireTag()} is empty by design and they are absent from the
 * {@link #ofWireTag(String)} index; their delivery is recorded by {@link #mechanism()}.
 *
 * <p>Three native ids occupy a message type table slot yet name no {@code <call>} child element, so they
 * carry an empty {@link #wireTag()} and the {@link Mechanism#INTERNAL} mechanism: legacy {@link #MUTE}
 * (12), {@link #WEB_CLIENT} (22), and {@link #CALL_RELAY} (35). The engine neither serializes nor
 * dispatches a wire child for any of them. {@link #MUTE} is superseded by {@link #MUTE_V2}, and the
 * inbound dispatch compares only the {@code mute_v2} tag, never {@code mute}. {@link #WEB_CLIENT} and
 * {@link #CALL_RELAY} have no lowercase wire literal at all; they exist only to map the native id.
 * Because the engine never emits or dispatches a {@code <mute>}, {@code <web_client>}, or
 * {@code <call_relay>} child, these ids are absent from the {@link #ofWireTag(String)} index, so an
 * inbound child of one of those names resolves to no type and is dropped, exactly as the engine drops it.
 */
public enum SignalingType {
    /**
     * Represents the sentinel the engine uses for the zeroth table slot when no message type matches.
     *
     * <p>This is the {@code None} entry at id {@code 0} of the native table, not a transmittable action;
     * it never names a wire child element nor rides an envelope, so its {@link #mechanism()} is
     * {@link Mechanism#NONE} and {@link #wireTag()} is empty.
     */
    NONE(0, Mechanism.NONE, null, -1),

    /**
     * Represents the initial offer that announces a one to one or group call to the peers.
     *
     * <p>The inbound validator expects a fixed header length of {@code 777400} bytes.
     */
    OFFER(1, Mechanism.CALL_CHILD, "offer", 777400),

    /**
     * Represents the server delivered receipt confirming an offer reached the peer.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 100} bytes.
     */
    OFFER_RECEIPT(2, Mechanism.RECEIPT, null, 100),

    /**
     * Represents the callee accepting the call.
     *
     * <p>The inbound validator expects a fixed header length of {@code 271184} bytes.
     */
    ACCEPT(3, Mechanism.CALL_CHILD, "accept", 271184),

    /**
     * Represents the callee declining the call before answering.
     *
     * <p>The inbound validator expects a fixed header length of {@code 240} bytes.
     */
    REJECT(4, Mechanism.CALL_CHILD, "reject", 240),

    /**
     * Represents either side ending the call, carrying a {@code reason} literal.
     *
     * <p>The inbound validator expects a fixed header length of {@code 6528} bytes.
     */
    TERMINATE(5, Mechanism.CALL_CHILD, "terminate", 6528),

    /**
     * Represents a transport plane exchange, carrying an inner transport sub type.
     *
     * <p>The inner sub type selects relay candidates, a candidate list, transport protocol, relay
     * latency, peer network health, or ICE/DTLS material; it is modeled separately by
     * {@link CallTransportSubType}. The inbound validator expects a fixed header length of {@code 1112}
     * bytes.
     */
    TRANSPORT(6, Mechanism.CALL_CHILD, "transport", 1112),

    /**
     * Represents the acknowledgement that an offer this device sent was accepted by the server.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element and names no
     * dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 1539272} bytes.
     */
    OFFER_ACK(7, Mechanism.ACK, null, 1539272),

    /**
     * Represents the negative acknowledgement that an offer this device sent was refused.
     *
     * <p>This is a {@link Mechanism#ACK} leg (a negative ack): it rides a shared {@code <ack>} element
     * and names no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator
     * expects a fixed header length of {@code 1539272} bytes.
     */
    OFFER_NACK(8, Mechanism.ACK, null, 1539272),

    /**
     * Represents a relay latency probe report.
     *
     * <p>The inbound validator expects a fixed header length of {@code 1144} bytes.
     */
    RELAY_LATENCY(9, Mechanism.CALL_CHILD, "relaylatency", 1144),

    /**
     * Represents the acknowledgement of a relay latency report.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 104} bytes.
     */
    RELAY_LATENCY_ACK(10, Mechanism.RECEIPT, null, 104),

    /**
     * Represents a call interruption notice, carrying a begin or end state.
     *
     * <p>The inbound validator expects a fixed header length of {@code 108} bytes.
     */
    INTERRUPTION(11, Mechanism.CALL_CHILD, "interruption", 108),

    /**
     * Represents the legacy mute toggle id, superseded on the wire by {@link #MUTE_V2} and emitting no
     * {@code <call>} child.
     *
     * <p>The native table reserves id {@code 12} for this type and the inbound validator recognizes it
     * with an expected fixed header length of {@code 0} bytes, so the engine knows the id, but it has no
     * wire child: the engine neither serializes a {@code <mute>} element nor dispatches one, and the
     * inbound dispatch compares only the {@code mute_v2} tag, never {@code mute}. This id is therefore
     * {@link Mechanism#INTERNAL} with an empty {@link #wireTag()} and is absent from the
     * {@link #ofWireTag(String)} index; an inbound {@code <mute>} resolves to no type and is dropped,
     * exactly as the engine drops it.
     */
    MUTE(12, Mechanism.INTERNAL, null, 0),

    /**
     * Represents a pre acceptance signal: the callee's device is alerting but the user has not
     * answered yet.
     *
     * <p>The inbound validator expects a fixed header length of {@code 284} bytes.
     */
    PREACCEPT(13, Mechanism.CALL_CHILD, "preaccept", 284),

    /**
     * Represents the receipt confirming an accept was delivered.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 100} bytes.
     */
    ACCEPT_RECEIPT(14, Mechanism.RECEIPT, null, 100),

    /**
     * Represents a video state change announcement during a call.
     *
     * <p>The inbound validator expects a fixed header length of {@code 269696} bytes.
     */
    VIDEO_STATE(15, Mechanism.CALL_CHILD, "video", 269696),

    /**
     * Represents a generic in call notification such as a battery state notice.
     *
     * <p>The inbound validator expects a fixed header length of {@code 104} bytes.
     */
    NOTIFY(16, Mechanism.CALL_CHILD, "notify", 104),

    /**
     * Represents a group membership and configuration update during an in progress group call.
     *
     * <p>The element bundles {@code voip_settings}, {@code relay}, {@code group_info}, {@code bot_info},
     * {@code extension_info}, {@code link_info}, and audio and video upgrade children. The inbound
     * validator expects a fixed header length of {@code 497848} bytes.
     */
    GROUP_UPDATE(17, Mechanism.CALL_CHILD, "group_update", 497848),

    /**
     * Represents a group call key re exchange, the one signaling message with a protobuf payload.
     *
     * <p>The inbound validator expects a fixed header length of {@code 548} bytes.
     */
    REKEY(18, Mechanism.CALL_CHILD, "enc_rekey", 548),

    /**
     * Represents a per participant peer state update.
     *
     * <p>The inbound validator expects a fixed header length of {@code 1896} bytes.
     */
    PEER_STATE(19, Mechanism.CALL_CHILD, "peer_state", 1896),

    /**
     * Represents the acknowledgement of a {@link #VIDEO_STATE} change.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 269592} bytes.
     */
    VIDEO_STATE_ACK(20, Mechanism.RECEIPT, null, 269592),

    /**
     * Represents a flow control request carrying target bitrate, width, and frame rate.
     *
     * <p>The inbound validator expects a fixed header length of {@code 116} bytes.
     */
    FLOW_CONTROL(21, Mechanism.CALL_CHILD, "flowcontrol", 116),

    /**
     * Represents the {@code WebClient} message type id, an internal slot the web and shared core never
     * emits as a {@code <call>} child.
     *
     * <p>The native message type table assigns this id its own slot and the inbound validator recognizes
     * it with an expected fixed header length of {@code 0} bytes, so the engine knows the id, but the
     * module exposes no wire form for it: there is no lowercase {@code web_client} wire literal, no
     * serializer, and no inbound dispatch reference. The web and shared core path neither emits nor
     * dispatches a {@code <web_client>} child, so this id is {@link Mechanism#INTERNAL} with an empty
     * {@link #wireTag()}, exists only to map the native id, and is absent from the
     * {@link #ofWireTag(String)} index.
     */
    WEB_CLIENT(22, Mechanism.INTERNAL, null, 0),

    /**
     * Represents the acknowledgement of an accept on the group call path.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element and names no
     * dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 313232} bytes.
     */
    ACCEPT_ACK(23, Mechanism.ACK, null, 313232),

    /**
     * Represents a group call lobby control message.
     *
     * <p>The inbound validator expects a fixed header length of {@code 104} bytes.
     */
    LOBBY(24, Mechanism.CALL_CHILD, "lobby", 104),

    /**
     * Represents the acknowledgement of a {@link #LOBBY} message.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 228272} bytes.
     */
    LOBBY_ACK(25, Mechanism.RECEIPT, null, 228272),

    /**
     * Represents the current mute toggle, requiring exactly one of a request state or a mute state.
     *
     * <p>The inbound validator expects a fixed header length of {@code 104} bytes.
     */
    MUTE_V2(26, Mechanism.CALL_CHILD, "mute_v2", 104),

    /**
     * Represents a call link creation request.
     *
     * <p>The inbound validator expects a fixed header length of {@code 184} bytes.
     */
    LINK_CREATE(27, Mechanism.CALL_CHILD, "link_create", 184),

    /**
     * Represents the acknowledgement of a {@link #LINK_CREATE} request.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element whose body echoes
     * the {@code link_create} request stanza and names no dedicated {@code <call>} child, so
     * {@link #wireTag()} is empty. The inbound validator expects a fixed header length of {@code 132}
     * bytes.
     */
    LINK_CREATE_ACK(28, Mechanism.ACK, null, 132),

    /**
     * Represents a group call heartbeat keepalive.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    HEARTBEAT(29, Mechanism.CALL_CHILD, "heartbeat", -1),

    /**
     * Represents the acknowledgement of a {@link #HEARTBEAT}.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 104} bytes.
     */
    HEARTBEAT_ACK(30, Mechanism.RECEIPT, null, 104),

    /**
     * Represents a call link query (preview or edit lookup).
     *
     * <p>The inbound validator expects a fixed header length of {@code 132} bytes.
     */
    LINK_QUERY(31, Mechanism.CALL_CHILD, "link_query", 132),

    /**
     * Represents the acknowledgement of a {@link #LINK_QUERY}.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element whose body echoes
     * the {@code link_query} request stanza and names no dedicated {@code <call>} child, so
     * {@link #wireTag()} is empty. The inbound validator expects a fixed header length of {@code 43840}
     * bytes.
     */
    LINK_QUERY_ACK(32, Mechanism.ACK, null, 43840),

    /**
     * Represents a call link join request.
     *
     * <p>The inbound validator expects a fixed header length of {@code 312} bytes.
     */
    LINK_JOIN(33, Mechanism.CALL_CHILD, "link_join", 312),

    /**
     * Represents the acknowledgement of a {@link #LINK_JOIN}, supplying the call creator and relay
     * token on success.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element whose body echoes
     * the join stanza and names no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The
     * inbound validator expects a fixed header length of {@code 531808} bytes.
     */
    LINK_JOIN_ACK(34, Mechanism.ACK, null, 531808),

    /**
     * Represents the {@code CallRelay} message type id, a transport plane relay list construct the
     * web and shared core never emits as a {@code <call>} child.
     *
     * <p>This id names the transport plane's relay bookkeeping, not a top level {@code <call>} child
     * message: every lowercase {@code call_relay} symbol is a transport internal identifier that
     * maintains the in memory relay candidate list. The relay endpoints, tokens, and keys this id tracks
     * reach the wire inside the {@code <relay>} block of the {@link #TRANSPORT transport} message and the
     * {@code group_info} of a {@link #GROUP_UPDATE group_update}, both modeled by {@link RelayInfo};
     * there is no standalone {@code <call_relay>} child element. The inbound validator has no fixed header
     * case, so {@link #fixedHeaderLength()} is empty. This id is therefore {@link Mechanism#INTERNAL}
     * with an empty {@link #wireTag()} and is absent from the {@link #ofWireTag(String)} index.
     */
    CALL_RELAY(35, Mechanism.INTERNAL, null, -1),

    /**
     * Represents an administrator request to remove a participant from a group call.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    REMOVE_USER(36, Mechanism.CALL_CHILD, "remove_user", -1),

    /**
     * Represents the acknowledgement of a {@link #REMOVE_USER} request.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 108} bytes.
     */
    REMOVE_USER_ACK(37, Mechanism.RECEIPT, null, 108),

    /**
     * Represents a screen share state change, carrying a state and a version.
     *
     * <p>The inbound validator expects a fixed header length of {@code 108} bytes.
     */
    SCREEN_SHARE(38, Mechanism.CALL_CHILD, "screen_share", 108),

    /**
     * Represents the acknowledgement of a {@link #SCREEN_SHARE} change.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 108} bytes.
     */
    SCREEN_SHARE_ACK(39, Mechanism.RECEIPT, null, 108),

    /**
     * Represents a DTMF tone, carrying a single tone child.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    DTMF_TONE(42, Mechanism.CALL_CHILD, "dtmf", -1),

    /**
     * Represents the start of a broadcast or business call.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    BCALL_START(45, Mechanism.CALL_CHILD, "bcall_start", -1),

    /**
     * Represents the acknowledgement of a {@link #BCALL_START}.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element whose body echoes
     * the {@code bcall_start} request stanza, so {@link #wireTag()} is empty.
     */
    BCALL_START_ACK(46, Mechanism.ACK, null, -1),

    /**
     * Represents a join request on a broadcast or business call.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    BCALL_JOIN(47, Mechanism.CALL_CHILD, "bcall_join", -1),

    /**
     * Represents the acknowledgement of a {@link #BCALL_JOIN}.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element whose body echoes
     * the {@code bcall_join} request stanza, so {@link #wireTag()} is empty.
     */
    BCALL_JOIN_ACK(48, Mechanism.ACK, null, -1),

    /**
     * Represents a leave request on a broadcast or business call.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    BCALL_LEAVE(49, Mechanism.CALL_CHILD, "bcall_leave", -1),

    /**
     * Represents the acknowledgement of a {@link #BCALL_LEAVE}.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element whose body echoes
     * the {@code bcall_leave} request stanza, so {@link #wireTag()} is empty.
     */
    BCALL_LEAVE_ACK(50, Mechanism.ACK, null, -1),

    /**
     * Represents an update on a broadcast or business call.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    BCALL_UPDATE(51, Mechanism.CALL_CHILD, "bcall_update", -1),

    /**
     * Represents the end of a broadcast or business call.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    BCALL_END(52, Mechanism.CALL_CHILD, "bcall_end", -1),

    /**
     * Represents the acknowledgement of a {@link #BCALL_END}.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element whose body echoes
     * the {@code bcall_end} request stanza, so {@link #wireTag()} is empty.
     */
    BCALL_END_ACK(53, Mechanism.ACK, null, -1),

    /**
     * Represents a notification on a broadcast or business call.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    BCALL_NOTIFY(54, Mechanism.CALL_CHILD, "bcall_notify", -1),

    /**
     * Represents a call link edit request.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    LINK_EDIT(55, Mechanism.CALL_CHILD, "link_edit", -1),

    /**
     * Represents the acknowledgement of a {@link #LINK_EDIT}.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element whose body echoes
     * the {@code link_edit} request stanza and names no dedicated {@code <call>} child, so
     * {@link #wireTag()} is empty. The inbound validator expects a fixed header length of {@code 128}
     * bytes.
     */
    LINK_EDIT_ACK(56, Mechanism.ACK, null, 128),

    /**
     * Represents a scheduled group call reminder.
     *
     * <p>The inbound validator expects a fixed header length of {@code 271912} bytes.
     */
    GROUP_CALL_REMINDER(57, Mechanism.CALL_CHILD, "group_call_reminder", 271912),

    /**
     * Represents a connection statistics report.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    CONNECT_STAT(58, Mechanism.CALL_CHILD, "connect_stat", -1),

    /**
     * Represents the acknowledgement of a {@link #PREACCEPT}.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element whose body echoes
     * the {@code preaccept} request stanza, so {@link #wireTag()} is empty. The acknowledgement arrives
     * as {@code <ack from="...@call" class="call" type="preaccept" id="..."/>}, the synchronous return to
     * the sent {@code <preaccept>}, over the {@code <ack>} envelope rather than the {@code <receipt>}
     * alternative.
     */
    PREACCEPT_ACK(59, Mechanism.ACK, null, -1),

    /**
     * Represents a generic user action on a call.
     *
     * <p>The element carries {@code action}, {@code attribution}, and {@code wearable} attributes. The
     * inbound validator expects a fixed header length of {@code 104} bytes.
     */
    USER_ACTION(60, Mechanism.CALL_CHILD, "user_action", 104),

    /**
     * Represents a bot reconfiguration request, carrying a request id.
     *
     * <p>The inbound validator expects a fixed header length of {@code 112} bytes.
     */
    RECONFIGURE_BOT(61, Mechanism.CALL_CHILD, "reconfigure_bot", 112),

    /**
     * Represents a call duration report.
     *
     * <p>The inbound validator expects a fixed header length of {@code 240} bytes.
     */
    DURATION(62, Mechanism.CALL_CHILD, "duration", 240),

    /**
     * Represents a group call duration report.
     *
     * <p>The inbound validator expects a fixed header length of {@code 236} bytes.
     */
    GROUP_CALL_DURATION(63, Mechanism.CALL_CHILD, "group_call_duration", 236),

    /**
     * Represents a readiness signal.
     *
     * <p>The inbound validator expects a fixed header length of {@code 104} bytes.
     */
    READY(65, Mechanism.CALL_CHILD, "ready", 104),

    /**
     * Represents a relay information update.
     *
     * <p>This is a standalone top level {@code <call>} child rather than a sub element of
     * {@code <transport>} or {@code <group_update>}; the inbound dispatch routes it to the transport
     * peer to peer relay information update handler. The inbound validator expects a fixed header length
     * of {@code 9792} bytes.
     */
    RELAY_INFO_UPDATE(66, Mechanism.CALL_CHILD, "relay_info_update", 9792),

    /**
     * Represents a request to leave the waiting room.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    WAITING_ROOM_LEAVE(67, Mechanism.CALL_CHILD, "waiting_room_leave", -1),

    /**
     * Represents the acknowledgement of a {@link #WAITING_ROOM_LEAVE}.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 104} bytes.
     */
    WAITING_ROOM_LEAVE_ACK(68, Mechanism.RECEIPT, null, 104),

    /**
     * Represents a request to toggle the waiting room on or off.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    WAITING_ROOM_TOGGLE(69, Mechanism.CALL_CHILD, "waiting_room_toggle", -1),

    /**
     * Represents the acknowledgement of a {@link #WAITING_ROOM_TOGGLE}.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 128} bytes.
     */
    WAITING_ROOM_TOGGLE_ACK(70, Mechanism.RECEIPT, null, 128),

    /**
     * Represents an administrator request to admit a participant from the waiting room.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    WAITING_ROOM_ADMIT(71, Mechanism.CALL_CHILD, "waiting_room_admit", -1),

    /**
     * Represents the acknowledgement of a {@link #WAITING_ROOM_ADMIT}.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 43628} bytes.
     */
    WAITING_ROOM_ADMIT_ACK(72, Mechanism.RECEIPT, null, 43628),

    /**
     * Represents an administrator request to deny a participant from the waiting room.
     *
     * <p>The inbound validator has no fixed header case for this type, so {@link #fixedHeaderLength()} is
     * empty and the message is variable length.
     */
    WAITING_ROOM_DENY(73, Mechanism.CALL_CHILD, "waiting_room_deny", -1),

    /**
     * Represents the acknowledgement of a {@link #WAITING_ROOM_DENY}.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 43628} bytes.
     */
    WAITING_ROOM_DENY_ACK(74, Mechanism.RECEIPT, null, 43628),

    /**
     * Represents a waiting room state update.
     *
     * <p>The inbound validator expects a fixed header length of {@code 43728} bytes.
     */
    WAITING_ROOM_UPDATE(75, Mechanism.CALL_CHILD, "waiting_room_update", 43728),

    /**
     * Represents a participant removal notice.
     *
     * <p>The inbound validator expects a fixed header length of {@code 356} bytes.
     */
    REMOVE(76, Mechanism.CALL_CHILD, "remove", 356),

    /**
     * Represents the acknowledgement of a {@link #REMOVE}.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 1120} bytes.
     */
    REMOVE_ACK(77, Mechanism.RECEIPT, null, 1120),

    /**
     * Represents the fanout cancellation of an outstanding offer to a set of participants.
     *
     * <p>The inbound validator expects a fixed header length of {@code 356} bytes.
     */
    CANCEL_OFFER(78, Mechanism.CALL_CHILD, "cancel_offer", 356),

    /**
     * Represents a request to add a call extension.
     *
     * <p>The element tag is {@code extension}; this is the canonical owner of that element for inbound
     * {@link #ofWireTag(String)} dispatch. The inbound validator expects a fixed header length of
     * {@code 488936} bytes.
     */
    ADD_EXTENSION(79, Mechanism.CALL_CHILD, "extension", 488936),

    /**
     * Represents the acknowledgement of an {@link #ADD_EXTENSION}.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element and names no
     * dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 228272} bytes.
     */
    ADD_EXTENSION_ACK(80, Mechanism.ACK, null, 228272),

    /**
     * Represents a request to remove a call extension.
     *
     * <p>This type reuses the {@code extension} element rather than a dedicated {@code remove_extension}
     * tag: it emits {@code <destination><extension extension_id=.../></destination>} where the element
     * tag is {@code extension}, the same element {@link #ADD_EXTENSION} emits. The two are distinguished
     * by message id and by the absent session and key children, not by element tag, so {@link #wireTag()}
     * is {@code extension} but {@link #ADD_EXTENSION} is the canonical owner of that element in the
     * inbound {@link #ofWireTag(String)} index. The inbound validator expects a fixed header length of
     * {@code 104} bytes.
     */
    REMOVE_EXTENSION(81, Mechanism.CALL_CHILD, "extension", 104),

    /**
     * Represents the acknowledgement of a {@link #REMOVE_EXTENSION}.
     *
     * <p>This is a {@link Mechanism#RECEIPT} leg: it rides a shared {@code <receipt>} element and names
     * no dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 104} bytes.
     */
    REMOVE_EXTENSION_ACK(82, Mechanism.RECEIPT, null, 104),

    /**
     * Represents acceptance of the group call terms of service.
     *
     * <p>The inbound validator expects a fixed header length of {@code 104} bytes.
     */
    GROUP_CALL_TOS_ACCEPTED(83, Mechanism.CALL_CHILD, "group_call_tos_accepted", 104),

    /**
     * Represents the acknowledgement of {@link #GROUP_CALL_TOS_ACCEPTED}.
     *
     * <p>This is a {@link Mechanism#ACK} leg: it rides a shared {@code <ack>} element and names no
     * dedicated {@code <call>} child, so {@link #wireTag()} is empty. The inbound validator expects a
     * fixed header length of {@code 104} bytes.
     */
    GROUP_CALL_TOS_ACCEPTED_ACK(84, Mechanism.ACK, null, 104);

    /**
     * Classifies how a signaling type is delivered on the wire.
     *
     * <p>The wa-voip engine carries action bearing messages as a dedicated child element of the
     * top level {@code <call>} stanza, but acknowledgement and receipt legs reuse the host stanza
     * layer's shared {@code <ack>} and {@code <receipt>} envelopes instead, and a few message type ids
     * occupy a table slot without any wire form at all. This split decides whether a type has a
     * {@link SignalingType#wireTag() wire tag} and whether it participates in
     * {@link SignalingType#ofWireTag(String)} child tag dispatch.
     */
    public enum Mechanism {
        /**
         * Marks the non transmittable {@link SignalingType#NONE} sentinel, which is neither an
         * action bearing child nor an envelope leg.
         */
        NONE,

        /**
         * Marks an action bearing type that is carried as its own child element inside the {@code <call>}
         * stanza.
         *
         * <p>These are the only types with a {@link SignalingType#wireTag() wire tag} and the only
         * types {@link SignalingType#ofWireTag(String)} resolves.
         */
        CALL_CHILD,

        /**
         * Marks an acknowledgement leg delivered inside the host stanza layer's shared {@code <ack>}
         * envelope rather than a dedicated {@code <call>} child.
         *
         * <p>The {@code <ack>} body echoes the named child stanza of the request it acknowledges, and the
         * engine reads the envelope {@code type} attribute to classify it. A type with this mechanism has
         * an empty {@link SignalingType#wireTag() wire tag}.
         */
        ACK,

        /**
         * Marks a receipt leg delivered inside the host stanza layer's shared {@code <receipt>} envelope
         * rather than a dedicated {@code <call>} child.
         *
         * <p>A type with this mechanism has an empty {@link SignalingType#wireTag() wire tag}.
         */
        RECEIPT,

        /**
         * Marks a native message type id that occupies a slot in the signaling message type table but
         * names no wire form: it neither rides a dedicated {@code <call>} child nor an
         * {@code <ack>}/{@code <receipt>} envelope.
         *
         * <p>These ids carry only a diagnostic display name and are loaded by no serializer or dispatch
         * comparison, so the engine never emits or classifies a wire child for them. A type with this
         * mechanism has an empty {@link SignalingType#wireTag() wire tag} and is absent from the
         * {@link SignalingType#ofWireTag(String)} index. It is distinct from {@link #NONE}, which is
         * the id-0 no match sentinel rather than a reserved table id.
         */
        INTERNAL
    }

    /**
     * Indexes the constants by their native message id for constant time {@link #ofIndex(int)} lookup.
     */
    private static final Map<Integer, SignalingType> BY_INDEX = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(SignalingType::index, type -> type));

    /**
     * Indexes the {@link Mechanism#CALL_CHILD} constants by their wire child tag for constant time
     * {@link #ofWireTag(String)} lookup.
     *
     * <p>Only action bearing types that name a dedicated {@code <call>} child participate; the
     * acknowledgement and receipt legs ride shared envelopes, and the {@link Mechanism#INTERNAL} reserved
     * ids and {@link #NONE} sentinel name no wire form, so all of those carry no child tag and are
     * excluded. The {@code extension} element is emitted by both {@link #ADD_EXTENSION} and
     * {@link #REMOVE_EXTENSION}; the merge keeps the lower native id ({@link #ADD_EXTENSION}) as the
     * canonical owner because the two are disambiguated downstream by message id rather than by element
     * tag.
     */
    private static final Map<String, SignalingType> BY_WIRE_TAG = Stream.of(values())
            .filter(type -> type.mechanism == Mechanism.CALL_CHILD)
            .collect(Collectors.toUnmodifiableMap(
                    type -> type.wireTag,
                    type -> type,
                    (left, right) -> left.index <= right.index ? left : right));

    /**
     * Holds the numeric message id the wa-voip engine dispatches on.
     */
    private final int index;

    /**
     * Holds the delivery mechanism that selects whether this type names a {@code <call>} child or rides
     * a shared envelope.
     */
    private final Mechanism mechanism;

    /**
     * Holds the lowercase wire child tag that names this action inside the {@code <call>} envelope, or
     * {@code null} when this type names no child element (an {@link Mechanism#ACK}/{@link Mechanism#RECEIPT}
     * envelope leg, the {@link Mechanism#NONE} sentinel, or an {@link Mechanism#INTERNAL} reserved id).
     */
    private final String wireTag;

    /**
     * Holds the fixed header byte length the inbound validator expects, or {@code -1} when no validator
     * case exists for this type and the message is variable length.
     */
    private final int fixedHeaderLength;

    /**
     * Constructs a constant bound to its native id, delivery mechanism, wire tag, and fixed header length.
     *
     * @param index             the numeric message id the engine dispatches on
     * @param mechanism         the delivery mechanism selecting child element versus envelope carriage
     * @param wireTag           the lowercase wire child tag naming this action, or {@code null} when this
     *                          type names no {@code <call>} child element
     * @param fixedHeaderLength the fixed header length, or {@code -1} when no validator case exists
     */
    SignalingType(int index, Mechanism mechanism, String wireTag, int fixedHeaderLength) {
        this.index = index;
        this.mechanism = mechanism;
        this.wireTag = wireTag;
        this.fixedHeaderLength = fixedHeaderLength;
    }

    /**
     * Returns the numeric message id the wa-voip engine dispatches on.
     *
     * <p>This id is the first word of the flattened call message and is stable across versions; it is
     * distinct from this constant's {@link Enum#ordinal()}, which has no protocol meaning because the
     * native id space contains the five gaps at 40, 41, 43, 44, and 64.
     *
     * @return the native message id
     */
    public int index() {
        return index;
    }

    /**
     * Returns the delivery mechanism that selects how this type is carried on the wire.
     *
     * <p>A {@link Mechanism#CALL_CHILD} type names a dedicated child element of the {@code <call>} stanza
     * and has a present {@link #wireTag()}; a {@link Mechanism#ACK} or {@link Mechanism#RECEIPT} type
     * rides the host stanza layer's shared envelope and has an empty {@link #wireTag()}; the
     * {@link Mechanism#INTERNAL} reserved ids and the {@link #NONE} sentinel name no wire form and also
     * have an empty {@link #wireTag()}.
     *
     * @return the delivery mechanism for this type
     */
    public Mechanism mechanism() {
        return mechanism;
    }

    /**
     * Returns the lowercase wire child tag that names this action inside the {@code <call>} envelope.
     *
     * <p>The result is present only for {@link Mechanism#CALL_CHILD} types, which name a dedicated child
     * element the receiver keys on; for {@link Mechanism#ACK} and {@link Mechanism#RECEIPT} legs, the
     * {@link Mechanism#INTERNAL} reserved ids, and the {@link #NONE} sentinel it is empty because they
     * carry no dedicated child element. A present tag is not necessarily a unique inbound dispatch key:
     * {@link #ADD_EXTENSION} and {@link #REMOVE_EXTENSION} both report {@code extension}, and
     * {@link #ofWireTag(String)} resolves that element to its canonical owner {@link #ADD_EXTENSION}.
     *
     * @return the wire child tag, or an empty result when this type names no {@code <call>} child element
     */
    public Optional<String> wireTag() {
        return Optional.ofNullable(wireTag);
    }

    /**
     * Returns the fixed header byte length the inbound validator expects for this type, if present.
     *
     * <p>The wa-voip header validator compares each flattened message's header length against a per type
     * expected length. Types that have no validator case carry no validated fixed length and yield an
     * empty result; those callers validate inbound shape by checking the required attributes of the
     * decoded record instead. A length of zero ({@link #MUTE}, {@link #WEB_CLIENT}) is a present
     * {@code OptionalInt.of(0)}, distinct from the empty result of a type with no validator case.
     *
     * @return the expected fixed header length, or an empty result when no validator case exists
     */
    public OptionalInt fixedHeaderLength() {
        return fixedHeaderLength < 0 ? OptionalInt.empty() : OptionalInt.of(fixedHeaderLength);
    }

    /**
     * Looks up the signaling type for a native message id.
     *
     * <p>The lookup is keyed on the protocol id, not on {@link Enum#ordinal()}. An id outside the
     * taxonomy, including any of the five gap ids, yields an empty result.
     *
     * @param index the native message id
     * @return the matching type, or an empty result when no type carries the id
     */
    public static Optional<SignalingType> ofIndex(int index) {
        return Optional.ofNullable(BY_INDEX.get(index));
    }

    /**
     * Looks up the signaling type for an inbound wire child tag.
     *
     * <p>This is the authoritative inbound dispatch path for action bearing messages: the receiver reads
     * the single child element tag of a {@code <call>} stanza and resolves it here. Only
     * {@link Mechanism#CALL_CHILD} types are resolvable; acknowledgement and receipt legs are classified
     * by the host stanza layer through the {@code <ack>}/{@code <receipt>} envelope, not through this
     * path. The shared {@code extension} element resolves to its canonical owner {@link #ADD_EXTENSION};
     * a {@link #REMOVE_EXTENSION} is then distinguished downstream by message id. A tag this taxonomy
     * does not declare, including {@code null}, yields an empty result so the caller can drop or buffer
     * the message. Matching is case sensitive because wire tags are always lowercase.
     *
     * @param wireTag the wire child tag, or {@code null}
     * @return the matching type, or an empty result when no {@link Mechanism#CALL_CHILD} type carries the tag
     */
    public static Optional<SignalingType> ofWireTag(String wireTag) {
        if (wireTag == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_WIRE_TAG.get(wireTag));
    }
}

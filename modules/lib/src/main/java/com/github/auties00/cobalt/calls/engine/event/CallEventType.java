package com.github.auties00.cobalt.calls.engine.event;

import java.util.Optional;

/**
 * Enumerates the 172 events of the WhatsApp VOIP in call event bus.
 *
 * <p>Every state change inside the WhatsApp VOIP engine funnels through a single generic dispatcher,
 * which selects the event by a small integer id and, in debug builds, logs a human readable display name
 * for it. This enum is a one to one transcription of that id space: there is exactly one constant per
 * native id, the constants appear in id order, and each constant's {@link #index()} equals its native id.
 * The id space is dense and has no gaps from {@code 0x00} through {@code 0xab}, so {@link #index()} also
 * equals {@link Enum#ordinal()}. The engine display name for each id is preserved verbatim by
 * {@link #displayName()} as a diagnostic aid; it is the engine's own log string, not a wire token, and
 * carries no protocol meaning.
 *
 * <p>The id is the dispatch key; callers that receive a raw id from the engine boundary resolve it
 * through the constant time {@link #ofIndex(int)}. An id outside the table yields an empty result so the
 * caller can drop the event rather than fail. The host facing subset of these events (state changes,
 * mute and video and screen share transitions, call link and waiting room acks, reactions, transcripts,
 * and so on) is the set fanned out to listeners after the engine's emit gate; the remainder are internal
 * lifecycle and diagnostic ids that the gate suppresses.
 *
 * <p>One id, {@code 0x76} ({@link #RESERVED_0X76}), carries no display name: its in bounds display name
 * pointer targets the engine's interned empty string, so the engine emits a blank label for it and its
 * human readable name is absent. It is retained as a reserved constant to keep every following constant's
 * {@link #index()} aligned with the native table.
 *
 * @implNote This implementation transcribes a 172 entry event display name table: each entry is the
 * {@code "EVENT: ..."} string the dispatcher logs for that id, and the constants are emitted in table
 * order with their id equal to their table position. 171 of the 172 slots resolve to a distinct
 * {@code "EVENT: ..."} literal; the single slot at index {@code 0x76} points to the interned empty string
 * and is represented by {@link #RESERVED_0X76}. The {@code "EVENT: "} prefix is stripped from each
 * {@link #displayName()}; the spelling of every retained literal is otherwise unchanged, including the
 * engine's own typo {@code "Self Sate Changed"} at id {@code 0x8b}, which is carried verbatim in the
 * display name while the constant uses the corrected identifier {@link #CALL_LINK_LOBBY_SELF_STATE_CHANGED}.
 */
public enum CallEventType {
    /**
     * Identifies the WhatsApp VOIP {@code Undefined event} event, native id {@code 0x00}.
     */
    UNDEFINED_EVENT(0, "Undefined event"),

    /**
     * Identifies the WhatsApp VOIP {@code Call offer sent} event, native id {@code 0x01}.
     */
    CALL_OFFER_SENT(1, "Call offer sent"),

    /**
     * Identifies the WhatsApp VOIP {@code Call offer received} event, native id {@code 0x02}.
     */
    CALL_OFFER_RECEIVED(2, "Call offer received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call offer ack received} event, native id {@code 0x03}.
     */
    CALL_OFFER_ACK_RECEIVED(3, "Call offer ack received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call offer nack received} event, native id {@code 0x04}.
     */
    CALL_OFFER_NACK_RECEIVED(4, "Call offer nack received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call offer receipt received} event, native id {@code 0x05}.
     */
    CALL_OFFER_RECEIPT_RECEIVED(5, "Call offer receipt received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call accept failed} event, native id {@code 0x06}.
     */
    CALL_ACCEPT_FAILED(6, "Call accept failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Call accept sent} event, native id {@code 0x07}.
     */
    CALL_ACCEPT_SENT(7, "Call accept sent"),

    /**
     * Identifies the WhatsApp VOIP {@code Call accept received} event, native id {@code 0x08}.
     */
    CALL_ACCEPT_RECEIVED(8, "Call accept received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call preaccept received} event, native id {@code 0x09}.
     */
    CALL_PREACCEPT_RECEIVED(9, "Call preaccept received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call terminate received} event, native id {@code 0x0a}.
     */
    CALL_TERMINATE_RECEIVED(10, "Call terminate received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call reject received} event, native id {@code 0x0b}.
     */
    CALL_REJECT_RECEIVED(11, "Call reject received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call offer resent} event, native id {@code 0x0c}.
     */
    CALL_OFFER_RESENT(12, "Call offer resent"),

    /**
     * Identifies the WhatsApp VOIP {@code Audio stream started} event, native id {@code 0x0d}.
     */
    AUDIO_STREAM_STARTED(13, "Audio stream started"),

    /**
     * Identifies the WhatsApp VOIP {@code P2P negotiation success} event, native id {@code 0x0e}.
     */
    P2P_NEGOTIATION_SUCCESS(14, "P2P negotiation success"),

    /**
     * Identifies the WhatsApp VOIP {@code Relay create success} event, native id {@code 0x0f}.
     */
    RELAY_CREATE_SUCCESS(15, "Relay create success"),

    /**
     * Identifies the WhatsApp VOIP {@code Call state changed} event, native id {@code 0x10}.
     */
    CALL_STATE_CHANGED(16, "Call state changed"),

    /**
     * Identifies the WhatsApp VOIP {@code P2P negotiation failed} event, native id {@code 0x11}.
     */
    P2P_NEGOTIATION_FAILED(17, "P2P negotiation failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Media stream error} event, native id {@code 0x12}.
     */
    MEDIA_STREAM_ERROR(18, "Media stream error"),

    /**
     * Identifies the WhatsApp VOIP {@code Audio init error} event, native id {@code 0x13}.
     */
    AUDIO_INIT_ERROR(19, "Audio init error"),

    /**
     * Identifies the WhatsApp VOIP {@code No available audio record sampling rate} event, native id {@code 0x14}.
     */
    NO_AVAILABLE_AUDIO_RECORD_SAMPLING_RATE(20, "No available audio record sampling rate"),

    /**
     * Identifies the WhatsApp VOIP {@code Call offer send failed} event, native id {@code 0x15}.
     */
    CALL_OFFER_SEND_FAILED(21, "Call offer send failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Processing call offer failed} event, native id {@code 0x16}.
     */
    PROCESSING_CALL_OFFER_FAILED(22, "Processing call offer failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Call accept send failed} event, native id {@code 0x17}.
     */
    CALL_ACCEPT_SEND_FAILED(23, "Call accept send failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Processing call preaccept failed} event, native id {@code 0x18}.
     */
    PROCESSING_CALL_PREACCEPT_FAILED(24, "Processing call preaccept failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Processing call accept failed} event, native id {@code 0x19}.
     */
    PROCESSING_CALL_ACCEPT_FAILED(25, "Processing call accept failed"),

    /**
     * Identifies the WhatsApp VOIP {@code About to create sound port} event, native id {@code 0x1a}.
     */
    ABOUT_TO_CREATE_SOUND_PORT(26, "About to create sound port"),

    /**
     * Identifies the WhatsApp VOIP {@code Sound port create failed} event, native id {@code 0x1b}.
     */
    SOUND_PORT_CREATE_FAILED(27, "Sound port create failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Transport cand addr send failed} event, native id {@code 0x1c}.
     */
    TRANSPORT_CAND_ADDR_SEND_FAILED(28, "Transport cand addr send failed"),

    /**
     * Identifies the WhatsApp VOIP {@code P2P transport create failed} event, native id {@code 0x1d}.
     */
    P2P_TRANSPORT_CREATE_FAILED(29, "P2P transport create failed"),

    /**
     * Identifies the WhatsApp VOIP {@code P2P transp media create failed} event, native id {@code 0x1e}.
     */
    P2P_TRANSP_MEDIA_CREATE_FAILED(30, "P2P transp media create failed"),

    /**
     * Identifies the WhatsApp VOIP {@code P2P transport start failed} event, native id {@code 0x1f}.
     */
    P2P_TRANSPORT_START_FAILED(31, "P2P transport start failed"),

    /**
     * Identifies the WhatsApp VOIP {@code P2P transport restart success} event, native id {@code 0x20}.
     */
    P2P_TRANSPORT_RESTART_SUCCESS(32, "P2P transport restart success"),

    /**
     * Identifies the WhatsApp VOIP {@code Relay info missing or invalid} event, native id {@code 0x21}.
     */
    RELAY_INFO_MISSING_OR_INVALID(33, "Relay info missing or invalid"),

    /**
     * Identifies the WhatsApp VOIP {@code Gathering host candidates failed} event, native id {@code 0x22}.
     */
    GATHERING_HOST_CANDIDATES_FAILED(34, "Gathering host candidates failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Media stream start error} event, native id {@code 0x23}.
     */
    MEDIA_STREAM_START_ERROR(35, "Media stream start error"),

    /**
     * Identifies the WhatsApp VOIP {@code Relay latency info send failed} event, native id {@code 0x24}.
     */
    RELAY_LATENCY_INFO_SEND_FAILED(36, "Relay latency info send failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Relay election send failed - DEPRECATED} event, native id {@code 0x25}.
     */
    RELAY_ELECTION_SEND_FAILED_DEPRECATED(37, "Relay election send failed - DEPRECATED"),

    /**
     * Identifies the WhatsApp VOIP {@code Call is ending} event, native id {@code 0x26}.
     */
    CALL_IS_ENDING(38, "Call is ending"),

    /**
     * Identifies the WhatsApp VOIP {@code Call capture buffer is filled} event, native id {@code 0x27}.
     */
    CALL_CAPTURE_BUFFER_IS_FILLED(39, "Call capture buffer is filled"),

    /**
     * Identifies the WhatsApp VOIP {@code Call capture finished} event, native id {@code 0x28}.
     */
    CALL_CAPTURE_FINISHED(40, "Call capture finished"),

    /**
     * Identifies the WhatsApp VOIP {@code Timeout when receiving packets} event, native id {@code 0x29}.
     */
    TIMEOUT_WHEN_RECEIVING_PACKETS(41, "Timeout when receiving packets"),

    /**
     * Identifies the WhatsApp VOIP {@code Timeout when sending packets} event, native id {@code 0x2a}.
     */
    TIMEOUT_WHEN_SENDING_PACKETS(42, "Timeout when sending packets"),

    /**
     * Identifies the WhatsApp VOIP {@code Receive traffic started} event, native id {@code 0x2b}.
     */
    RECEIVE_TRAFFIC_STARTED(43, "Receive traffic started"),

    /**
     * Identifies the WhatsApp VOIP {@code Receive traffic stopped} event, native id {@code 0x2c}.
     */
    RECEIVE_TRAFFIC_STOPPED(44, "Receive traffic stopped"),

    /**
     * Identifies the WhatsApp VOIP {@code RTCP packet received} event, native id {@code 0x2d}.
     */
    RTCP_PACKET_RECEIVED(45, "RTCP packet received"),

    /**
     * Identifies the WhatsApp VOIP {@code RTCP Bye received} event, native id {@code 0x2e}.
     */
    RTCP_BYE_RECEIVED(46, "RTCP Bye received"),

    /**
     * Identifies the WhatsApp VOIP {@code Relay binds failed} event, native id {@code 0x2f}.
     */
    RELAY_BINDS_FAILED(47, "Relay binds failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Sound port created} event, native id {@code 0x30}.
     */
    SOUND_PORT_CREATED(48, "Sound port created"),

    /**
     * Identifies the WhatsApp VOIP {@code Audio driver restart} event, native id {@code 0x31}.
     */
    AUDIO_DRIVER_RESTART(49, "Audio driver restart"),

    /**
     * Identifies the WhatsApp VOIP {@code Echo} event, native id {@code 0x32}.
     */
    ECHO(50, "Echo"),

    /**
     * Identifies the WhatsApp VOIP {@code Self Video state changed} event, native id {@code 0x33}.
     */
    SELF_VIDEO_STATE_CHANGED(51, "Self Video state changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Peer Video state changed} event, native id {@code 0x34}.
     */
    PEER_VIDEO_STATE_CHANGED(52, "Peer Video state changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Video port created} event, native id {@code 0x35}.
     */
    VIDEO_PORT_CREATED(53, "Video port created"),

    /**
     * Identifies the WhatsApp VOIP {@code Video port create failed} event, native id {@code 0x36}.
     */
    VIDEO_PORT_CREATE_FAILED(54, "Video port create failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Video decode started} event, native id {@code 0x37}.
     */
    VIDEO_DECODE_STARTED(55, "Video decode started"),

    /**
     * Identifies the WhatsApp VOIP {@code Video render started} event, native id {@code 0x38}.
     */
    VIDEO_RENDER_STARTED(56, "Video render started"),

    /**
     * Identifies the WhatsApp VOIP {@code Video capture started} event, native id {@code 0x39}.
     */
    VIDEO_CAPTURE_STARTED(57, "Video capture started"),

    /**
     * Identifies the WhatsApp VOIP {@code Video preview failed} event, native id {@code 0x3a}.
     */
    VIDEO_PREVIEW_FAILED(58, "Video preview failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Video preview ready} event, native id {@code 0x3b}.
     */
    VIDEO_PREVIEW_READY(59, "Video preview ready"),

    /**
     * Identifies the WhatsApp VOIP {@code Video preview minimized} event, native id {@code 0x3c}.
     */
    VIDEO_PREVIEW_MINIMIZED(60, "Video preview minimized"),

    /**
     * Identifies the WhatsApp VOIP {@code Video stream create error} event, native id {@code 0x3d}.
     */
    VIDEO_STREAM_CREATE_ERROR(61, "Video stream create error"),

    /**
     * Identifies the WhatsApp VOIP {@code Video render format changed} event, native id {@code 0x3e}.
     */
    VIDEO_RENDER_FORMAT_CHANGED(62, "Video render format changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Video codec mismatch} event, native id {@code 0x3f}.
     */
    VIDEO_CODEC_MISMATCH(63, "Video codec mismatch"),

    /**
     * Identifies the WhatsApp VOIP {@code Video decode paused} event, native id {@code 0x40}.
     */
    VIDEO_DECODE_PAUSED(64, "Video decode paused"),

    /**
     * Identifies the WhatsApp VOIP {@code Video decode resumed} event, native id {@code 0x41}.
     */
    VIDEO_DECODE_RESUMED(65, "Video decode resumed"),

    /**
     * Identifies the WhatsApp VOIP {@code Video Encode Fatal Error} event, native id {@code 0x42}.
     */
    VIDEO_ENCODE_FATAL_ERROR(66, "Video Encode Fatal Error"),

    /**
     * Identifies the WhatsApp VOIP {@code Video Decode Fatal Error} event, native id {@code 0x43}.
     */
    VIDEO_DECODE_FATAL_ERROR(67, "Video Decode Fatal Error"),

    /**
     * Identifies the WhatsApp VOIP {@code Battery level low} event, native id {@code 0x44}.
     */
    BATTERY_LEVEL_LOW(68, "Battery level low"),

    /**
     * Identifies the WhatsApp VOIP {@code Peer battery level low} event, native id {@code 0x45}.
     */
    PEER_BATTERY_LEVEL_LOW(69, "Peer battery level low"),

    /**
     * Identifies the WhatsApp VOIP {@code Group info changed} event, native id {@code 0x46}.
     */
    GROUP_INFO_CHANGED(70, "Group info changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Field stats ready} event, native id {@code 0x47}.
     */
    FIELD_STATS_READY(71, "Field stats ready"),

    /**
     * Identifies the WhatsApp VOIP {@code Call waiting state changed} event, native id {@code 0x48}.
     */
    CALL_WAITING_STATE_CHANGED(72, "Call waiting state changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Mute state changed} event, native id {@code 0x49}.
     */
    MUTE_STATE_CHANGED(73, "Mute state changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Interruption state changed} event, native id {@code 0x4a}.
     */
    INTERRUPTION_STATE_CHANGED(74, "Interruption state changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Incoming traffic state for peer changed} event, native id {@code 0x4b}.
     */
    INCOMING_TRAFFIC_STATE_FOR_PEER_CHANGED(75, "Incoming traffic state for peer changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Processing call accept receipt failed} event, native id {@code 0x4c}.
     */
    PROCESSING_CALL_ACCEPT_RECEIPT_FAILED(76, "Processing call accept receipt failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Group Participant Left} event, native id {@code 0x4d}.
     */
    GROUP_PARTICIPANT_LEFT(77, "Group Participant Left"),

    /**
     * Identifies the WhatsApp VOIP {@code Request to change audio route} event, native id {@code 0x4e}.
     */
    REQUEST_TO_CHANGE_AUDIO_ROUTE(78, "Request to change audio route"),

    /**
     * Identifies the WhatsApp VOIP {@code Handle accept ack failed} event, native id {@code 0x4f}.
     */
    HANDLE_ACCEPT_ACK_FAILED(79, "Handle accept ack failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Call missed by the user} event, native id {@code 0x50}.
     */
    CALL_MISSED_BY_THE_USER(80, "Call missed by the user"),

    /**
     * Identifies the WhatsApp VOIP {@code Weak WiFi switched to cellular} event, native id {@code 0x51}.
     */
    WEAK_WIFI_SWITCHED_TO_CELLULAR(81, "Weak WiFi switched to cellular"),

    /**
     * Identifies the WhatsApp VOIP {@code Auto switched to the new call} event, native id {@code 0x52}.
     */
    AUTO_SWITCHED_TO_THE_NEW_CALL(82, "Auto switched to the new call"),

    /**
     * Identifies the WhatsApp VOIP {@code Call Rejected Decryption Failure} event, native id {@code 0x53}.
     */
    CALL_REJECTED_DECRYPTION_FAILURE(83, "Call Rejected Decryption Failure"),

    /**
     * Identifies the WhatsApp VOIP {@code Peer display orientation changed} event, native id {@code 0x54}.
     */
    PEER_DISPLAY_ORIENTATION_CHANGED(84, "Peer display orientation changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Handle offer ack failed} event, native id {@code 0x55}.
     */
    HANDLE_OFFER_ACK_FAILED(85, "Handle offer ack failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Pending call auto rejected} event, native id {@code 0x56}.
     */
    PENDING_CALL_AUTO_REJECTED(86, "Pending call auto rejected"),

    /**
     * Identifies the WhatsApp VOIP {@code File descriptor leak detected} event, native id {@code 0x57}.
     */
    FILE_DESCRIPTOR_LEAK_DETECTED(87, "File descriptor leak detected"),

    /**
     * Identifies the WhatsApp VOIP {@code Restart camera} event, native id {@code 0x58}.
     */
    RESTART_CAMERA(88, "Restart camera"),

    /**
     * Identifies the WhatsApp VOIP {@code Audio test replay finished} event, native id {@code 0x59}.
     */
    AUDIO_TEST_REPLAY_FINISHED(89, "Audio test replay finished"),

    /**
     * Identifies the WhatsApp VOIP {@code Sync devices} event, native id {@code 0x5a}.
     */
    SYNC_DEVICES(90, "Sync devices"),

    /**
     * Identifies the WhatsApp VOIP {@code Video codec state changed} event, native id {@code 0x5b}.
     */
    VIDEO_CODEC_STATE_CHANGED(91, "Video codec state changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Call Fatal} event, native id {@code 0x5c}.
     */
    CALL_FATAL(92, "Call Fatal"),

    /**
     * Identifies the WhatsApp VOIP {@code Update joinable call log} event, native id {@code 0x5d}.
     */
    UPDATE_JOINABLE_CALL_LOG(93, "Update joinable call log"),

    /**
     * Identifies the WhatsApp VOIP {@code Lobby nack received} event, native id {@code 0x5e}.
     */
    LOBBY_NACK_RECEIVED(94, "Lobby nack received"),

    /**
     * Identifies the WhatsApp VOIP {@code Play call tone} event, native id {@code 0x5f}.
     */
    PLAY_CALL_TONE(95, "Play call tone"),

    /**
     * Identifies the WhatsApp VOIP {@code Send joinable client poll critical event} event, native id {@code 0x60}.
     */
    SEND_JOINABLE_CLIENT_POLL_CRITICAL_EVENT(96, "Send joinable client poll critical event"),

    /**
     * Identifies the WhatsApp VOIP {@code Send linked group call downgraded critical event} event, native id {@code 0x61}.
     */
    SEND_LINKED_GROUP_CALL_DOWNGRADED_CRITICAL_EVENT(97, "Send linked group call downgraded critical event"),

    /**
     * Identifies the WhatsApp VOIP {@code Update Voip settings} event, native id {@code 0x62}.
     */
    UPDATE_VOIP_SETTINGS(98, "Update Voip settings"),

    /**
     * Identifies the WhatsApp VOIP {@code Critical error in VoIP stack} event, native id {@code 0x63}.
     */
    CRITICAL_ERROR_IN_VOIP_STACK(99, "Critical error in VoIP stack"),

    /**
     * Identifies the WhatsApp VOIP {@code Speaker status changed} event, native id {@code 0x64}.
     */
    SPEAKER_STATUS_CHANGED(100, "Speaker status changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Lonely state timeout} event, native id {@code 0x65}.
     */
    LONELY_STATE_TIMEOUT(101, "Lonely state timeout"),

    /**
     * Identifies the WhatsApp VOIP {@code Mute by another participant} event, native id {@code 0x66}.
     */
    MUTE_BY_ANOTHER_PARTICIPANT(102, "Mute by another participant"),

    /**
     * Identifies the WhatsApp VOIP {@code Link_create ack received} event, native id {@code 0x67}.
     */
    LINK_CREATE_ACK_RECEIVED(103, "Link_create ack received"),

    /**
     * Identifies the WhatsApp VOIP {@code Link_create nack received} event, native id {@code 0x68}.
     */
    LINK_CREATE_NACK_RECEIVED(104, "Link_create nack received"),

    /**
     * Identifies the WhatsApp VOIP {@code Heartbeat nack received} event, native id {@code 0x69}.
     */
    HEARTBEAT_NACK_RECEIVED(105, "Heartbeat nack received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call link state changed} event, native id {@code 0x6a}.
     */
    CALL_LINK_STATE_CHANGED(106, "Call link state changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Lobby timeout} event, native id {@code 0x6b}.
     */
    LOBBY_TIMEOUT(107, "Lobby timeout"),

    /**
     * Identifies the WhatsApp VOIP {@code Mute request failed} event, native id {@code 0x6c}.
     */
    MUTE_REQUEST_FAILED(108, "Mute request failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Link_query nack received} event, native id {@code 0x6d}.
     */
    LINK_QUERY_NACK_RECEIVED(109, "Link_query nack received"),

    /**
     * Identifies the WhatsApp VOIP {@code Link_join nack received} event, native id {@code 0x6e}.
     */
    LINK_JOIN_NACK_RECEIVED(110, "Link_join nack received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call grid ranking changed} event, native id {@code 0x6f}.
     */
    CALL_GRID_RANKING_CHANGED(111, "Call grid ranking changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Group Call Buffer Handle Messages} event, native id {@code 0x70}.
     */
    GROUP_CALL_BUFFER_HANDLE_MESSAGES(112, "Group Call Buffer Handle Messages"),

    /**
     * Identifies the WhatsApp VOIP {@code Remove_user nack received} event, native id {@code 0x71}.
     */
    REMOVE_USER_NACK_RECEIVED(113, "Remove_user nack received"),

    /**
     * Identifies the WhatsApp VOIP {@code Video Rendering State Changed} event, native id {@code 0x72}.
     */
    VIDEO_RENDERING_STATE_CHANGED(114, "Video Rendering State Changed"),

    /**
     * Identifies the WhatsApp VOIP {@code User removed} event, native id {@code 0x73}.
     */
    USER_REMOVED(115, "User removed"),

    /**
     * Identifies the WhatsApp VOIP {@code Screen Share} event, native id {@code 0x74}.
     */
    SCREEN_SHARE(116, "Screen Share"),

    /**
     * Identifies the WhatsApp VOIP {@code Network Health Status Changed} event, native id {@code 0x75}.
     */
    NETWORK_HEALTH_STATUS_CHANGED(117, "Network Health Status Changed"),

    /**
     * Reserves event id {@code 0x76}, whose engine display name is blank.
     *
     * <p>The display name pointer at this table slot is in bounds but targets the engine's interned
     * empty string rather than an {@code "EVENT: ..."} literal, so the engine carries no label for this
     * event and its meaning cannot be recovered from the display name alone. The slot is retained so that
     * every constant's {@link #index()} stays aligned with its native table position; it lies between
     * {@link #NETWORK_HEALTH_STATUS_CHANGED} ({@code 0x75}) and {@link #HIGH_DATA_USAGE_DETECTED}
     * ({@code 0x77}), placing it in the same network and data usage diagnostic cluster.
     */
    RESERVED_0X76(118, ""),

    /**
     * Identifies the WhatsApp VOIP {@code High Data Usage Detected} event, native id {@code 0x77}.
     */
    HIGH_DATA_USAGE_DETECTED(119, "High Data Usage Detected"),

    /**
     * Identifies the WhatsApp VOIP {@code Lid Caller Display Info} event, native id {@code 0x78}.
     */
    LID_CALLER_DISPLAY_INFO(120, "Lid Caller Display Info"),

    /**
     * Identifies the WhatsApp VOIP {@code Eager call dismiss} event, native id {@code 0x79}.
     */
    EAGER_CALL_DISMISS(121, "Eager call dismiss"),

    /**
     * Identifies the WhatsApp VOIP {@code Offer peek timeout} event, native id {@code 0x7a}.
     */
    OFFER_PEEK_TIMEOUT(122, "Offer peek timeout"),

    /**
     * Identifies the WhatsApp VOIP {@code Network Health Status Changed V2} event, native id {@code 0x7b}.
     */
    NETWORK_HEALTH_STATUS_CHANGED_V2(123, "Network Health Status Changed V2"),

    /**
     * Identifies the WhatsApp VOIP {@code Auto Video Pause Status Changed} event, native id {@code 0x7c}.
     */
    AUTO_VIDEO_PAUSE_STATUS_CHANGED(124, "Auto Video Pause Status Changed"),

    /**
     * Identifies the WhatsApp VOIP {@code BCall Created} event, native id {@code 0x7d}.
     */
    BCALL_CREATED(125, "BCall Created"),

    /**
     * Identifies the WhatsApp VOIP {@code BCall Create Failed} event, native id {@code 0x7e}.
     */
    BCALL_CREATE_FAILED(126, "BCall Create Failed"),

    /**
     * Identifies the WhatsApp VOIP {@code BCall Audience Updated} event, native id {@code 0x7f}.
     */
    BCALL_AUDIENCE_UPDATED(127, "BCall Audience Updated"),

    /**
     * Identifies the WhatsApp VOIP {@code Call summary received} event, native id {@code 0x80}.
     */
    CALL_SUMMARY_RECEIVED(128, "Call summary received"),

    /**
     * Identifies the WhatsApp VOIP {@code BCall Join Failed} event, native id {@code 0x81}.
     */
    BCALL_JOIN_FAILED(129, "BCall Join Failed"),

    /**
     * Identifies the WhatsApp VOIP {@code BCall End Failed} event, native id {@code 0x82}.
     */
    BCALL_END_FAILED(130, "BCall End Failed"),

    /**
     * Identifies the WhatsApp VOIP {@code BCall Joined} event, native id {@code 0x83}.
     */
    BCALL_JOINED(131, "BCall Joined"),

    /**
     * Identifies the WhatsApp VOIP {@code BCall Leave Failed} event, native id {@code 0x84}.
     */
    BCALL_LEAVE_FAILED(132, "BCall Leave Failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Content detector type} event, native id {@code 0x85}.
     */
    CONTENT_DETECTOR_TYPE(133, "Content detector type"),

    /**
     * Identifies the WhatsApp VOIP {@code BCall Ended} event, native id {@code 0x86}.
     */
    BCALL_ENDED(134, "BCall Ended"),

    /**
     * Identifies the WhatsApp VOIP {@code BCall start notify} event, native id {@code 0x87}.
     */
    BCALL_START_NOTIFY(135, "BCall start notify"),

    /**
     * Identifies the WhatsApp VOIP {@code Link Edit success} event, native id {@code 0x88}.
     */
    LINK_EDIT_SUCCESS(136, "Link Edit success"),

    /**
     * Identifies the WhatsApp VOIP {@code Link Edit failed} event, native id {@code 0x89}.
     */
    LINK_EDIT_FAILED(137, "Link Edit failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Update 1on1 CallLog} event, native id {@code 0x8a}.
     */
    UPDATE_1ON1_CALL_LOG(138, "Update 1on1 CallLog"),

    /**
     * Identifies the WhatsApp VOIP {@code Call Link Lobby Self Sate Changed} event, native id {@code 0x8b}.
     *
     * <p>The engine display name spells the literal {@code "Call Link Lobby Self Sate Changed"}; that
     * spelling is carried verbatim by {@link #displayName()} while this constant uses the corrected
     * identifier.
     */
    CALL_LINK_LOBBY_SELF_STATE_CHANGED(139, "Call Link Lobby Self Sate Changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Data Channel Ready} event, native id {@code 0x8c}.
     */
    DATA_CHANNEL_READY(140, "Data Channel Ready"),

    /**
     * Identifies the WhatsApp VOIP {@code Audio Tx Started} event, native id {@code 0x8d}.
     */
    AUDIO_TX_STARTED(141, "Audio Tx Started"),

    /**
     * Identifies the WhatsApp VOIP {@code Group Call Reminder} event, native id {@code 0x8e}.
     */
    GROUP_CALL_REMINDER(142, "Group Call Reminder"),

    /**
     * Identifies the WhatsApp VOIP {@code Voice chat wave received} event, native id {@code 0x8f}.
     */
    VOICE_CHAT_WAVE_RECEIVED(143, "Voice chat wave received"),

    /**
     * Identifies the WhatsApp VOIP {@code Data Channel Connection Timeout} event, native id {@code 0x90}.
     */
    DATA_CHANNEL_CONNECTION_TIMEOUT(144, "Data Channel Connection Timeout"),

    /**
     * Identifies the WhatsApp VOIP {@code Reaction State Changed} event, native id {@code 0x91}.
     */
    REACTION_STATE_CHANGED(145, "Reaction State Changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Video state changed} event, native id {@code 0x92}.
     */
    VIDEO_STATE_CHANGED(146, "Video state changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Peer video permission changed} event, native id {@code 0x93}.
     */
    PEER_VIDEO_PERMISSION_CHANGED(147, "Peer video permission changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Raise Hand State Changed} event, native id {@code 0x94}.
     */
    RAISE_HAND_STATE_CHANGED(148, "Raise Hand State Changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Bot Reconfigure Success} event, native id {@code 0x95}.
     */
    BOT_RECONFIGURE_SUCCESS(149, "Bot Reconfigure Success"),

    /**
     * Identifies the WhatsApp VOIP {@code Audio Device Ready} event, native id {@code 0x96}.
     */
    AUDIO_DEVICE_READY(150, "Audio Device Ready"),

    /**
     * Identifies the WhatsApp VOIP {@code Bot Early Connect} event, native id {@code 0x97}.
     */
    BOT_EARLY_CONNECT(151, "Bot Early Connect"),

    /**
     * Identifies the WhatsApp VOIP {@code Microphone device ready} event, native id {@code 0x98}.
     */
    MICROPHONE_DEVICE_READY(152, "Microphone device ready"),

    /**
     * Identifies the WhatsApp VOIP {@code Speaker device ready} event, native id {@code 0x99}.
     */
    SPEAKER_DEVICE_READY(153, "Speaker device ready"),

    /**
     * Identifies the WhatsApp VOIP {@code Wearable Attribution State Changed} event, native id {@code 0x9a}.
     */
    WEARABLE_ATTRIBUTION_STATE_CHANGED(154, "Wearable Attribution State Changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Transcript Received} event, native id {@code 0x9b}.
     */
    TRANSCRIPT_RECEIVED(155, "Transcript Received"),

    /**
     * Identifies the WhatsApp VOIP {@code Relay List Update} event, native id {@code 0x9c}.
     */
    RELAY_LIST_UPDATE(156, "Relay List Update"),

    /**
     * Identifies the WhatsApp VOIP {@code Waiting room denied} event, native id {@code 0x9d}.
     */
    WAITING_ROOM_DENIED(157, "Waiting room denied"),

    /**
     * Identifies the WhatsApp VOIP {@code Waiting room state changed} event, native id {@code 0x9e}.
     */
    WAITING_ROOM_STATE_CHANGED(158, "Waiting room state changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Remove failed} event, native id {@code 0x9f}.
     */
    REMOVE_FAILED(159, "Remove failed"),

    /**
     * Identifies the WhatsApp VOIP {@code Bot Presence Changed} event, native id {@code 0xa0}.
     */
    BOT_PRESENCE_CHANGED(160, "Bot Presence Changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Waiting room toggle acked} event, native id {@code 0xa1}.
     */
    WAITING_ROOM_TOGGLE_ACKED(161, "Waiting room toggle acked"),

    /**
     * Identifies the WhatsApp VOIP {@code Link query acked} event, native id {@code 0xa2}.
     */
    LINK_QUERY_ACKED(162, "Link query acked"),

    /**
     * Identifies the WhatsApp VOIP {@code Encode target FPS changed} event, native id {@code 0xa3}.
     */
    ENCODE_TARGET_FPS_CHANGED(163, "Encode target FPS changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Encode params changed} event, native id {@code 0xa4}.
     */
    ENCODE_PARAMS_CHANGED(164, "Encode params changed"),

    /**
     * Identifies the WhatsApp VOIP {@code Waiting room admit acked} event, native id {@code 0xa5}.
     */
    WAITING_ROOM_ADMIT_ACKED(165, "Waiting room admit acked"),

    /**
     * Identifies the WhatsApp VOIP {@code Waiting room deny acked} event, native id {@code 0xa6}.
     */
    WAITING_ROOM_DENY_ACKED(166, "Waiting room deny acked"),

    /**
     * Identifies the WhatsApp VOIP {@code P2P Transport Update} event, native id {@code 0xa7}.
     */
    P2P_TRANSPORT_UPDATE(167, "P2P Transport Update"),

    /**
     * Identifies the WhatsApp VOIP {@code Call Add Extension Received} event, native id {@code 0xa8}.
     */
    CALL_ADD_EXTENSION_RECEIVED(168, "Call Add Extension Received"),

    /**
     * Identifies the WhatsApp VOIP {@code Call Add Extension Success} event, native id {@code 0xa9}.
     */
    CALL_ADD_EXTENSION_SUCCESS(169, "Call Add Extension Success"),

    /**
     * Identifies the WhatsApp VOIP {@code Call Add Extension Failure} event, native id {@code 0xaa}.
     */
    CALL_ADD_EXTENSION_FAILURE(170, "Call Add Extension Failure"),

    /**
     * Identifies the WhatsApp VOIP {@code AI TOS Accept Failed} event, native id {@code 0xab}.
     */
    AI_TOS_ACCEPT_FAILED(171, "AI TOS Accept Failed");

    /**
     * Indexes the constants by their native event id for constant time {@link #ofIndex(int)} lookup.
     *
     * <p>The id space is dense and has no gaps, so a constant's native id equals its {@link Enum#ordinal()}
     * and {@link #values()} is already ordered by id: slot {@code i} holds the constant whose
     * {@link #index()} is {@code i}.
     */
    private static final CallEventType[] BY_INDEX = values();

    /**
     * Holds the native event id the WhatsApp VOIP dispatcher selects on.
     */
    private final int index;

    /**
     * Holds the recovered engine display name with its {@code "EVENT: "} prefix stripped, or an empty
     * string for the reserved slot at {@code 0x76}.
     */
    private final String displayName;

    /**
     * Constructs a constant bound to its native id and recovered display name.
     *
     * @param index       the native event id the dispatcher selects on
     * @param displayName the recovered engine display name without its {@code "EVENT: "} prefix, or an
     *                    empty string when the name was not recovered
     */
    CallEventType(int index, String displayName) {
        this.index = index;
        this.displayName = displayName;
    }

    /**
     * Returns the native event id the WhatsApp VOIP dispatcher selects on.
     *
     * <p>The id is dense and has no gaps across the table, so it coincides with this constant's
     * {@link Enum#ordinal()}; it is exposed as a named accessor because it is the value that crosses the
     * engine boundary and is matched by {@link #ofIndex(int)}.
     *
     * @return the native event id, in the range {@code 0x00} through {@code 0xab} inclusive
     */
    public int index() {
        return index;
    }

    /**
     * Returns the recovered engine display name for this event, without its {@code "EVENT: "} prefix.
     *
     * <p>The value is the literal the engine's dispatcher logs for this id and is provided purely as a
     * diagnostic label; it is not a wire token and must not be matched against stanza content. The
     * reserved slot {@link #RESERVED_0X76} returns the empty string because no display name was recovered
     * for it.
     *
     * @return the display name, or the empty string for the reserved {@code 0x76} slot; never {@code null}
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Looks up the event type for a native event id.
     *
     * <p>The lookup is keyed on the native id, which for this dense table also equals the constant's
     * {@link Enum#ordinal()}. An id outside the range {@code 0x00} through {@code 0xab} yields an empty
     * result so the caller can drop an unrecognized event rather than fail.
     *
     * @param index the native event id
     * @return the matching event type, or an empty result when no constant carries the id
     */
    public static Optional<CallEventType> ofIndex(int index) {
        if (index < 0 || index >= BY_INDEX.length) {
            return Optional.empty();
        }
        return Optional.of(BY_INDEX[index]);
    }
}

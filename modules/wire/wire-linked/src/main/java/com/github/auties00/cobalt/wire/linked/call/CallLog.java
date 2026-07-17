package com.github.auties00.cobalt.wire.linked.call;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a VoIP call log record that is synchronized across all linked
 * devices via the WhatsApp app state sync mechanism.
 *
 * <p>Call log records keep call history consistent across all devices linked
 * to the same WhatsApp account. Each record captures the metadata of a single
 * call: its {@linkplain #callResult() outcome}, {@linkplain #duration() duration},
 * {@linkplain #startTime() timing}, {@linkplain #callType() classification},
 * and {@linkplain #participants() per-participant results}. Call log records
 * are also included in the initial history synchronization payload so that
 * newly linked devices receive the complete call history.
 *
 * <p>The record distinguishes between regular calls, scheduled calls, and
 * voice chats through the {@link Type} enum. When a call notification was
 * suppressed, the {@link #isDndMode()} flag and the {@link #silenceReason()}
 * field describe why the call was silenced.
 *
 * <p>For group calls, per-participant outcomes are captured in the
 * {@link #participants()} list. Each {@link ParticipantInfo} entry records
 * the individual outcome for a single participant, since different
 * participants in the same group call may have different results (for example,
 * one participant connected while another missed the call).
 */
@ProtobufMessage(name = "CallLogRecord")
public final class CallLog {
    /**
     * The outcome of this call, indicating how it ended or whether it is
     * still in progress.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    Result callResult;

    /**
     * Whether the device was in Do Not Disturb mode when this call was
     * received.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean isDndMode;

    /**
     * The reason why the incoming call notification was silenced.
     *
     * @see SilenceReason
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    SilenceReason silenceReason;

    /**
     * The duration of the call in seconds. This value is only meaningful
     * when the call result is {@link Result#CONNECTED}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    Long duration;

    /**
     * The instant at which the call was started. This timestamp is used
     * to sort call log records chronologically during history
     * synchronization.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant startTime;

    /**
     * Whether this was an incoming call ({@code true}) or an outgoing call
     * ({@code false}).
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    Boolean isIncoming;

    /**
     * Whether this was a video call ({@code true}) or a voice-only call
     * ({@code false}).
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    Boolean isVideo;

    /**
     * Whether this call was initiated through a shareable call link. When
     * {@code true}, the {@link #callLinkToken} identifies the specific
     * link used to join the call.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    Boolean isCallLink;

    /**
     * The token identifying the call link, if this is a call link call.
     *
     * @see #isCallLink
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String callLinkToken;

    /**
     * The identifier of the scheduled call, if this call was pre-scheduled.
     * This field is only set for calls of type {@link Type#SCHEDULED_CALL}.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String scheduledCallId;

    /**
     * The unique identifier for this call.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    String callId;

    /**
     * The {@link Jid} of the user who initiated this call.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    Jid callCreatorJid;

    /**
     * The {@link Jid} of the group, if this is a group call. This field
     * is {@code null} for one-to-one calls.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    Jid groupJid;

    /**
     * The per-participant metadata for this call, capturing each
     * participant's {@link Jid} and their individual call outcome.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.MESSAGE)
    List<ParticipantInfo> participants;

    /**
     * The classification of this call, distinguishing between regular
     * calls, scheduled calls, and voice chats.
     *
     * @see Type
     */
    @ProtobufProperty(index = 15, type = ProtobufType.ENUM)
    Type callType;


    /**
     * Constructs a new {@code CallLog} with all the specified field values.
     *
     * @param callResult     the outcome of this call
     * @param isDndMode      whether the device was in Do Not Disturb mode
     * @param silenceReason  the reason the call notification was silenced
     * @param duration       the call duration in seconds
     * @param startTime      the instant the call was started
     * @param isIncoming     whether this was an incoming call
     * @param isVideo        whether this was a video call
     * @param isCallLink     whether this was a call link call
     * @param callLinkToken  the call link token
     * @param scheduledCallId the scheduled call identifier
     * @param callId         the unique call identifier
     * @param callCreatorJid the JID of the call creator
     * @param groupJid       the group JID, or {@code null} for one-to-one calls
     * @param participants   the per-participant information
     * @param callType       the call type classification
     */
    CallLog(Result callResult, Boolean isDndMode, SilenceReason silenceReason, Long duration, Instant startTime, Boolean isIncoming, Boolean isVideo, Boolean isCallLink, String callLinkToken, String scheduledCallId, String callId, Jid callCreatorJid, Jid groupJid, List<ParticipantInfo> participants, Type callType) {
        this.callResult = callResult;
        this.isDndMode = isDndMode;
        this.silenceReason = silenceReason;
        this.duration = duration;
        this.startTime = startTime;
        this.isIncoming = isIncoming;
        this.isVideo = isVideo;
        this.isCallLink = isCallLink;
        this.callLinkToken = callLinkToken;
        this.scheduledCallId = scheduledCallId;
        this.callId = callId;
        this.callCreatorJid = callCreatorJid;
        this.groupJid = groupJid;
        this.participants = participants;
        this.callType = callType;
    }

    /**
     * Returns the outcome of this call, indicating how it ended or whether
     * it is still in progress.
     *
     * <p>For group calls, this represents the overall call result. Individual
     * per-participant outcomes are available through {@link #participants()}.
     *
     * @return an {@code Optional} describing the call result, or an empty
     *         {@code Optional} if the result has not been set
     */
    public Optional<Result> callResult() {
        return Optional.ofNullable(callResult);
    }

    /**
     * Returns whether the device was in Do Not Disturb mode when this call
     * was received.
     *
     * @return {@code true} if the device was in DND mode, {@code false}
     *         otherwise or if the value was not set
     */
    public boolean isDndMode() {
        return isDndMode != null && isDndMode;
    }

    /**
     * Returns the reason why the call notification was silenced, if any.
     *
     * <p>A non-{@link SilenceReason#NONE NONE} value indicates that the
     * incoming call notification was suppressed. Common reasons include
     * privacy settings (such as "Silence Unknown Callers"), scheduled DND
     * windows, and lightweight call notifications.
     *
     * @return an {@code Optional} describing the silence reason, or an empty
     *         {@code Optional} if the reason is not available
     * @see #isDndMode()
     */
    public Optional<SilenceReason> silenceReason() {
        return Optional.ofNullable(silenceReason);
    }

    /**
     * Returns the duration of the call in seconds.
     *
     * <p>This value is only meaningful when the call result is
     * {@link Result#CONNECTED}. For calls that were missed, rejected, or
     * cancelled, the duration is typically zero or absent.
     *
     * @return an {@code OptionalLong} containing the duration in seconds, or
     *         an empty {@code OptionalLong} if the duration is not available
     */
    public OptionalLong duration() {
        return duration == null ? OptionalLong.empty() : OptionalLong.of(duration);
    }

    /**
     * Returns the instant at which the call was started.
     *
     * <p>This timestamp is used to sort call log records chronologically
     * and is expressed in epoch seconds.
     *
     * @return an {@code Optional} describing the start time, or an empty
     *         {@code Optional} if the start time is not available
     */
    public Optional<Instant> startTime() {
        return Optional.ofNullable(startTime);
    }

    /**
     * Returns whether this was an incoming call.
     *
     * @return {@code true} if the call was incoming, {@code false} if it was
     *         outgoing or if the value was not set
     */
    public boolean isIncoming() {
        return isIncoming != null && isIncoming;
    }

    /**
     * Returns whether this was a video call.
     *
     * @return {@code true} for video calls, {@code false} for voice-only
     *         calls or if the value was not set
     */
    public boolean isVideo() {
        return isVideo != null && isVideo;
    }

    /**
     * Returns whether this call was initiated through a shareable call link.
     *
     * <p>Call links allow anyone with the link to join the call directly.
     * When this returns {@code true}, the {@link #callLinkToken()} method
     * provides the token identifying the specific link used.
     *
     * @return {@code true} if this call was a call link call, {@code false}
     *         otherwise or if the value was not set
     * @see #callLinkToken()
     */
    public boolean isCallLink() {
        return isCallLink != null && isCallLink;
    }

    /**
     * Returns the token identifying the call link, if this is a call link
     * call.
     *
     * @return an {@code Optional} describing the call link token, or an empty
     *         {@code Optional} if this is not a call link call
     * @see #isCallLink()
     */
    public Optional<String> callLinkToken() {
        return Optional.ofNullable(callLinkToken);
    }

    /**
     * Returns the identifier of the scheduled call, if this call was
     * pre-scheduled.
     *
     * <p>This field is only present for calls of type
     * {@link Type#SCHEDULED_CALL}.
     *
     * @return an {@code Optional} describing the scheduled call identifier, or
     *         an empty {@code Optional} if this is not a scheduled call
     * @see Type#SCHEDULED_CALL
     */
    public Optional<String> scheduledCallId() {
        return Optional.ofNullable(scheduledCallId);
    }

    /**
     * Returns the unique identifier for this call.
     *
     * <p>This identifier is also used as the message key ID for the
     * corresponding call log message in the chat.
     *
     * @return an {@code Optional} describing the call identifier, or an empty
     *         {@code Optional} if the identifier is not available
     */
    public Optional<String> callId() {
        return Optional.ofNullable(callId);
    }

    /**
     * Returns the {@link Jid} of the user who created (initiated) this call.
     *
     * @return an {@code Optional} describing the call creator's JID, or an
     *         empty {@code Optional} if the creator is not available
     */
    public Optional<Jid> callCreatorJid() {
        return Optional.ofNullable(callCreatorJid);
    }

    /**
     * Returns the {@link Jid} of the group, if this is a group call.
     *
     * @return an {@code Optional} describing the group JID, or an empty
     *         {@code Optional} for one-to-one calls
     */
    public Optional<Jid> groupJid() {
        return Optional.ofNullable(groupJid);
    }

    /**
     * Returns the list of participants in this call with their individual
     * call outcomes.
     *
     * <p>In group calls, each participant may have a different outcome: one
     * participant may have connected while another missed the call. For
     * one-to-one calls, this list typically contains at most one entry
     * for the remote peer.
     *
     * @return an unmodifiable list of {@link ParticipantInfo} entries, or an
     *         empty list if no participant information is available
     */
    public List<ParticipantInfo> participants() {
        return participants == null ? List.of() : Collections.unmodifiableList(participants);
    }

    /**
     * Returns the classification of this call.
     *
     * @return an {@code Optional} describing the call type, or an empty
     *         {@code Optional} if the type is not available
     * @see Type
     */
    public Optional<Type> callType() {
        return Optional.ofNullable(callType);
    }

    /**
     * Sets the outcome of this call.
     *
     * @param callResult the call result to set
     */
    public void setCallResult(Result callResult) {
        this.callResult = callResult;
    }

    /**
     * Sets whether the device was in Do Not Disturb mode when this call
     * was received.
     *
     * @param isDndMode whether DND mode was active
     */
    public void setDndMode(Boolean isDndMode) {
        this.isDndMode = isDndMode;
    }

    /**
     * Sets the reason why the call notification was silenced.
     *
     * @param silenceReason the silence reason to set
     */
    public void setSilenceReason(SilenceReason silenceReason) {
        this.silenceReason = silenceReason;
    }

    /**
     * Sets the duration of the call in seconds.
     *
     * @param duration the call duration in seconds
     */
    public void setDuration(Long duration) {
        this.duration = duration;
    }

    /**
     * Sets the instant at which the call was started.
     *
     * @param startTime the start time to set
     */
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets whether this was an incoming call.
     *
     * @param isIncoming whether the call was incoming
     */
    public void setIncoming(Boolean isIncoming) {
        this.isIncoming = isIncoming;
    }

    /**
     * Sets whether this was a video call.
     *
     * @param isVideo whether the call was a video call
     */
    public void setVideo(Boolean isVideo) {
        this.isVideo = isVideo;
    }

    /**
     * Sets whether this call was initiated through a shareable call link.
     *
     * @param isCallLink whether this was a call link call
     */
    public void setCallLink(Boolean isCallLink) {
        this.isCallLink = isCallLink;
    }

    /**
     * Sets the token identifying the call link.
     *
     * @param callLinkToken the call link token to set
     */
    public void setCallLinkToken(String callLinkToken) {
        this.callLinkToken = callLinkToken;
    }

    /**
     * Sets the identifier of the scheduled call.
     *
     * @param scheduledCallId the scheduled call identifier to set
     */
    public void setScheduledCallId(String scheduledCallId) {
        this.scheduledCallId = scheduledCallId;
    }

    /**
     * Sets the unique identifier for this call.
     *
     * @param callId the call identifier to set
     */
    public void setCallId(String callId) {
        this.callId = callId;
    }

    /**
     * Sets the {@link Jid} of the user who created this call.
     *
     * @param callCreatorJid the call creator's JID to set
     */
    public void setCallCreatorJid(Jid callCreatorJid) {
        this.callCreatorJid = callCreatorJid;
    }

    /**
     * Sets the {@link Jid} of the group for group calls.
     *
     * @param groupJid the group JID to set
     */
    public void setGroupJid(Jid groupJid) {
        this.groupJid = groupJid;
    }

    /**
     * Sets the list of participants and their individual call results.
     *
     * @param participants the participant information list to set
     */
    public void setParticipants(List<ParticipantInfo> participants) {
        this.participants = participants;
    }

    /**
     * Sets the classification of this call.
     *
     * @param callType the call type to set
     */
    public void setCallType(Type callType) {
        this.callType = callType;
    }

    /**
     * Represents the outcome of a VoIP call as recorded in the synchronized
     * call log.
     *
     * <p>These results describe how a call ended or its current state. When
     * rendering the call history, several of these results are grouped into
     * simplified display categories: {@link #CONNECTED} represents a
     * completed call, while {@link #UNAVAILABLE} and {@link #ABANDONED} are
     * both displayed as missed calls. {@link #INVALID}, {@link #UPCOMING},
     * and {@link #FAILED} are treated as failed calls.
     *
     * <p>This enum is also used at the per-participant level within
     * {@link ParticipantInfo} to track individual outcomes in group calls.
     */
    @ProtobufEnum(name = "CallLogRecord.CallResult")
    public static enum Result {
        /**
         * The call was successfully connected and a media session was
         * established between the participants. This is displayed as a
         * completed call in the call history.
         */
        CONNECTED(0),

        /**
         * The callee explicitly rejected (declined) the incoming call.
         */
        REJECTED(1),

        /**
         * The caller cancelled the call before it was answered.
         */
        CANCELLED(2),

        /**
         * The call was accepted on another linked device belonging to the
         * same WhatsApp account.
         */
        ACCEPTEDELSEWHERE(3),

        /**
         * The call was not answered. This includes scenarios where the
         * device was in Do Not Disturb mode or notifications were muted.
         */
        MISSED(4),

        /**
         * The call result is invalid or could not be determined.
         */
        INVALID(5),

        /**
         * The callee was unavailable to take the call. This is displayed
         * as a missed call in the call history.
         */
        UNAVAILABLE(6),

        /**
         * A scheduled call that has not yet started. This result is used
         * for {@link Type#SCHEDULED_CALL scheduled calls} during the
         * period before the call begins.
         */
        UPCOMING(7),

        /**
         * The call failed due to a technical error, such as a network
         * failure or media session setup error.
         */
        FAILED(8),

        /**
         * The call was abandoned by the participant without completing.
         * This is displayed as a missed call in the call history.
         */
        ABANDONED(9),

        /**
         * The call is currently in progress.
         */
        ONGOING(10);

        /**
         * The protobuf index of this call result.
         */
        final int index;

        /**
         * Constructs a new {@code Result} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        Result(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf index of this call result.
         *
         * @return the numeric index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Represents the classification of a VoIP call in the synchronized call
     * log, distinguishing between standard calls, pre-scheduled calls, and
     * persistent voice chat sessions.
     */
    @ProtobufEnum(name = "CallLogRecord.CallType")
    public static enum Type {
        /**
         * A standard one-to-one or group call initiated in real time.
         */
        REGULAR(0),

        /**
         * A pre-scheduled call with a defined start time.
         *
         * <p>Scheduled calls have an associated
         * {@linkplain CallLog#scheduledCallId() scheduled call identifier}
         * and begin in the {@link Result#UPCOMING} state until the
         * scheduled time arrives.
         */
        SCHEDULED_CALL(1),

        /**
         * A persistent voice chat session within a group.
         *
         * <p>Voice chats allow group members to join and leave freely
         * without a formal call invitation, similar to an always-available
         * audio channel.
         */
        VOICE_CHAT(2);

        /**
         * The protobuf index of this call type.
         */
        final int index;

        /**
         * Constructs a new {@code Type} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf index of this call type.
         *
         * @return the numeric index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Represents the reason why an incoming call notification was silenced.
     *
     * <p>When a call is silenced, the call log entry may display
     * supplementary text such as "Silenced unknown caller" to explain
     * why the notification was not shown.
     *
     * @see CallLog#isDndMode()
     */
    @ProtobufEnum(name = "CallLogRecord.SilenceReason")
    public static enum SilenceReason {
        /**
         * The call notification was not silenced. This is the default
         * value, indicating that the incoming call notification was
         * displayed normally.
         */
        NONE(0),

        /**
         * The call was silenced because of a scheduled Do Not Disturb
         * window or notification schedule.
         */
        SCHEDULED(1),

        /**
         * The call was silenced due to privacy settings, such as the
         * "Silence Unknown Callers" feature that suppresses calls from
         * numbers not in the user's contacts.
         */
        PRIVACY(2),

        /**
         * The call was silenced because it used a lightweight call
         * notification.
         *
         * <p>Lightweight calls display a minimal notification banner
         * instead of the full-screen ringing UI.
         */
        LIGHTWEIGHT(3);

        /**
         * The protobuf index of this silence reason.
         */
        final int index;

        /**
         * Constructs a new {@code SilenceReason} with the specified protobuf
         * index.
         *
         * @param index the protobuf enum index
         */
        SilenceReason(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf index of this silence reason.
         *
         * @return the numeric index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Represents per-participant metadata within a {@link CallLog} record,
     * capturing each participant's {@link Jid} and their individual call
     * outcome.
     *
     * <p>In group calls, each participant may have a different outcome. For
     * example, one participant may have connected ({@link Result#CONNECTED})
     * while another was rejected ({@link Result#REJECTED}) or missed the
     * call ({@link Result#MISSED}). The WhatsApp client uses these
     * per-participant results to display individual call statuses in the
     * group call detail view.
     */
    @ProtobufMessage(name = "CallLogRecord.ParticipantInfo")
    public static final class ParticipantInfo {
        /**
         * The {@link Jid} of this participant.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid userJid;

        /**
         * The individual call outcome for this participant. This uses the
         * same {@link Result} enum as the top-level
         * {@link CallLog#callResult() call result}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
        Result callResult;


        /**
         * Constructs a new {@code ParticipantInfo} with the specified JID
         * and call result.
         *
         * @param userJid    the participant's JID
         * @param callResult the participant's individual call outcome
         */
        ParticipantInfo(Jid userJid, Result callResult) {
            this.userJid = userJid;
            this.callResult = callResult;
        }

        /**
         * Returns the {@link Jid} of this participant.
         *
         * @return an {@code Optional} describing the participant's JID, or
         *         an empty {@code Optional} if the JID is not available
         */
        public Optional<Jid> userJid() {
            return Optional.ofNullable(userJid);
        }

        /**
         * Returns the individual call outcome for this participant.
         *
         * @return an {@code Optional} describing the participant's call
         *         result, or an empty {@code Optional} if the result is
         *         not available
         */
        public Optional<Result> callResult() {
            return Optional.ofNullable(callResult);
        }

        /**
         * Sets the {@link Jid} of this participant.
         *
         * @param userJid the participant's JID to set
         */
        public void setUserJid(Jid userJid) {
            this.userJid = userJid;
    }

        /**
         * Sets the individual call outcome for this participant.
         *
         * @param callResult the participant's call result to set
         */
        public void setCallResult(Result callResult) {
            this.callResult = callResult;
    }
    }
}

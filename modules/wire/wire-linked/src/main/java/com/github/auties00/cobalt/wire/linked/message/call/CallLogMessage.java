package com.github.auties00.cobalt.wire.linked.message.call;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.Message;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A message that records the history of a completed call inside a chat.
 *
 * <p>WhatsApp inserts call log entries into conversations to keep a visible trail of calls: every
 * entry captures whether the call included a video stream, the final outcome (connected, missed,
 * rejected and so on), the duration in seconds, the kind of call (regular, scheduled or voice
 * chat) and, for group calls, the list of participants together with their individual outcomes.
 *
 * <p>Call log messages are informational only: they are rendered as a system entry in the chat
 * view and do not carry any body text of their own.
 */
@ProtobufMessage(name = "Message.CallLogMessage")
public final class CallLogMessage implements Message {
    /**
     * Whether the logged call included a video stream.
     *
     * <p>A {@code true} value denotes a video call, {@code false} an audio only call. The field
     * may be {@code null} when the server omits it, in which case the call is treated as audio only.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isVideo;

    /**
     * Final outcome of the call as seen by the local participant.
     *
     * <p>See {@link CallOutcome} for the possible values, which cover both successful completions
     * and various failure modes such as missed, rejected or silenced calls.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    CallOutcome callOutcome;

    /**
     * Total duration of the call in seconds.
     *
     * <p>Only meaningful when the call was actually connected. For missed, rejected or failed calls
     * the value is usually zero or absent.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    Long durationSecs;

    /**
     * Kind of call being logged.
     *
     * <p>Distinguishes between regular one-off calls, calls that were started from a scheduled
     * call event, and voice chats held inside group conversations.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    CallType callType;

    /**
     * List of participants that took part in the call, together with their individual outcomes.
     *
     * <p>Populated for group calls and voice chats. One-to-one calls typically leave this list empty.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    List<CallParticipant> participants;

    /**
     * Constructs a new call log message with the given flags, outcome, duration, call type and
     * participant list.
     *
     * <p>This constructor is package-private: use the generated {@code CallLogMessageBuilder} to
     * create instances.
     *
     * @param isVideo      whether the call included a video stream, may be {@code null}
     * @param callOutcome  the final outcome of the call, may be {@code null}
     * @param durationSecs the duration of the call in seconds, may be {@code null}
     * @param callType     the kind of call being logged, may be {@code null}
     * @param participants the participants that joined the call, may be {@code null}
     */
    CallLogMessage(Boolean isVideo, CallOutcome callOutcome, Long durationSecs, CallType callType, List<CallParticipant> participants) {
        this.isVideo = isVideo;
        this.callOutcome = callOutcome;
        this.durationSecs = durationSecs;
        this.callType = callType;
        this.participants = participants;
    }

    /**
     * Returns whether the logged call included a video stream.
     *
     * <p>A missing or {@code null} underlying value is treated as {@code false}, reflecting the
     * WhatsApp convention that calls without a video flag are audio only.
     *
     * @return {@code true} if the call carried a video stream, {@code false} otherwise
     */
    public boolean isVideo() {
        return isVideo != null && isVideo;
    }

    /**
     * Returns the final outcome of the call as observed by the local participant.
     *
     * @return an {@link Optional} containing the {@link CallOutcome}, or empty if not set
     */
    public Optional<CallOutcome> callOutcome() {
        return Optional.ofNullable(callOutcome);
    }

    /**
     * Returns the total duration of the call in seconds.
     *
     * @return an {@link OptionalLong} with the duration in seconds, or empty if not set
     */
    public OptionalLong durationSecs() {
        return durationSecs == null ? OptionalLong.empty() : OptionalLong.of(durationSecs);
    }

    /**
     * Returns the kind of call being logged.
     *
     * @return an {@link Optional} containing the {@link CallType}, or empty if not set
     */
    public Optional<CallType> callType() {
        return Optional.ofNullable(callType);
    }

    /**
     * Returns the participants that took part in the call.
     *
     * <p>The returned list is an unmodifiable view; modifications must be made through
     * {@link #setParticipants(List)}.
     *
     * @return an unmodifiable {@link List} of {@link CallParticipant}, never {@code null}
     */
    public List<CallParticipant> participants() {
        return participants == null ? List.of() : Collections.unmodifiableList(participants);
    }

    /**
     * Sets whether the logged call included a video stream.
     *
     * @param isVideo {@code true} for a video call, {@code false} for audio only, or {@code null} to clear the field
     */
    public void setVideo(Boolean isVideo) {
        this.isVideo = isVideo;
    }

    /**
     * Sets the final outcome of the call.
     *
     * @param callOutcome the {@link CallOutcome}, or {@code null} to clear the field
     */
    public void setCallOutcome(CallOutcome callOutcome) {
        this.callOutcome = callOutcome;
    }

    /**
     * Sets the total duration of the call in seconds.
     *
     * @param durationSecs the duration in seconds, or {@code null} to clear the field
     */
    public void setDurationSecs(Long durationSecs) {
        this.durationSecs = durationSecs;
    }

    /**
     * Sets the kind of call being logged.
     *
     * @param callType the {@link CallType}, or {@code null} to clear the field
     */
    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    /**
     * Sets the list of participants that took part in the call.
     *
     * @param participants the participant list, or {@code null} to clear the field
     */
    public void setParticipants(List<CallParticipant> participants) {
        this.participants = participants;
    }

    /**
     * Describes the outcome of a call as recorded in the chat history.
     *
     * <p>The outcome captures both the result from the perspective of the local participant and,
     * when attached to a {@link CallParticipant}, the per-participant result in a group call.
     */
    @ProtobufEnum(name = "Message.CallLogMessage.CallOutcome")
    public static enum CallOutcome {
        /**
         * The call was successfully connected and completed.
         */
        CONNECTED(0),
        /**
         * The call rang on the device but was not answered in time.
         */
        MISSED(1),
        /**
         * The call failed because of a network or signalling error.
         */
        FAILED(2),
        /**
         * The call was explicitly rejected by the callee.
         */
        REJECTED(3),
        /**
         * The call was answered from another linked device belonging to the same account.
         */
        ACCEPTED_ELSEWHERE(4),
        /**
         * The call is still in progress at the time the log entry was produced.
         */
        ONGOING(5),
        /**
         * The call was silenced by the recipient's do-not-disturb mode.
         */
        SILENCED_BY_DND(6),
        /**
         * The call was silenced because the caller is not in the recipient's contacts and
         * the silence-unknown-callers preference is enabled.
         */
        SILENCED_UNKNOWN_CALLER(7);

        /**
         * Creates a call outcome constant with the given protobuf index.
         *
         * @param index the wire value assigned to this constant by the protobuf schema
         */
        CallOutcome(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Wire value assigned to this constant by the protobuf schema.
         */
        final int index;

        /**
         * Returns the wire value of this constant.
         *
         * @return the protobuf index of this enum constant
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Distinguishes between the different kinds of calls that can appear in a chat's call log.
     *
     * <p>A {@link #REGULAR} call is an ad hoc one-to-one or group call, a {@link #SCHEDULED_CALL}
     * is a call that was started from a previously announced scheduled event, and a
     * {@link #VOICE_CHAT} is a drop-in voice room hosted inside a group.
     */
    @ProtobufEnum(name = "Message.CallLogMessage.CallType")
    public static enum CallType {
        /**
         * A regular ad hoc call started directly from a chat.
         */
        REGULAR(0),
        /**
         * A call started from a previously announced scheduled call event.
         */
        SCHEDULED_CALL(1),
        /**
         * A voice chat hosted inside a group conversation.
         */
        VOICE_CHAT(2);

        /**
         * Creates a call type constant with the given protobuf index.
         *
         * @param index the wire value assigned to this constant by the protobuf schema
         */
        CallType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Wire value assigned to this constant by the protobuf schema.
         */
        final int index;

        /**
         * Returns the wire value of this constant.
         *
         * @return the protobuf index of this enum constant
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Entry describing a single participant of a multi-party call log.
     *
     * <p>Each entry binds a participant {@link Jid} to the {@link CallOutcome} observed for that
     * participant. This is how group call and voice chat logs report, for example, that one
     * participant connected successfully while another missed the call.
     */
    @ProtobufMessage(name = "Message.CallLogMessage.CallParticipant")
    public static final class CallParticipant {
        /**
         * {@link Jid} of the participant this entry refers to.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid jid;

        /**
         * Per-participant outcome of the call.
         *
         * <p>Captures how the call terminated for this specific participant, independently of the
         * outcome observed by the other parties.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
        CallOutcome callOutcome;

        /**
         * Constructs a new call participant entry with the given {@link Jid} and outcome.
         *
         * <p>This constructor is package-private: use the generated
         * {@code CallLogMessageCallParticipantBuilder} to create instances.
         *
         * @param jid         the {@link Jid} of the participant, may be {@code null}
         * @param callOutcome the per-participant outcome, may be {@code null}
         */
        CallParticipant(Jid jid, CallOutcome callOutcome) {
            this.jid = jid;
            this.callOutcome = callOutcome;
        }

        /**
         * Returns the {@link Jid} of the participant this entry describes.
         *
         * @return an {@link Optional} containing the participant's {@link Jid}, or empty if not set
         */
        public Optional<Jid> jid() {
            return Optional.ofNullable(jid);
        }

        /**
         * Returns the outcome observed for this specific participant.
         *
         * @return an {@link Optional} containing the {@link CallOutcome}, or empty if not set
         */
        public Optional<CallOutcome> callOutcome() {
            return Optional.ofNullable(callOutcome);
        }

        /**
         * Sets the {@link Jid} of the participant this entry describes.
         *
         * @param jid the participant's {@link Jid}, or {@code null} to clear the field
         */
        public void setJid(Jid jid) {
            this.jid = jid;
    }

        /**
         * Sets the outcome observed for this specific participant.
         *
         * @param callOutcome the {@link CallOutcome}, or {@code null} to clear the field
         */
        public void setCallOutcome(CallOutcome callOutcome) {
            this.callOutcome = callOutcome;
    }
    }
}

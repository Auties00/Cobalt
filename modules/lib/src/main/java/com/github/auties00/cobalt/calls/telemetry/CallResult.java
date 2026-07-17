package com.github.auties00.cobalt.calls.telemetry;

import com.github.auties00.cobalt.wire.linked.call.CallEndReason;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Enumerates the result codes the voip engine records for a call outcome.
 *
 * <p>The engine stores a call result separately from the call state: the state machine tracks where a
 * call is in its lifecycle, while the result records why it ended (or that it succeeded). Each constant
 * binds the numeric result code the engine writes into the call context ({@link #wireCode()}) and
 * projects to the user facing {@link CallEndReason} via {@link #toEndReason()}.
 *
 * <p>The projection maps each result to the wire {@code reason} literal it corresponds to and delegates
 * to {@link CallEndReason#fromWireValue(String)}, so the mapping rides the model's own stable,
 * unknown collapsing wire contract rather than depending on the spelling of any model constant. A result
 * that does not denote a termination reason, such as {@link #ACCEPTED}, projects to
 * {@link CallEndReason#UNKNOWN}.
 *
 * @implNote This implementation numbers the constants to match the engine's internal result string table,
 * which mirrors the WAM {@code CALL_RESULT_TYPE} enum: {@code 1 = Connected}, {@code 2 = RejectedByUser},
 * {@code 3 = RejectedByServer}, {@code 6 = SetupError}, {@code 7 = ServerNack},
 * {@code 8 = CallOfferAckNotReceived}, {@code 0xf = RejectedTOS}, {@code 0x10 = RejectedE2E},
 * {@code 0x11 = RejectedUnavailable}, {@code 0x13 = PeerSetupError}, {@code 0x16 = AcceptedElsewhere},
 * {@code 0x17 = RejectedElsewhere}, {@code 0x19 = CallIsFull}, {@code 0x1c = CallDoesNotExistForRejoin}.
 */
public enum CallResult {
    /**
     * Represents a call that connected successfully; not a termination reason.
     *
     * <p>A normal hangup leaves the result at this value; the hangup is recorded on the separate
     * terminate reason field, not the call result. {@link #toEndReason()} maps it to
     * {@link CallEndReason#UNKNOWN}.
     */
    ACCEPTED(1),

    /**
     * Represents a call that failed to set up locally, internal result string {@code SetupError}.
     *
     * <p>The engine records this on a local setup failure, including a relay bind failure on the
     * terminate path. {@link #toEndReason()} maps it to {@link CallEndReason#SETUP_FAILED}.
     */
    SETUP_ERROR(6),

    /**
     * Represents an outbound offer the server rejected with a NACK, internal result string
     * {@code ServerNack}.
     *
     * <p>The engine records this when an offer ack carries an error. Unlike the accept ack, the
     * offer ack error carries no subcode map: an offer NACK collapses to this single code.
     * {@link #toEndReason()} maps it to {@link CallEndReason#SETUP_FAILED}.
     */
    SERVER_NACK(7),

    /**
     * Represents an outbound call whose offer was never acknowledged by the server, internal result
     * string {@code CallOfferAckNotReceived}.
     *
     * <p>This is the initial result for a freshly placed call, cleared once the offer is acknowledged.
     * {@link #toEndReason()} maps it to {@link CallEndReason#SETUP_FAILED}.
     */
    CALL_OFFER_ACK_NOT_RECEIVED(8),

    /**
     * Represents a call that failed to set up on the peer side, internal result string
     * {@code PeerSetupError}.
     *
     * <p>The engine records this on a peer side setup failure, including the {@code setup_failed}
     * terminate reason. {@link #toEndReason()} maps it to {@link CallEndReason#SETUP_FAILED}.
     */
    PEER_SETUP_ERROR(0x13),

    /**
     * Represents a call accepted on another device of the same account, internal result string
     * {@code AcceptedElsewhere}.
     *
     * <p>{@link #toEndReason()} maps it to the {@code accepted_elsewhere} wire reason.
     */
    ACCEPTED_ELSEWHERE(0x16),

    /**
     * Represents a call rejected on another device of the same account, internal result string
     * {@code RejectedElsewhere}.
     *
     * <p>{@link #toEndReason()} maps it to the {@code rejected_elsewhere} wire reason.
     */
    REJECTED_ELSEWHERE(0x17),

    /**
     * Represents an accept the server rejected because the call no longer exists, internal result string
     * {@code CallDoesNotExistForRejoin}.
     *
     * <p>The engine records this result on an accept NACK carrying server error {@code 404}, and on the
     * terminate path when the local user accepted a call that had already been terminated so there is no
     * call left to rejoin. There is no distinct terminate reason wire literal, so {@link #toEndReason()}
     * collapses to {@link CallEndReason#SETUP_FAILED}, the accept side setup failure.
     */
    CALL_DOES_NOT_EXIST_FOR_REJOIN(0x1c),

    /**
     * Represents an accept the server rejected because the call is full, internal result string
     * {@code CallIsFull}.
     *
     * <p>The engine records this result on an accept NACK carrying server error {@code 434}. There is no
     * distinct terminate reason wire literal, so {@link #toEndReason()} collapses to
     * {@link CallEndReason#SETUP_FAILED}, the accept side setup failure.
     */
    CALL_IS_FULL(0x19),

    /**
     * Represents a call rejected by the user, internal result string {@code RejectedByUser}.
     *
     * <p>The engine sets this result when an inbound {@code <reject>} stanza carries an empty reject
     * reason. There is no distinct terminate reason wire literal, so {@link #toEndReason()} collapses to
     * the generic {@code rejected} reason.
     */
    REJECTED_BY_USER(2),

    /**
     * Represents a call rejected by the server, internal result string {@code RejectedByServer}.
     *
     * <p>The engine sets this result for the inbound {@code <reject>} reason {@code uncallable}. There is
     * no distinct terminate reason wire literal, so {@link #toEndReason()} collapses to the generic
     * {@code rejected} reason.
     */
    REJECTED_BY_SERVER(3),

    /**
     * Represents a call rejected because the peer was unavailable, internal result string
     * {@code RejectedUnavailable}.
     *
     * <p>The engine sets this result for the inbound {@code <reject>} reason {@code unavailable}. This
     * value stays distinct on the engine's result string and call log axis; on the terminate axis there
     * is no matching {@link CallEndReason} member, so {@link #toEndReason()} collapses to the generic
     * {@code rejected} reason. The {@code unavailable} reason is a {@code <reject>} subtype, not a
     * terminate reason literal, so it has no dedicated {@link CallEndReason} projection.
     */
    REJECTED_UNAVAILABLE(0x11),

    /**
     * Represents a call rejected because the terms of service were not accepted, internal result string
     * {@code RejectedTOS}.
     *
     * <p>The engine sets this result for the inbound {@code <reject>} reason {@code tos}. There is no
     * distinct terminate reason wire literal, so {@link #toEndReason()} collapses to the generic
     * {@code rejected} reason.
     */
    REJECTED_TOS(0xf),

    /**
     * Represents a call rejected because of an end to end encryption failure, internal result string
     * {@code RejectedE2E}.
     *
     * <p>The engine sets this result for the inbound {@code <reject>} reasons {@code enc} and
     * {@code no-raw-e2e}. There is no distinct terminate reason wire literal, so {@link #toEndReason()}
     * collapses to the generic {@code rejected} reason.
     */
    REJECTED_E2E(0x10);

    /**
     * Holds the numeric result code the engine stores in the call context.
     */
    private final int wireCode;

    /**
     * Constructs a result bound to its numeric code.
     *
     * @param wireCode the numeric result code the engine stores in the call context
     */
    CallResult(int wireCode) {
        this.wireCode = wireCode;
    }

    /**
     * Returns the numeric result code the engine stores in the call context.
     *
     * <p>Every constant carries a code, so the result is always present; the {@link OptionalInt} return
     * type is retained for API stability.
     *
     * @return the numeric result code, always present
     */
    public OptionalInt wireCode() {
        return OptionalInt.of(wireCode);
    }

    /**
     * Projects this result onto the user facing {@link CallEndReason}.
     *
     * <p>Each result is mapped to the wire {@code reason} literal it corresponds to, then resolved
     * through {@link CallEndReason#fromWireValue(String)}. {@link #ACCEPTED} carries no termination
     * reason and resolves to {@link CallEndReason#UNKNOWN}. The setup failure results
     * ({@link #CALL_OFFER_ACK_NOT_RECEIVED}, {@link #SERVER_NACK}, {@link #SETUP_ERROR},
     * {@link #PEER_SETUP_ERROR}, {@link #CALL_DOES_NOT_EXIST_FOR_REJOIN}, {@link #CALL_IS_FULL}) all map to
     * the {@code setup_failed} reason. Every reject family variant collapses to the generic
     * {@code rejected} reason: their distinctions ({@code unavailable}, {@code tos}, {@code enc},
     * {@code uncallable}) are {@code <reject>} stanza reason subtypes that the engine's terminate reason
     * mapper does not carry, so none of them has its own {@link CallEndReason} member.
     *
     * @return the corresponding public end reason, never {@code null}
     */
    public CallEndReason toEndReason() {
        var wireReason = switch (this) {
            case ACCEPTED -> null;
            case CALL_OFFER_ACK_NOT_RECEIVED, SERVER_NACK, SETUP_ERROR, PEER_SETUP_ERROR,
                 CALL_DOES_NOT_EXIST_FOR_REJOIN, CALL_IS_FULL -> "setup_failed";
            case ACCEPTED_ELSEWHERE -> "accepted_elsewhere";
            case REJECTED_ELSEWHERE -> "rejected_elsewhere";
            case REJECTED_BY_USER, REJECTED_BY_SERVER, REJECTED_UNAVAILABLE, REJECTED_TOS, REJECTED_E2E ->
                    "rejected";
        };
        return CallEndReason.fromWireValue(wireReason);
    }

    /**
     * Maps an accept ack NACK error code to the call result the engine records for it.
     *
     * <p>The server returns an {@code <ack class="call" type="accept">} carrying an {@code error} code when
     * it rejects an accept. The two known codes map onto a result: {@code 404} to
     * {@link #CALL_DOES_NOT_EXIST_FOR_REJOIN} (the call no longer exists) and {@code 434} to
     * {@link #CALL_IS_FULL}. Any other error code records no specific result; the call still ends as an
     * accept side setup failure, so the caller falls back to the generic outcome.
     *
     * @param error the {@code error} code carried on the accept NACK
     * @return the matching result, or {@link Optional#empty()} when {@code error} is not a mapped code
     */
    public static Optional<CallResult> fromAcceptAckError(int error) {
        return switch (error) {
            case 404 -> Optional.of(CALL_DOES_NOT_EXIST_FOR_REJOIN);
            case 434 -> Optional.of(CALL_IS_FULL);
            default -> Optional.empty();
        };
    }
}

package com.github.auties00.cobalt.wire.linked.business.support;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Outcome of submitting feedback on a WhatsApp support-assistant message.
 *
 * <p>Each reply from the WhatsApp support assistant is rated by the user with a
 * thumbs-up or thumbs-down control; on a negative rating one or more
 * negative-feedback reasons may also be reported. The server replies with the
 * submission verdict and, on a failed submission, an error code and a
 * human-readable error message.
 *
 * <p>The error code is exposed as a raw string because the WhatsApp client
 * passes the server value through unchanged and defaults it to the literal
 * {@code "500"} when missing.
 */
@ProtobufMessage(name = "SupportMessageFeedbackSubmission")
public final class SupportMessageFeedbackSubmission {
    /**
     * Whether the feedback was submitted successfully. {@code false} when the
     * server omitted the verdict.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean success;

    /**
     * Error code returned for a failed submission. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String errorCode;

    /**
     * Human-readable error message returned for a failed submission.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String errorMessage;

    /**
     * Constructs a new {@code SupportMessageFeedbackSubmission}. The reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param success      whether the submission succeeded
     * @param errorCode    the error code, or {@code null}
     * @param errorMessage the human-readable error message, or {@code null}
     */
    SupportMessageFeedbackSubmission(boolean success, String errorCode, String errorMessage) {
        this.success = success;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns whether the feedback was submitted successfully.
     *
     * @return {@code true} when the server reported success, {@code false}
     *         otherwise
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns the error code reported for a failed submission.
     *
     * @return the error code, or empty when the server omitted it
     */
    public Optional<String> errorCode() {
        return Optional.ofNullable(errorCode);
    }

    /**
     * Returns the human-readable error message reported for a failed
     * submission.
     *
     * @return the error message, or empty when the server omitted it
     */
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}

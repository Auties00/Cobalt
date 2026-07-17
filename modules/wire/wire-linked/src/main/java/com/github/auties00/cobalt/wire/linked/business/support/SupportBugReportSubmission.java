package com.github.auties00.cobalt.wire.linked.business.support;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Outcome of a WhatsApp support bug-report submission.
 *
 * <p>WhatsApp's in-app "report a problem" flow forwards the user's bug-report
 * payload to the support backend and reports the outcome back. On success the
 * server assigns a bug-report identifier and a task identifier; on failure it
 * returns a numeric error code and a human-readable error message.
 *
 * <p>This model is that outcome.
 */
@ProtobufMessage(name = "SupportBugReportSubmission")
public final class SupportBugReportSubmission {
    /**
     * Whether the bug report was submitted successfully. {@code false} when the
     * server omitted the verdict.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean success;

    /**
     * Numeric error code returned for a failed submission. {@code null} when
     * the server omitted it (notably on a successful submission).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    final Long errorCode;

    /**
     * Human-readable error message returned for a failed submission.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String errorMessage;

    /**
     * Server-assigned bug-report identifier returned for a successful
     * submission. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String bugReportId;

    /**
     * Server-assigned support-task identifier returned for a successful
     * submission. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String taskId;

    /**
     * Constructs a new {@code SupportBugReportSubmission}. The reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param success      whether the submission succeeded
     * @param errorCode    the numeric error code, or {@code null}
     * @param errorMessage the human-readable error message, or {@code null}
     * @param bugReportId  the assigned bug-report identifier, or {@code null}
     * @param taskId       the assigned support-task identifier, or {@code null}
     */
    SupportBugReportSubmission(boolean success,
                               Long errorCode,
                               String errorMessage,
                               String bugReportId,
                               String taskId) {
        this.success = success;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.bugReportId = bugReportId;
        this.taskId = taskId;
    }

    /**
     * Returns whether the bug report was submitted successfully.
     *
     * @return {@code true} when the server reported success, {@code false}
     *         otherwise
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns the numeric error code reported for a failed submission.
     *
     * @return the error code, or empty when the server omitted it
     */
    public OptionalLong errorCode() {
        return errorCode == null ? OptionalLong.empty() : OptionalLong.of(errorCode);
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

    /**
     * Returns the bug-report identifier assigned to a successful submission.
     *
     * @return the bug-report identifier, or empty when the server omitted it
     */
    public Optional<String> bugReportId() {
        return Optional.ofNullable(bugReportId);
    }

    /**
     * Returns the support-task identifier assigned to a successful submission.
     *
     * @return the task identifier, or empty when the server omitted it
     */
    public Optional<String> taskId() {
        return Optional.ofNullable(taskId);
    }
}

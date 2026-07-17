package com.github.auties00.cobalt.wire.linked.chat.group;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Outcome of an appeal filed against a WhatsApp group suspension.
 *
 * <p>When WhatsApp suspends a group for policy reasons, a group admin can
 * file a written appeal asking the server to lift the suspension. The
 * server replies with a verdict telling the client whether the appeal was
 * accepted (either freshly recorded or already on file from an earlier
 * submission) or rejected, and on success records the time the appeal
 * was filed.
 *
 * <p>This model is that verdict. The verdict code is exposed as a raw
 * {@link String} because only the success-equivalent verdicts are
 * confirmable from the WhatsApp client; the full failure-verdict set is
 * not enumerated there. Callers may use {@link #accepted()} to collapse
 * the verdict into a single boolean: it is {@code true} when the verdict
 * is one of the success-equivalent values, {@code false} otherwise.
 */
@ProtobufMessage(name = "GroupSuspensionAppeal")
public final class GroupSuspensionAppeal {
    /**
     * Verdict identifier for "newly recorded appeal accepted".
     */
    private static final String VERDICT_SUCCESS = "SUCCESS";

    /**
     * Verdict identifier for "a previous appeal is already on file".
     */
    private static final String VERDICT_APPEAL_ALREADY_EXISTS = "APPEAL_ALREADY_EXISTS";

    /**
     * Server-reported verdict code, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String verdict;

    /**
     * Human-readable failure reason returned when the appeal was
     * rejected, or {@code null} when the appeal was accepted or the
     * server attached no reason.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String errorMessage;

    /**
     * Instant at which the appeal was filed, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final Long filedAtEpochSecond;

    /**
     * Constructs a new {@code GroupSuspensionAppeal}. Any reference
     * argument may be {@code null} when the server omitted the
     * corresponding field.
     *
     * @param verdict            the server-reported verdict code, or
     *                           {@code null}
     * @param errorMessage       the failure reason, or {@code null}
     * @param filedAtEpochSecond the instant the appeal was filed, as an
     *                           epoch second, or {@code null}
     */
    GroupSuspensionAppeal(String verdict, String errorMessage, Long filedAtEpochSecond) {
        this.verdict = verdict;
        this.errorMessage = errorMessage;
        this.filedAtEpochSecond = filedAtEpochSecond;
    }

    /**
     * Returns the server-reported verdict code.
     *
     * @return an {@code Optional} carrying the verdict code, or empty
     *         when the server omitted it
     */
    public Optional<String> verdict() {
        return Optional.ofNullable(verdict);
    }

    /**
     * Returns whether the server accepted the appeal.
     *
     * <p>This is a convenience that collapses {@link #verdict()} into a
     * single boolean: it is {@code true} when the verdict is one of the
     * success-equivalent values (a newly recorded appeal or a previous
     * appeal already on file), {@code false} otherwise.
     *
     * @return {@code true} when the server accepted the appeal
     */
    public boolean accepted() {
        return VERDICT_SUCCESS.equals(verdict) || VERDICT_APPEAL_ALREADY_EXISTS.equals(verdict);
    }

    /**
     * Returns the human-readable failure reason.
     *
     * @return an {@code Optional} carrying the failure reason, or empty
     *         when the appeal was accepted or the server attached no
     *         reason
     */
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    /**
     * Returns the instant at which the appeal was filed.
     *
     * @return an {@code Optional} carrying the filing instant, or empty
     *         when the server omitted it
     */
    public Optional<Instant> filedAt() {
        return Optional.ofNullable(filedAtEpochSecond).map(Instant::ofEpochSecond);
    }
}

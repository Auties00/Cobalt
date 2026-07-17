package com.github.auties00.cobalt.wire.linked.business.support;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Outcome of submitting a free-form WhatsApp support contact form.
 *
 * <p>WhatsApp lets a user file a support request through the in-app
 * contact form: a short free-text description of the issue, an opaque
 * diagnostic bundle collected by the client, and the originating support
 * flow are sent to the server. On success the server assigns a ticket
 * identifier and exposes the WhatsApp support phone number the user can
 * message about the ticket. On failure the server reports a numeric error
 * code and a human-readable message.
 *
 * <p>This model is that outcome: the success marker, the assigned ticket
 * id and support contact when the submission succeeded, and the error
 * code and message when it failed.
 */
@ProtobufMessage(name = "SupportContactFormSubmission")
public final class SupportContactFormSubmission {
    /**
     * Whether the submission was accepted. {@code false} both when the
     * server explicitly reported failure and when it omitted the success
     * marker entirely.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean success;

    /**
     * Numeric error code returned by the server when the submission
     * failed, or {@code null} when the server attached none.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final Integer errorCode;

    /**
     * Human-readable error message returned by the server when the
     * submission failed, or {@code null} when the server attached none.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String errorMessage;

    /**
     * Support ticket identifier the server assigned on success, or
     * {@code null} when the submission failed or the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String ticketId;

    /**
     * WhatsApp address of the support phone number the user can message
     * about the assigned ticket, or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final Jid supportPhoneNumber;

    /**
     * Constructs a new {@code SupportContactFormSubmission}. Any
     * reference argument may be {@code null} when the server omitted the
     * corresponding field.
     *
     * @param success            whether the submission was accepted
     * @param errorCode          the numeric error code, or {@code null}
     * @param errorMessage       the human-readable error message, or
     *                           {@code null}
     * @param ticketId           the assigned ticket id, or {@code null}
     * @param supportPhoneNumber the support contact {@link Jid}, or
     *                           {@code null}
     */
    SupportContactFormSubmission(boolean success, Integer errorCode, String errorMessage, String ticketId,
                                 Jid supportPhoneNumber) {
        this.success = success;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.ticketId = ticketId;
        this.supportPhoneNumber = supportPhoneNumber;
    }

    /**
     * Returns whether the submission was accepted.
     *
     * @return {@code true} when the server reported the submission
     *         accepted
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns the numeric error code returned on failure.
     *
     * @return an {@code OptionalInt} carrying the error code, or empty
     *         when the submission succeeded or the server attached none
     */
    public OptionalInt errorCode() {
        return errorCode != null ? OptionalInt.of(errorCode) : OptionalInt.empty();
    }

    /**
     * Returns the human-readable error message returned on failure.
     *
     * @return an {@code Optional} carrying the error message, or empty
     *         when the submission succeeded or the server attached none
     */
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    /**
     * Returns the support ticket identifier assigned on success.
     *
     * @return an {@code Optional} carrying the ticket id, or empty when
     *         the submission failed or the server omitted it
     */
    public Optional<String> ticketId() {
        return Optional.ofNullable(ticketId);
    }

    /**
     * Returns the WhatsApp address of the support phone number the user
     * can message about the assigned ticket.
     *
     * @return an {@code Optional} carrying the support contact
     *         {@link Jid}, or empty when the server omitted it
     */
    public Optional<Jid> supportPhoneNumber() {
        return Optional.ofNullable(supportPhoneNumber);
    }
}

package com.github.auties00.cobalt.exception.linked.mobile;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

import java.util.Optional;

/**
 * Thrown when registering a phone number against the WhatsApp mobile
 * registration API fails.
 *
 * <p>Registration is the flow that asks WhatsApp to issue a verification
 * code (by SMS or voice call), submits the code the user typed, and
 * collects the credentials the mobile client uses to authenticate later
 * sessions. This exception covers every rejection along that path:
 * an invalid number, a rate limit, an anti-spam block, a wrong code, or a
 * banned account.
 *
 * @apiNote
 * Raised whenever registration cannot complete; when the server returned
 * a body, {@link #erroneousResponse()} exposes it so the caller can
 * decode the specific reason and any retry hint (typically a
 * {@code retry_after} field for rate-limit driven rejections).
 * {@link #toErrorResult()} reports {@link WhatsAppLinkedClientErrorResult#DISCONNECT}
 * because authentication never completed and no session can exist yet.
 */
public final class WhatsAppRegistrationException extends WhatsAppMobileException {

    /**
     * The raw response returned by the registration API, or {@code null}
     * when the failure happened before any server reply.
     */
    private final String erroneousResponse;

    /**
     * Constructs a new registration exception with a message and API response.
     *
     * @param message           the detail message describing the registration failure
     * @param erroneousResponse the raw response body returned by the registration API
     */
    public WhatsAppRegistrationException(String message, String erroneousResponse) {
        super(message);
        this.erroneousResponse = erroneousResponse;
    }

    /**
     * Constructs a new registration exception with a descriptive message.
     *
     * @param message the detail message describing the registration failure
     */
    public WhatsAppRegistrationException(String message) {
        super(message);
        this.erroneousResponse = null;
    }

    /**
     * Constructs a new registration exception that wraps an underlying cause.
     *
     * @param cause the underlying exception that caused the registration to fail
     */
    public WhatsAppRegistrationException(Throwable cause) {
        super(cause);
        this.erroneousResponse = null;
    }

    /**
     * Returns the raw response body returned by the registration API, if any.
     *
     * @apiNote
     * The response is the JSON the WhatsApp registration servers
     * produced. It typically carries a status string, a reason code, and
     * a {@code retry_after} hint when the rejection is rate-limit
     * driven.
     *
     * @return the raw response when one is available, otherwise empty
     */
    public Optional<String> erroneousResponse() {
        return Optional.ofNullable(erroneousResponse);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCONNECT}: registration must
     * complete before any session can exist, so there is no live session to
     * reconnect.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCONNECT;
    }
}

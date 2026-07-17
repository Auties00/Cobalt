package com.github.auties00.cobalt.wire.cloud;

import java.util.Objects;
import java.util.Optional;

/**
 * A phone-number security event, decoded from a {@code security} webhook change.
 *
 * <p>The platform reports security-relevant actions on a phone number, such as a two-step
 * verification PIN change or a number being disabled, naming the affected phone number, the event,
 * and the requester that initiated it.
 */
public final class CloudSecurityUpdate {
    /**
     * The display phone number the event concerns.
     */
    private final String displayPhoneNumber;

    /**
     * The security event, for example {@code pin_changed}.
     */
    private final String event;

    /**
     * The identifier of the requester that initiated the event, or {@code null} when not reported.
     */
    private final String requester;

    /**
     * Constructs a new security update.
     *
     * @param displayPhoneNumber the display phone number the event concerns
     * @param event              the security event
     * @param requester          the requester that initiated the event, or {@code null}
     * @throws NullPointerException if {@code displayPhoneNumber} or {@code event} is {@code null}
     */
    public CloudSecurityUpdate(String displayPhoneNumber, String event, String requester) {
        this.displayPhoneNumber = Objects.requireNonNull(displayPhoneNumber, "displayPhoneNumber must not be null");
        this.event = Objects.requireNonNull(event, "event must not be null");
        this.requester = requester;
    }

    /**
     * Returns the display phone number the event concerns.
     *
     * @return the display phone number
     */
    public String displayPhoneNumber() {
        return displayPhoneNumber;
    }

    /**
     * Returns the security event.
     *
     * @return the event, for example {@code pin_changed}
     */
    public String event() {
        return event;
    }

    /**
     * Returns the identifier of the requester that initiated the event.
     *
     * @return an {@link Optional} carrying the requester, or empty when not reported
     */
    public Optional<String> requester() {
        return Optional.ofNullable(requester);
    }
}

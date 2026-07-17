package com.github.auties00.cobalt.wire.cloud;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A single contact entry delivered through an {@code smb_app_state_sync} webhook change.
 *
 * <p>WhatsApp Coexistence (the WhatsApp Business app sharing a number with the Cloud API) delivers the
 * business app's current and changed contacts shortly after onboarding succeeds. Each entry describes
 * one contact and the action that produced it. The {@code type} is currently always {@code contact};
 * the {@code action} is the add, update, or remove verb. This is a Cloud-only concept with no universal
 * counterpart, so it carries its own model rather than reusing the contact store types.
 */
public final class CloudAppStateSyncContact {
    /**
     * The entry type, currently always {@code contact}.
     */
    private final String type;

    /**
     * The action that produced the entry.
     */
    private final CloudAppStateSyncAction action;

    /**
     * The contact's full name, or {@code null} when not reported.
     */
    private final String fullName;

    /**
     * The contact's first name, or {@code null} when not reported.
     */
    private final String firstName;

    /**
     * The contact's phone number, or {@code null} when not reported.
     */
    private final String phoneNumber;

    /**
     * The per-entry timestamp, or {@code null} when not reported.
     */
    private final Instant timestamp;

    /**
     * Constructs a new app-state sync contact entry.
     *
     * @param type        the entry type
     * @param action      the action that produced the entry
     * @param fullName    the contact's full name, or {@code null}
     * @param firstName   the contact's first name, or {@code null}
     * @param phoneNumber the contact's phone number, or {@code null}
     * @param timestamp   the per-entry timestamp, or {@code null}
     * @throws NullPointerException if {@code type} or {@code action} is {@code null}
     */
    public CloudAppStateSyncContact(String type, CloudAppStateSyncAction action, String fullName, String firstName,
                                    String phoneNumber, Instant timestamp) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.fullName = fullName;
        this.firstName = firstName;
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
    }

    /**
     * Returns the entry type.
     *
     * @return the type, currently always {@code contact}
     */
    public String type() {
        return type;
    }

    /**
     * Returns the action that produced the entry.
     *
     * @return the {@link CloudAppStateSyncAction}
     */
    public CloudAppStateSyncAction action() {
        return action;
    }

    /**
     * Returns the contact's full name.
     *
     * @return an {@link Optional} carrying the full name, or empty when not reported
     */
    public Optional<String> fullName() {
        return Optional.ofNullable(fullName);
    }

    /**
     * Returns the contact's first name.
     *
     * @return an {@link Optional} carrying the first name, or empty when not reported
     */
    public Optional<String> firstName() {
        return Optional.ofNullable(firstName);
    }

    /**
     * Returns the contact's phone number.
     *
     * @return an {@link Optional} carrying the phone number, or empty when not reported
     */
    public Optional<String> phoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    /**
     * Returns the per-entry timestamp.
     *
     * @return an {@link Optional} carrying the timestamp, or empty when not reported
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }
}

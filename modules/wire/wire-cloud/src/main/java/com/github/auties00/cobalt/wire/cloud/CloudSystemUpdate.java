package com.github.auties00.cobalt.wire.cloud;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A Cloud-only system notification, decoded from a {@code system}-typed inbound message.
 *
 * <p>WhatsApp delivers identity-change notifications as inbound messages whose {@code type} is
 * {@code system}: a consumer changing their phone number, or their underlying account identity
 * changing. These have no universal counterpart in the shared message model, so they are surfaced
 * through this Cloud-only model in addition to the empty chat-message fallback.
 *
 * <p>The notification is a closed two-variant union selected by the wire {@code system.type} member:
 * {@link NumberChanged} reports a {@code customer_changed_number} transition and carries the new phone
 * number, while {@link IdentityChanged} reports a {@code customer_identity_changed} transition and
 * carries the new account identity hash. Pattern matching on the variant recovers the change-specific
 * field. Every variant shares the sender phone number, the optional human-readable body, the optional
 * customer identifier, and the optional notification timestamp.
 */
public sealed interface CloudSystemUpdate permits CloudSystemUpdate.NumberChanged, CloudSystemUpdate.IdentityChanged {
    /**
     * Returns the phone number of the consumer that sent the notification.
     *
     * <p>For a {@link NumberChanged} transition this is the prior phone number of the consumer, before
     * the change took effect.
     *
     * @return the sender phone number
     */
    String from();

    /**
     * Returns the human-readable system message body.
     *
     * @return an {@link Optional} carrying the body, or empty when not reported
     */
    Optional<String> body();

    /**
     * Returns the consumer's customer identifier.
     *
     * @return an {@link Optional} carrying the customer identifier, or empty when not reported
     */
    Optional<String> customer();

    /**
     * Returns the notification timestamp.
     *
     * @return an {@link Optional} carrying the timestamp, or empty when not reported
     */
    Optional<Instant> timestamp();

    /**
     * A {@code customer_changed_number} notification, reporting that a consumer moved to a new phone
     * number.
     *
     * <p>The {@link #newWaId() new phone number} is the number the consumer now uses; the shared
     * {@link #from()} accessor carries the prior number.
     */
    final class NumberChanged implements CloudSystemUpdate {
        /**
         * The prior phone number of the consumer, the inbound message sender.
         */
        private final String from;

        /**
         * The human-readable system message body, or {@code null} when not reported.
         */
        private final String body;

        /**
         * The consumer's customer identifier, or {@code null} when not reported.
         */
        private final String customer;

        /**
         * The notification timestamp, or {@code null} when not reported.
         */
        private final Instant timestamp;

        /**
         * The new phone number the consumer moved to.
         */
        private final String newWaId;

        /**
         * Constructs a new number-changed notification.
         *
         * @param from      the prior phone number of the consumer
         * @param body      the human-readable message body, or {@code null}
         * @param customer  the consumer's customer identifier, or {@code null}
         * @param timestamp the notification timestamp, or {@code null}
         * @param newWaId   the new phone number the consumer moved to
         * @throws NullPointerException if {@code from} or {@code newWaId} is {@code null}
         */
        public NumberChanged(String from, String body, String customer, Instant timestamp, String newWaId) {
            this.from = Objects.requireNonNull(from, "from must not be null");
            this.body = body;
            this.customer = customer;
            this.timestamp = timestamp;
            this.newWaId = Objects.requireNonNull(newWaId, "newWaId must not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String from() {
            return from;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> body() {
            return Optional.ofNullable(body);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> customer() {
            return Optional.ofNullable(customer);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }

        /**
         * Returns the new phone number the consumer moved to.
         *
         * @return the new phone number
         */
        public String newWaId() {
            return newWaId;
        }
    }

    /**
     * A {@code customer_identity_changed} notification, reporting that a consumer's underlying account
     * identity changed.
     *
     * <p>The {@link #identity() identity hash} fingerprints the consumer's new account identity; a
     * change typically signals a SIM swap or device reinstall and may warrant re-establishing trust.
     */
    final class IdentityChanged implements CloudSystemUpdate {
        /**
         * The phone number of the consumer, the inbound message sender.
         */
        private final String from;

        /**
         * The human-readable system message body, or {@code null} when not reported.
         */
        private final String body;

        /**
         * The consumer's customer identifier, or {@code null} when not reported.
         */
        private final String customer;

        /**
         * The notification timestamp, or {@code null} when not reported.
         */
        private final Instant timestamp;

        /**
         * The new account identity hash.
         */
        private final String identity;

        /**
         * Constructs a new identity-changed notification.
         *
         * @param from      the phone number of the consumer
         * @param body      the human-readable message body, or {@code null}
         * @param customer  the consumer's customer identifier, or {@code null}
         * @param timestamp the notification timestamp, or {@code null}
         * @param identity  the new account identity hash
         * @throws NullPointerException if {@code from} or {@code identity} is {@code null}
         */
        public IdentityChanged(String from, String body, String customer, Instant timestamp, String identity) {
            this.from = Objects.requireNonNull(from, "from must not be null");
            this.body = body;
            this.customer = customer;
            this.timestamp = timestamp;
            this.identity = Objects.requireNonNull(identity, "identity must not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String from() {
            return from;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> body() {
            return Optional.ofNullable(body);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> customer() {
            return Optional.ofNullable(customer);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }

        /**
         * Returns the new account identity hash.
         *
         * @return the identity hash
         */
        public String identity() {
            return identity;
        }
    }
}

package com.github.auties00.cobalt.wire.cloud.flow;

import java.util.Objects;
import java.util.Optional;

/**
 * A Flow lifecycle or health event, decoded from a {@code flows} webhook change.
 *
 * <p>The platform reports both status transitions (draft to published, published to deprecated) and
 * endpoint health alerts (error rate, latency, availability) for the Flows of a WhatsApp Business
 * Account. The event is a closed two-variant union: a {@link StatusChange} carries the
 * {@linkplain StatusChange#oldStatus() old} and {@linkplain StatusChange#newStatus() new} status of a
 * lifecycle transition, while an {@link EndpointHealth} carries an endpoint health alert. Pattern
 * matching on the variant recovers the event-specific data; every variant shares the
 * {@linkplain #event() event kind}, the optional {@linkplain #flowId() flow id}, and the optional
 * human-readable {@linkplain #message() message}. The {@link #event()} accessor stays visible so an
 * event kind this client does not yet specialise still surfaces its raw token.
 */
public sealed interface CloudFlowStatusUpdate permits CloudFlowStatusUpdate.StatusChange,
        CloudFlowStatusUpdate.EndpointHealth {
    /**
     * Returns the event kind.
     *
     * @return the event, for example {@code FLOW_STATUS_CHANGE} or {@code ENDPOINT_ERROR_RATE}
     */
    String event();

    /**
     * Returns the server-assigned flow id.
     *
     * @return an {@link Optional} carrying the id, or empty when not reported
     */
    Optional<String> flowId();

    /**
     * Returns the human-readable event description.
     *
     * @return an {@link Optional} carrying the description, or empty when not reported
     */
    Optional<String> message();

    /**
     * A Flow lifecycle status transition.
     *
     * <p>Reports the {@linkplain #oldStatus() old} and {@linkplain #newStatus() new} status of a Flow
     * as it moves through its lifecycle, for example draft to published.
     */
    final class StatusChange implements CloudFlowStatusUpdate {
        /**
         * The event kind.
         */
        private final String event;

        /**
         * The server-assigned flow id, or {@code null} when not reported.
         */
        private final String flowId;

        /**
         * The human-readable event description, or {@code null} when not reported.
         */
        private final String message;

        /**
         * The status before the transition, or {@code null} when not reported.
         */
        private final String oldStatus;

        /**
         * The status after the transition, or {@code null} when not reported.
         */
        private final String newStatus;

        /**
         * Constructs a new status-change event.
         *
         * @param event     the event kind
         * @param flowId    the server-assigned flow id, or {@code null}
         * @param message   the human-readable description, or {@code null}
         * @param oldStatus the status before the transition, or {@code null}
         * @param newStatus the status after the transition, or {@code null}
         * @throws NullPointerException if {@code event} is {@code null}
         */
        public StatusChange(String event, String flowId, String message, String oldStatus, String newStatus) {
            this.event = Objects.requireNonNull(event, "event must not be null");
            this.flowId = flowId;
            this.message = message;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String event() {
            return event;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> flowId() {
            return Optional.ofNullable(flowId);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> message() {
            return Optional.ofNullable(message);
        }

        /**
         * Returns the status before the transition.
         *
         * @return an {@link Optional} carrying the old status, or empty when not reported
         */
        public Optional<String> oldStatus() {
            return Optional.ofNullable(oldStatus);
        }

        /**
         * Returns the status after the transition.
         *
         * @return an {@link Optional} carrying the new status, or empty when not reported
         */
        public Optional<String> newStatus() {
            return Optional.ofNullable(newStatus);
        }
    }

    /**
     * A Flow endpoint health alert.
     *
     * <p>Reports an error-rate, latency, or availability alert for the endpoint a Flow exchanges data
     * with; the human-readable {@linkplain #message() message} describes the alert.
     */
    final class EndpointHealth implements CloudFlowStatusUpdate {
        /**
         * The event kind.
         */
        private final String event;

        /**
         * The server-assigned flow id, or {@code null} when not reported.
         */
        private final String flowId;

        /**
         * The human-readable event description, or {@code null} when not reported.
         */
        private final String message;

        /**
         * Constructs a new endpoint-health event.
         *
         * @param event   the event kind
         * @param flowId  the server-assigned flow id, or {@code null}
         * @param message the human-readable description, or {@code null}
         * @throws NullPointerException if {@code event} is {@code null}
         */
        public EndpointHealth(String event, String flowId, String message) {
            this.event = Objects.requireNonNull(event, "event must not be null");
            this.flowId = flowId;
            this.message = message;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String event() {
            return event;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> flowId() {
            return Optional.ofNullable(flowId);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> message() {
            return Optional.ofNullable(message);
        }
    }
}

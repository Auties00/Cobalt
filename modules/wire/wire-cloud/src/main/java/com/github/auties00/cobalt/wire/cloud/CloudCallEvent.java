package com.github.auties00.cobalt.wire.cloud;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A WhatsApp Cloud API calling event, decoded from a {@code calls} webhook change or from a
 * call-permission reply riding the {@code messages} webhook.
 *
 * <p>The Calling API delivers four semantically distinct events, each carrying a different field set, so
 * this type is a sealed hierarchy rather than one wide record. The two inbound signaling events, grouped
 * under {@link Signaling}, are the {@link Connect} carrying an SDP offer and the {@link Terminate}
 * carrying the final disposition (status, duration, and the start and end timestamps); they are delivered
 * to the call listener. The {@link Status} event reports the lifecycle of a business-initiated call
 * ({@code RINGING}, {@code ACCEPTED}, {@code REJECTED}). The {@link PermissionReply} event reports a
 * consumer's accept or reject of a permission request and is delivered as an interactive message rather
 * than over the {@code calls} change. Every variant carries the call identifier, the other party's phone
 * number, and an optional event timestamp; matching on the sealed type recovers the variant-specific
 * fields.
 */
public sealed interface CloudCallEvent permits CloudCallEvent.Signaling, CloudCallEvent.Status,
        CloudCallEvent.PermissionReply {
    /**
     * Returns the call identifier.
     *
     * @return the call id, or the message id for a {@link PermissionReply}
     */
    String callId();

    /**
     * Returns the other party's phone number in E.164 form.
     *
     * @return the phone number
     */
    String from();

    /**
     * Returns the event timestamp.
     *
     * @return an {@link Optional} carrying the timestamp, or empty when the event carried none
     */
    Optional<Instant> timestamp();

    /**
     * An inbound signaling event delivered over the {@code calls} webhook change, either a
     * {@link Connect} offer or a {@link Terminate} disposition.
     *
     * <p>The two signaling events are the only ones the call listener receives, so they share this
     * sealed sub-type to give the callback a single, exhaustively matchable parameter type.
     */
    sealed interface Signaling extends CloudCallEvent permits Connect, Terminate {
    }

    /**
     * An inbound call offer carrying an SDP session description.
     *
     * <p>A consumer placing a call delivers a {@code connect} event whose {@code session} object carries
     * an SDP offer; answering it requires reading the carried {@link #session()}. The direction is
     * present on the wire and is normally {@link CloudCallDirection#USER_INITIATED} for an inbound offer.
     */
    final class Connect implements Signaling {
        /**
         * The call identifier assigned by the server.
         */
        private final String callId;

        /**
         * The calling consumer's phone number in E.164 form.
         */
        private final String from;

        /**
         * The business phone number the call was placed to, or {@code null} when the event carried none.
         */
        private final String to;

        /**
         * The call direction, normally {@link CloudCallDirection#USER_INITIATED}, or {@code null} when
         * the event carried none.
         */
        private final CloudCallDirection direction;

        /**
         * The SDP session carried on the offer, normally an {@link CloudCallSession.Type#OFFER}, or
         * {@code null} when the event carried no session.
         */
        private final CloudCallSession session;

        /**
         * The event timestamp, or {@code null} when absent.
         */
        private final Instant timestamp;

        /**
         * Constructs a new connect event.
         *
         * @param callId    the call identifier
         * @param from      the calling consumer's phone number in E.164 form
         * @param to        the business phone number the call was placed to, or {@code null}
         * @param direction the call direction, or {@code null} when none
         * @param session   the SDP session, or {@code null} when none
         * @param timestamp the event timestamp, or {@code null} when absent
         * @throws NullPointerException if {@code callId} or {@code from} is {@code null}
         */
        public Connect(String callId, String from, String to, CloudCallDirection direction,
                       CloudCallSession session, Instant timestamp) {
            this.callId = Objects.requireNonNull(callId, "callId must not be null");
            this.from = Objects.requireNonNull(from, "from must not be null");
            this.to = to;
            this.direction = direction;
            this.session = session;
            this.timestamp = timestamp;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String callId() {
            return callId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String from() {
            return from;
        }

        /**
         * Returns the business phone number the call was placed to.
         *
         * @return an {@link Optional} carrying the number, or empty when the event carried none
         */
        public Optional<String> to() {
            return Optional.ofNullable(to);
        }

        /**
         * Returns the call direction.
         *
         * @return an {@link Optional} carrying the {@link CloudCallDirection}, normally
         *         {@link CloudCallDirection#USER_INITIATED}, or empty when the event carried none
         */
        public Optional<CloudCallDirection> direction() {
            return Optional.ofNullable(direction);
        }

        /**
         * Returns the SDP session carried on the offer.
         *
         * @return an {@link Optional} carrying the {@link CloudCallSession}, normally an
         *         {@link CloudCallSession.Type#OFFER}, or empty when the event carried no session
         */
        public Optional<CloudCallSession> session() {
            return Optional.ofNullable(session);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }
    }

    /**
     * A call-end event carrying the final disposition of a call.
     *
     * <p>A {@code terminate} event reports the final {@link #status()} alongside the call
     * {@link #durationSeconds() duration} and the {@link #startTime() start} and {@link #endTime() end}
     * timestamps when the call connected.
     */
    final class Terminate implements Signaling {
        /**
         * The call identifier assigned by the server.
         */
        private final String callId;

        /**
         * The other party's phone number in E.164 form.
         */
        private final String from;

        /**
         * The business phone number the call was placed to, or {@code null} when the event carried none.
         */
        private final String to;

        /**
         * The call direction, or {@code null} when the event carried none.
         */
        private final CloudCallDirection direction;

        /**
         * The terminate status, for example {@code "COMPLETED"}, {@code "FAILED"}, or {@code "REJECTED"},
         * or {@code null} when the event carried none.
         */
        private final String status;

        /**
         * The call duration in seconds, or {@code null} when the event carried none.
         */
        private final Integer durationSeconds;

        /**
         * The call start time, or {@code null} when the event carried none.
         */
        private final Instant startTime;

        /**
         * The call end time, or {@code null} when the event carried none.
         */
        private final Instant endTime;

        /**
         * The event timestamp, or {@code null} when absent.
         */
        private final Instant timestamp;

        /**
         * Constructs a new terminate event.
         *
         * @param callId          the call identifier
         * @param from            the other party's phone number in E.164 form
         * @param to              the business phone number the call was placed to, or {@code null}
         * @param direction       the call direction, or {@code null} when none
         * @param status          the terminate status, or {@code null} when none
         * @param durationSeconds the call duration in seconds, or {@code null} when none
         * @param startTime       the call start time, or {@code null} when none
         * @param endTime         the call end time, or {@code null} when none
         * @param timestamp       the event timestamp, or {@code null} when absent
         * @throws NullPointerException if {@code callId} or {@code from} is {@code null}
         */
        public Terminate(String callId, String from, String to, CloudCallDirection direction, String status,
                         Integer durationSeconds, Instant startTime, Instant endTime, Instant timestamp) {
            this.callId = Objects.requireNonNull(callId, "callId must not be null");
            this.from = Objects.requireNonNull(from, "from must not be null");
            this.to = to;
            this.direction = direction;
            this.status = status;
            this.durationSeconds = durationSeconds;
            this.startTime = startTime;
            this.endTime = endTime;
            this.timestamp = timestamp;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String callId() {
            return callId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String from() {
            return from;
        }

        /**
         * Returns the business phone number the call was placed to.
         *
         * @return an {@link Optional} carrying the number, or empty when the event carried none
         */
        public Optional<String> to() {
            return Optional.ofNullable(to);
        }

        /**
         * Returns the call direction.
         *
         * @return an {@link Optional} carrying the {@link CloudCallDirection}, or empty when the event
         *         carried none
         */
        public Optional<CloudCallDirection> direction() {
            return Optional.ofNullable(direction);
        }

        /**
         * Returns the terminate status.
         *
         * @return an {@link Optional} carrying the status, or empty when the event carried none
         */
        public Optional<String> status() {
            return Optional.ofNullable(status);
        }

        /**
         * Returns the call duration in seconds.
         *
         * @return an {@link OptionalInt} carrying the duration, or empty when the event carried none
         */
        public OptionalInt durationSeconds() {
            return durationSeconds == null ? OptionalInt.empty() : OptionalInt.of(durationSeconds);
        }

        /**
         * Returns the call start time.
         *
         * @return an {@link Optional} carrying the start time, or empty when the event carried none
         */
        public Optional<Instant> startTime() {
            return Optional.ofNullable(startTime);
        }

        /**
         * Returns the call end time.
         *
         * @return an {@link Optional} carrying the end time, or empty when the event carried none
         */
        public Optional<Instant> endTime() {
            return Optional.ofNullable(endTime);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }
    }

    /**
     * A business-initiated call status transition.
     *
     * <p>A call the business places reports its lifecycle ({@link #status()} is one of
     * {@link CloudCallStatus#RINGING}, {@link CloudCallStatus#ACCEPTED}, or
     * {@link CloudCallStatus#REJECTED}) through a {@code statuses[]} entry typed {@code call}. The
     * callee phone number is the only party present and is carried by {@link #from()}; the
     * {@link #direction()} is {@link CloudCallDirection#BUSINESS_INITIATED}.
     */
    final class Status implements CloudCallEvent {
        /**
         * The call identifier assigned by the server.
         */
        private final String callId;

        /**
         * The callee phone number in E.164 form.
         */
        private final String from;

        /**
         * The reported status.
         */
        private final CloudCallStatus status;

        /**
         * The call direction, {@link CloudCallDirection#BUSINESS_INITIATED}.
         */
        private final CloudCallDirection direction;

        /**
         * The event timestamp, or {@code null} when absent.
         */
        private final Instant timestamp;

        /**
         * Constructs a new business-initiated call status event.
         *
         * @param callId    the call identifier
         * @param from      the callee phone number in E.164 form
         * @param status    the reported status
         * @param direction the call direction
         * @param timestamp the event timestamp, or {@code null} when absent
         * @throws NullPointerException if {@code callId}, {@code from}, {@code status}, or
         *                              {@code direction} is {@code null}
         */
        public Status(String callId, String from, CloudCallStatus status, CloudCallDirection direction,
                      Instant timestamp) {
            this.callId = Objects.requireNonNull(callId, "callId must not be null");
            this.from = Objects.requireNonNull(from, "from must not be null");
            this.status = Objects.requireNonNull(status, "status must not be null");
            this.direction = Objects.requireNonNull(direction, "direction must not be null");
            this.timestamp = timestamp;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String callId() {
            return callId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String from() {
            return from;
        }

        /**
         * Returns the reported status.
         *
         * @return the {@link CloudCallStatus}
         */
        public CloudCallStatus status() {
            return status;
        }

        /**
         * Returns the call direction.
         *
         * @return the direction, {@link CloudCallDirection#BUSINESS_INITIATED}
         */
        public CloudCallDirection direction() {
            return direction;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }
    }

    /**
     * A consumer's reply to a call-permission request.
     *
     * <p>A consumer accepting or rejecting a permission request delivers an interactive message whose
     * {@code interactive.type} is {@code call_permission_reply}. The {@link #permissionResponse()} is
     * {@link CloudCallPermissionResponse#ACCEPT} or {@link CloudCallPermissionResponse#REJECT}; an
     * accepted permission carries a {@link #permissionExpiration() expiration} and a rejected one does
     * not. The {@link #callId()} carries the message id of the reply.
     */
    final class PermissionReply implements CloudCallEvent {
        /**
         * The message id of the permission reply.
         */
        private final String callId;

        /**
         * The replying consumer's phone number in E.164 form.
         */
        private final String from;

        /**
         * The permission reply response.
         */
        private final CloudCallPermissionResponse permissionResponse;

        /**
         * The source of the permission reply, for example {@code "user_action"}, or {@code null} when the
         * event carried none.
         */
        private final String permissionResponseSource;

        /**
         * The expiration of a granted permission, or {@code null} when none was granted.
         */
        private final Instant permissionExpiration;

        /**
         * The event timestamp, or {@code null} when absent.
         */
        private final Instant timestamp;

        /**
         * Constructs a new call-permission reply event.
         *
         * @param callId                   the message id of the permission reply
         * @param from                     the replying consumer's phone number in E.164 form
         * @param permissionResponse       the response
         * @param permissionResponseSource the reply source, or {@code null} when none
         * @param permissionExpiration     the granted permission expiration, or {@code null} when none
         * @param timestamp                the event timestamp, or {@code null} when absent
         * @throws NullPointerException if {@code callId}, {@code from}, or {@code permissionResponse} is
         *                              {@code null}
         */
        public PermissionReply(String callId, String from, CloudCallPermissionResponse permissionResponse,
                               String permissionResponseSource, Instant permissionExpiration, Instant timestamp) {
            this.callId = Objects.requireNonNull(callId, "callId must not be null");
            this.from = Objects.requireNonNull(from, "from must not be null");
            this.permissionResponse = Objects.requireNonNull(permissionResponse, "permissionResponse must not be null");
            this.permissionResponseSource = permissionResponseSource;
            this.permissionExpiration = permissionExpiration;
            this.timestamp = timestamp;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String callId() {
            return callId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String from() {
            return from;
        }

        /**
         * Returns the permission reply response.
         *
         * @return the {@link CloudCallPermissionResponse}
         */
        public CloudCallPermissionResponse permissionResponse() {
            return permissionResponse;
        }

        /**
         * Returns the source of the permission reply.
         *
         * @return an {@link Optional} carrying the source, for example {@code "user_action"}, or empty
         *         when the event carried none
         */
        public Optional<String> permissionResponseSource() {
            return Optional.ofNullable(permissionResponseSource);
        }

        /**
         * Returns the expiration of a granted permission.
         *
         * @return an {@link Optional} carrying the expiration, or empty when none was granted
         */
        public Optional<Instant> permissionExpiration() {
            return Optional.ofNullable(permissionExpiration);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }
    }
}

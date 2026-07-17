package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Sealed reply family for a {@link SmaxGroupsCancelGroupMembershipRequestsRequest}.
 *
 * The three variants partition every reply the relay can return: {@link Success}, {@link ClientError} and
 * {@link ServerError}. {@link Success} ships a per-participant outcome list; callers must walk
 * {@link Success#participants()} and inspect {@link Success.CancelParticipantResult#rejectionReason()} to surface
 * per-row failures.
 */
public sealed interface SmaxGroupsCancelGroupMembershipRequestsResponse extends SmaxStanza.Response
        permits SmaxGroupsCancelGroupMembershipRequestsResponse.Success, SmaxGroupsCancelGroupMembershipRequestsResponse.ClientError, SmaxGroupsCancelGroupMembershipRequestsResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsCancelGroupMembershipRequestsResponse} variant in
     * priority order and returns the first that parses cleanly.
     *
     * {@link Success} is probed first, then {@link ClientError}, then {@link ServerError}.
     *
     * @implNote This implementation returns an empty {@link Optional} when the stanza shape matches none of the
     * documented variants; WA Web throws a parsing failure on the same path, but Cobalt defers the decision to the
     * caller so it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsCancelGroupMembershipRequestsRequest} stanza, used to
     *                validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsCancelGroupMembershipRequestsRPC",
            exports = "sendCancelGroupMembershipRequestsRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsCancelGroupMembershipRequestsResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Reply variant carrying the per-participant outcome list when the relay processed the cancellation envelope.
     *
     * The IQ envelope succeeds even when individual rows are rejected; callers must walk {@link #participants()}
     * to detect partial rejection.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCancelGroupMembershipRequestsResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupAddressingModeMixin")
    final class Success implements SmaxGroupsCancelGroupMembershipRequestsResponse {
        /**
         * Holds the optional {@code addressing_mode} attribute echoed on the IQ envelope.
         */
        private final String addressingMode;

        /**
         * Holds the per-participant outcome rows.
         */
        private final List<CancelParticipantResult> participants;

        /**
         * Constructs a {@link Success}.
         *
         * The supplied participant list is defensively copied and a {@code null} value is treated as empty.
         *
         * @param addressingMode the optional addressing-mode echo; may be {@code null}
         * @param participants   the per-participant outcomes; may be {@code null}
         */
        public Success(String addressingMode, List<CancelParticipantResult> participants) {
            this.addressingMode = addressingMode;
            this.participants = List.copyOf(Objects.requireNonNullElse(participants, List.of()));
        }

        /**
         * Returns the optional {@code addressing_mode} echo.
         *
         * @return an {@link Optional} carrying the addressing mode, or empty when the relay omitted it
         */
        public Optional<String> addressingMode() {
            return Optional.ofNullable(addressingMode);
        }

        /**
         * Returns the per-participant outcome rows.
         *
         * @return an unmodifiable list of outcome rows; never {@code null}
         */
        public List<CancelParticipantResult> participants() {
            return participants;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * The IQ must be a valid {@code type="result"} echo of the request and must carry a
         * {@code <cancel_membership_requests>} child whose {@code <participant>} grand-children satisfy
         * {@link CancelParticipantResult#of(Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCancelGroupMembershipRequestsResponseSuccess",
                exports = "parseCancelGroupMembershipRequestsResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var cancel = stanza.getChild("cancel_membership_requests").orElse(null);
            if (cancel == null) {
                return Optional.empty();
            }
            var addressingMode = stanza.getAttributeAsString("addressing_mode").orElse(null);
            var participants = new ArrayList<CancelParticipantResult>();
            for (var participantNode : cancel.getChildren("participant")) {
                var participant = CancelParticipantResult.of(participantNode).orElse(null);
                if (participant == null) {
                    return Optional.empty();
                }
                participants.add(participant);
            }
            return Optional.of(new Success(addressingMode, participants));
        }

        /**
         * Compares this success to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Success} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.addressingMode, that.addressingMode)
                    && Objects.equals(this.participants, that.participants);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(addressingMode, participants);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsCancelGroupMembershipRequestsResponse.Success[addressingMode="
                    + addressingMode + ", participants=" + participants + ']';
        }

        /**
         * Per-participant outcome row for a single cancellation target.
         *
         * The row surfaces the cancelled {@link Jid}, the optional phone-number echo, and an optional
         * {@link RejectionReason} payload identifying which arm of the cancellation disjunction the relay took
         * when the row was refused.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsMembershipRequestsCancellationParticipantMixins")
        public static final class CancelParticipantResult {
            /**
             * Holds the participant {@link Jid}.
             */
            private final Jid jid;

            /**
             * Holds the optional {@code phone_number} echo.
             */
            private final String phoneNumber;

            /**
             * Holds the optional rejection-reason payload, populated only when the relay refused the row.
             */
            private final RejectionReason rejectionReason;

            /**
             * Constructs a {@link CancelParticipantResult} row.
             *
             * @param jid             the participant {@link Jid}
             * @param phoneNumber     the optional {@code phone_number} echo; may be {@code null}
             * @param rejectionReason the optional rejection reason; may be {@code null}
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public CancelParticipantResult(Jid jid, String phoneNumber,
                                           RejectionReason rejectionReason) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.phoneNumber = phoneNumber;
                this.rejectionReason = rejectionReason;
            }

            /**
             * Returns the participant {@link Jid}.
             *
             * @return the {@link Jid}; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns the optional {@code phone_number} echo.
             *
             * @return an {@link Optional} carrying the phone number, or empty when the relay omitted it
             */
            public Optional<String> phoneNumber() {
                return Optional.ofNullable(phoneNumber);
            }

            /**
             * Returns the optional rejection-reason payload.
             *
             * @return an {@link Optional} carrying the reason, or empty when the cancellation succeeded
             */
            public Optional<RejectionReason> rejectionReason() {
                return Optional.ofNullable(rejectionReason);
            }

            /**
             * Tries to parse a {@link CancelParticipantResult} row from a single {@code <participant>} child.
             *
             * The stanza must be a {@code <participant>} carrying a {@code jid} attribute, with an optional
             * {@code <request_not_found/>} or {@code <not_authorized/>} child triggering the rejection arm.
             *
             * @param stanza the {@code <participant>} child
             * @return an {@link Optional} carrying the parsed row, or empty when the stanza does not match
             */
            @WhatsAppWebExport(moduleName = "WASmaxInGroupsCancelGroupMembershipRequestsResponseSuccess",
                    exports = "parseCancelGroupMembershipRequestsResponseSuccessCancelMembershipRequestsParticipant",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<CancelParticipantResult> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("participant")) {
                    return Optional.empty();
                }
                var jid = stanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var phoneNumber = stanza.getAttributeAsString("phone_number").orElse(null);
                var rejectionReason = RejectionReason.of(stanza).orElse(null);
                return Optional.of(new CancelParticipantResult(jid, phoneNumber, rejectionReason));
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is a {@link CancelParticipantResult} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (CancelParticipantResult) obj;
                return Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.phoneNumber, that.phoneNumber)
                        && Objects.equals(this.rejectionReason, that.rejectionReason);
            }

            /**
             * Returns a hash composed of every field.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, phoneNumber, rejectionReason);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsCancelGroupMembershipRequestsResponse.Success.CancelParticipantResult[jid="
                        + jid
                        + ", phoneNumber=" + phoneNumber
                        + ", rejectionReason=" + rejectionReason + ']';
            }

            /**
             * Rejection-reason payload for a participant the relay refused to cancel.
             *
             * Identifies which arm of the disjunction the relay took: {@link Kind#REQUEST_NOT_FOUND} (the
             * participant has no pending request on this group) or {@link Kind#NOT_AUTHORIZED} (the caller does
             * not own the targeted request and lacks admin rights to cancel on behalf of others).
             */
            @WhatsAppWebModule(moduleName = "WASmaxInGroupsCancelGroupMembershipRequestsParticipantRequestNotFoundMixin")
            @WhatsAppWebModule(moduleName = "WASmaxInGroupsParticipantNotAuthorizedMixin")
            public static final class RejectionReason {
                /**
                 * Holds the rejection-arm marker.
                 */
                private final Kind kind;

                /**
                 * Constructs a {@link RejectionReason}.
                 *
                 * @param kind the rejection-arm marker
                 * @throws NullPointerException if {@code kind} is {@code null}
                 */
                public RejectionReason(Kind kind) {
                    this.kind = Objects.requireNonNull(kind, "kind cannot be null");
                }

                /**
                 * Returns the rejection-arm marker.
                 *
                 * @return the kind; never {@code null}
                 */
                public Kind kind() {
                    return kind;
                }

                /**
                 * Tries to parse a {@link RejectionReason} payload from a {@code <participant>} child.
                 *
                 * The presence of a {@code <request_not_found/>} or {@code <not_authorized/>} grand-child
                 * discriminates the rejected arms; absence signals the accepted arm.
                 *
                 * @param stanza the {@code <participant>} child
                 * @return an {@link Optional} carrying the parsed payload, or empty when the row succeeded
                 */
                @WhatsAppWebExport(moduleName = "WASmaxInGroupsMembershipRequestsCancellationParticipantMixins",
                        exports = "parseMembershipRequestsCancellationParticipantMixins",
                        adaptation = WhatsAppAdaptation.ADAPTED)
                public static Optional<RejectionReason> of(Stanza stanza) {
                    Objects.requireNonNull(stanza, "stanza cannot be null");
                    if (stanza.getChild("request_not_found").isPresent()) {
                        return Optional.of(new RejectionReason(Kind.REQUEST_NOT_FOUND));
                    }
                    if (stanza.getChild("not_authorized").isPresent()) {
                        return Optional.of(new RejectionReason(Kind.NOT_AUTHORIZED));
                    }
                    return Optional.empty();
                }

                /**
                 * Compares this payload to {@code obj} for value equality on {@link #kind()}.
                 *
                 * @param obj the other object
                 * @return {@code true} when {@code obj} is a {@link RejectionReason} with the same kind
                 */
                @Override
                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    }
                    if (obj == null || obj.getClass() != this.getClass()) {
                        return false;
                    }
                    var that = (RejectionReason) obj;
                    return this.kind == that.kind;
                }

                /**
                 * Returns a hash derived from {@link #kind()}.
                 *
                 * @return the hash code
                 */
                @Override
                public int hashCode() {
                    return Objects.hash(kind);
                }

                /**
                 * Returns a debug string carrying {@link #kind()}.
                 *
                 * @return the debug representation
                 */
                @Override
                public String toString() {
                    return "SmaxGroupsCancelGroupMembershipRequestsResponse.Success.CancelParticipantResult.RejectionReason[kind="
                            + kind + ']';
                }

                /**
                 * Discriminator for the cancellation-rejection arms.
                 *
                 * Each constant maps to one documented rejection arm.
                 */
                public enum Kind {
                    /**
                     * Indicates the relay could not find a pending membership request matching the supplied
                     * participant on this group.
                     */
                    REQUEST_NOT_FOUND,

                    /**
                     * Indicates the caller is not authorised to cancel the targeted request; typically because
                     * they neither own it nor hold admin rights on the group.
                     */
                    NOT_AUTHORIZED
                }
            }
        }
    }

    /**
     * Reply variant emitted when the relay rejected the request envelope as malformed or unauthorised.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCancelGroupMembershipRequestsResponseClientError")
    final class ClientError implements SmaxGroupsCancelGroupMembershipRequestsResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the relay.
         */
        private final String errorText;

        /**
         * Constructs a {@link ClientError} from raw error attributes.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code echoed by the relay.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text echoed by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from {@code stanza}.
         *
         * Delegates the envelope validation to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)},
         * which checks the shared {@code <iq type="error"><error code="..." text="..."/></iq>} shape.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCancelGroupMembershipRequestsResponseClientError",
                exports = "parseCancelGroupMembershipRequestsResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this error to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link ClientError} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsCancelGroupMembershipRequestsResponse.ClientError[errorCode="
                    + errorCode + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Reply variant emitted on transient relay-side failure.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCancelGroupMembershipRequestsResponseServerError")
    final class ServerError implements SmaxGroupsCancelGroupMembershipRequestsResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the relay.
         */
        private final String errorText;

        /**
         * Constructs a {@link ServerError} from raw error attributes.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code echoed by the relay.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text echoed by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from {@code stanza}.
         *
         * Delegates the envelope validation to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)},
         * which checks the shared {@code <iq type="error"><error code="..." text="..."/></iq>} shape.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCancelGroupMembershipRequestsResponseServerError",
                exports = "parseCancelGroupMembershipRequestsResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this error to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link ServerError} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsCancelGroupMembershipRequestsResponse.ServerError[errorCode="
                    + errorCode + ", errorText=" + errorText + ']';
        }
    }
}

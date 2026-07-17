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
 * The sealed reply family for a {@link SmaxGroupsRemoveParticipantsRequest}.
 *
 * <p>{@link Success} always wraps the per-participant outcome list returned by the relay; the envelope succeeds
 * even when some entries are rejected at the participant-policy level, so callers must walk
 * {@link Success#participants()} to detect partial failures. {@link ClientError} and {@link ServerError} surface
 * the relay's reason codes.
 */
public sealed interface SmaxGroupsRemoveParticipantsResponse extends SmaxStanza.Response
        permits SmaxGroupsRemoveParticipantsResponse.Success, SmaxGroupsRemoveParticipantsResponse.ClientError, SmaxGroupsRemoveParticipantsResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsRemoveParticipantsResponse} variant in priority
     * order and returns the first that parses cleanly.
     *
     * <p>Tries {@link Success} first, then {@link ClientError}, then {@link ServerError}.
     *
     * @implNote The empty {@link Optional} surfaces when the stanza shape matches none of the documented
     * variants; WA Web throws {@code SmaxParsingFailure} on the same path, but Cobalt defers the decision to the
     * caller so it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsRemoveParticipantsRequest} stanza, used to validate
     *                echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsRemoveParticipantsRPC",
            exports = "sendRemoveParticipantsRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsRemoveParticipantsResponse> of(Stanza stanza, Stanza request) {
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
     * The reply variant carrying the per-participant outcome list when the relay accepted the request envelope.
     *
     * <p>The IQ envelope succeeds even when every candidate is rejected at the participant-policy level
     * (not-in-group, not-allowed, not-acceptable, linked-groups-server-error); callers must walk
     * {@link #participants()} to detect partial or total rejection. The relay echoes back both the
     * {@code linked_groups} flag (via {@link #removeLinkedGroups()}) and the addressing mode (via
     * {@link #addressingMode()}).
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsRemoveParticipantsResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupAddressingModeMixin")
    final class Success implements SmaxGroupsRemoveParticipantsResponse {
        /**
         * The {@code linked_groups="true"} echo lifted from the {@code <remove>} child.
         */
        private final boolean removeLinkedGroups;

        /**
         * The optional {@code addressing_mode} attribute echoed on the IQ envelope.
         */
        private final String addressingMode;

        /**
         * The per-participant outcome rows projected from the {@code <remove>} child.
         */
        private final List<RemoveParticipantResult> participants;

        /**
         * Constructs a {@link Success}.
         *
         * @param removeLinkedGroups whether the relay echoed the {@code linked_groups="true"} flag
         * @param addressingMode     the optional addressing-mode echo ({@code "lid"} or {@code "pn"}); may be
         *                           {@code null}
         * @param participants       the per-participant outcomes; defensively copied, {@code null} treated as
         *                           empty
         */
        public Success(boolean removeLinkedGroups, String addressingMode,
                       List<RemoveParticipantResult> participants) {
            this.removeLinkedGroups = removeLinkedGroups;
            this.addressingMode = addressingMode;
            this.participants = List.copyOf(Objects.requireNonNullElse(participants, List.of()));
        }

        /**
         * Returns whether the relay echoed {@code linked_groups="true"}.
         *
         * @return {@code true} when the cascade flag was echoed back
         */
        public boolean removeLinkedGroups() {
            return removeLinkedGroups;
        }

        /**
         * Returns the optional {@code addressing_mode} echo.
         *
         * <p>The relay flips between {@code "lid"} and {@code "pn"} according to the group's addressing mode; the
         * field is omitted on legacy groups.
         *
         * @return an {@link Optional} carrying the mode, or empty when the relay omitted it
         */
        public Optional<String> addressingMode() {
            return Optional.ofNullable(addressingMode);
        }

        /**
         * Returns the per-participant outcome rows.
         *
         * @return an unmodifiable list of outcome rows; never {@code null}
         */
        public List<RemoveParticipantResult> participants() {
            return participants;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * <p>The IQ must be a valid {@code type="result"} echo of the request, must carry a {@code <remove>} child,
         * and every {@code <participant>} grand-child must satisfy {@link RemoveParticipantResult#of(Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsRemoveParticipantsResponseSuccess",
                exports = "parseRemoveParticipantsResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var remove = stanza.getChild("remove").orElse(null);
            if (remove == null) {
                return Optional.empty();
            }
            var linkedGroups = remove.hasAttribute("linked_groups", "true");
            var addressingMode = stanza.getAttributeAsString("addressing_mode").orElse(null);
            var participants = new ArrayList<RemoveParticipantResult>();
            for (var participantNode : remove.getChildren("participant")) {
                var participant = RemoveParticipantResult.of(participantNode).orElse(null);
                if (participant == null) {
                    return Optional.empty();
                }
                participants.add(participant);
            }
            return Optional.of(new Success(linkedGroups, addressingMode, participants));
        }

        /**
         * Compares this success to {@code obj} for value equality across every field.
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
            return this.removeLinkedGroups == that.removeLinkedGroups
                    && Objects.equals(this.addressingMode, that.addressingMode)
                    && Objects.equals(this.participants, that.participants);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(removeLinkedGroups, addressingMode, participants);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsRemoveParticipantsResponse.Success[removeLinkedGroups=" + removeLinkedGroups
                    + ", addressingMode=" + addressingMode
                    + ", participants=" + participants + ']';
        }

        /**
         * The per-participant outcome row produced by the relay for a single removal candidate.
         *
         * <p>The WA Web wire-level shape is a 4-arm disjunction ({@code ParticipantNotInGroup},
         * {@code ParticipantNotAllowed}, {@code ParticipantNotAcceptable},
         * {@code RemoveParticipantsLinkedGroupsServerError}). This class fuses the four arms into a single shape
         * exposing the always-present {@link #jid()} plus an optional {@link RejectionReason} payload that
         * distinguishes the rejected arms via {@link RejectionReason#errorCode()}.
         *
         * @implNote The fusion keeps the response shape stable across all four arms so callers do not need to
         * pattern-match on the disjunction at every call site; the {@code rejectionReason} {@link Optional} is
         * the discriminator.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsParticipantNotInGroupOrParticipantNotAllowedOrParticipantNotAcceptableOrRemoveParticipantsLinkedGroupsServerErrorMixinGroup")
        public static final class RemoveParticipantResult {
            /**
             * The participant {@link Jid}, always present on every outcome row.
             */
            private final Jid jid;

            /**
             * The optional {@code phone_number} attribute echoed by the relay.
             */
            private final String phoneNumber;

            /**
             * The optional {@code username} attribute echoed by the relay.
             */
            private final String username;

            /**
             * The optional rejection-reason payload populated only on the rejected arms.
             */
            private final RejectionReason rejectionReason;

            /**
             * Constructs a {@link RemoveParticipantResult} row.
             *
             * @param jid             the participant {@link Jid}
             * @param phoneNumber     the optional {@code phone_number} echo; may be {@code null}
             * @param username        the optional {@code username} echo; may be {@code null}
             * @param rejectionReason the optional rejection-reason payload; may be {@code null}
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public RemoveParticipantResult(Jid jid, String phoneNumber, String username,
                                           RejectionReason rejectionReason) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.phoneNumber = phoneNumber;
                this.username = username;
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
             * Returns the optional {@code username} echo.
             *
             * @return an {@link Optional} carrying the username, or empty when the relay omitted it
             */
            public Optional<String> username() {
                return Optional.ofNullable(username);
            }

            /**
             * Returns the optional rejection-reason payload.
             *
             * <p>An empty value identifies this row as a successful removal; a present value identifies it as one
             * of the rejected arms and carries the reason code via {@link RejectionReason#errorCode()}.
             *
             * @return an {@link Optional} carrying the rejection payload
             */
            public Optional<RejectionReason> rejectionReason() {
                return Optional.ofNullable(rejectionReason);
            }

            /**
             * Tries to parse an outcome row from a single {@code <participant>} child of the {@code <remove>}
             * payload.
             *
             * <p>The stanza must be a {@code <participant>} carrying a {@code jid} attribute, with an optional
             * {@code error} attribute discriminating the rejected arms.
             *
             * @param stanza the {@code <participant>} child
             * @return an {@link Optional} carrying the parsed row, or empty when the stanza does not match
             */
            @WhatsAppWebExport(moduleName = "WASmaxInGroupsRemoveParticipantsResponseSuccess",
                    exports = "parseRemoveParticipantsResponseSuccessRemoveParticipant",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<RemoveParticipantResult> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("participant")) {
                    return Optional.empty();
                }
                var jid = stanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var phoneNumber = stanza.getAttributeAsString("phone_number").orElse(null);
                var username = stanza.getAttributeAsString("username").orElse(null);
                var rejectionReason = RejectionReason.of(stanza).orElse(null);
                return Optional.of(new RemoveParticipantResult(jid, phoneNumber, username,
                        rejectionReason));
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is a {@link RemoveParticipantResult} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (RemoveParticipantResult) obj;
                return Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.phoneNumber, that.phoneNumber)
                        && Objects.equals(this.username, that.username)
                        && Objects.equals(this.rejectionReason, that.rejectionReason);
            }

            /**
             * Returns a hash composed of every field.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, phoneNumber, username, rejectionReason);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsRemoveParticipantsResponse.Success.RemoveParticipantResult[jid=" + jid
                        + ", phoneNumber=" + phoneNumber
                        + ", username=" + username
                        + ", rejectionReason=" + rejectionReason + ']';
            }

            /**
             * The rejection-reason payload for a candidate the relay refused to remove.
             *
             * <p>{@link #errorCode()} carries the raw error code lifted from the {@code error} attribute; possible
             * values correspond to the four rejected arms ({@code ParticipantNotInGroup},
             * {@code ParticipantNotAllowed}, {@code ParticipantNotAcceptable},
             * {@code RemoveParticipantsLinkedGroupsServerError}).
             */
            public static final class RejectionReason {
                /**
                 * The numeric rejection-error code lifted from the {@code error} attribute.
                 */
                private final int errorCode;

                /**
                 * Constructs a {@link RejectionReason} payload.
                 *
                 * @param errorCode the numeric error code
                 */
                public RejectionReason(int errorCode) {
                    this.errorCode = errorCode;
                }

                /**
                 * Returns the numeric rejection-error code.
                 *
                 * @return the error code
                 */
                public int errorCode() {
                    return errorCode;
                }

                /**
                 * Tries to parse a {@link RejectionReason} payload from a {@code <participant>} child.
                 *
                 * <p>The presence of a non-negative {@code error} attribute discriminates a rejected arm from the
                 * success arm; an empty value signals the success arm.
                 *
                 * @param stanza the {@code <participant>} child
                 * @return an {@link Optional} carrying the parsed payload, or empty when the stanza is on the
                 *         success arm
                 */
                public static Optional<RejectionReason> of(Stanza stanza) {
                    Objects.requireNonNull(stanza, "stanza cannot be null");
                    var error = stanza.getAttributeAsInt("error").orElse(-1);
                    if (error < 0) {
                        return Optional.empty();
                    }
                    return Optional.of(new RejectionReason(error));
                }

                /**
                 * Compares this payload to {@code obj} for value equality on {@link #errorCode()}.
                 *
                 * @param obj the other object
                 * @return {@code true} when {@code obj} is a {@link RejectionReason} with the same code
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
                    return this.errorCode == that.errorCode;
                }

                /**
                 * Returns a hash derived from {@link #errorCode()}.
                 *
                 * @return the hash code
                 */
                @Override
                public int hashCode() {
                    return Objects.hash(errorCode);
                }

                /**
                 * Returns a debug string carrying {@link #errorCode()}.
                 *
                 * @return the debug representation
                 */
                @Override
                public String toString() {
                    return "SmaxGroupsRemoveParticipantsResponse.Success.RemoveParticipantResult.RejectionReason[errorCode="
                            + errorCode + ']';
                }
            }
        }
    }

    /**
     * The reply variant emitted when the relay rejected the request envelope as malformed, unauthorised, or
     * referencing a non-existent group.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsRemoveParticipantsResponseClientError")
    final class ClientError implements SmaxGroupsRemoveParticipantsResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay.
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
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} which validates the shared
         * {@code <iq type="error"><error code="..." text="..."/></iq>} envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsRemoveParticipantsResponseClientError",
                exports = "parseRemoveParticipantsResponseClientError",
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
            return "SmaxGroupsRemoveParticipantsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The reply variant emitted on transient relay-side failure.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsRemoveParticipantsResponseServerError")
    final class ServerError implements SmaxGroupsRemoveParticipantsResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay.
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
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} which validates the shared
         * {@code <iq type="error"><error code="..." text="..."/></iq>} envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsRemoveParticipantsResponseServerError",
                exports = "parseRemoveParticipantsResponseServerError",
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
            return "SmaxGroupsRemoveParticipantsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The sealed reply family for a {@link SmaxGroupsMembershipRequestsActionRequest}.
 *
 * <p>The three variants split the relay's response into distinct cases: {@link Success} carries the per-participant
 * approval and rejection outcomes that feed back into the admin "Pending requests" UI, and
 * {@link ClientError}/{@link ServerError} surface the relay's reason codes.
 */
public sealed interface SmaxGroupsMembershipRequestsActionResponse extends SmaxStanza.Response
        permits SmaxGroupsMembershipRequestsActionResponse.Success, SmaxGroupsMembershipRequestsActionResponse.ClientError, SmaxGroupsMembershipRequestsActionResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsMembershipRequestsActionResponse} variant in priority
     * order and returns the first that parses cleanly.
     *
     * <p>Tries {@link Success} first, then {@link ClientError}, then {@link ServerError}.
     *
     * @implNote The empty {@link Optional} surfaces when the stanza shape matches none of the three documented
     * variants; WA Web throws {@code SmaxParsingFailure} on the same path, but Cobalt defers the decision to the
     * caller so it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound request
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsMembershipRequestsActionRPC",
            exports = "sendMembershipRequestsActionRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsMembershipRequestsActionResponse> of(Stanza stanza, Stanza request) {
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
     * The reply variant emitted when the relay processed the action and returned per-participant outcomes split into
     * approved and rejected lists.
     *
     * <p>The admin UI iterates {@link #approveParticipants()} and {@link #rejectParticipants()} and folds each entry
     * into the per-participant record it displays.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsMembershipRequestsActionResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupAddressingModeMixin")
    final class Success implements SmaxGroupsMembershipRequestsActionResponse {
        /**
         * The optional addressing-mode echo on the IQ envelope.
         */
        private final String addressingMode;

        /**
         * The per-participant approval outcomes.
         */
        private final List<ApproveParticipantResult> approveParticipants;

        /**
         * The per-participant rejection outcomes.
         */
        private final List<RejectParticipantResult> rejectParticipants;

        /**
         * Constructs a {@link Success} reply.
         *
         * <p>Either outcome list may be empty when the matching {@code <approve>}/{@code <reject>} container is
         * absent on the envelope; {@code null} normalises to {@link List#of()}.
         *
         * @param addressingMode      the optional addressing-mode echo; may be {@code null}
         * @param approveParticipants the per-participant approval outcomes; may be {@code null}
         * @param rejectParticipants  the per-participant rejection outcomes; may be {@code null}
         */
        public Success(String addressingMode,
                       List<ApproveParticipantResult> approveParticipants,
                       List<RejectParticipantResult> rejectParticipants) {
            this.addressingMode = addressingMode;
            this.approveParticipants = List.copyOf(
                    Objects.requireNonNullElse(approveParticipants, List.of()));
            this.rejectParticipants = List.copyOf(
                    Objects.requireNonNullElse(rejectParticipants, List.of()));
        }

        /**
         * Returns the optional addressing-mode echo.
         *
         * @return an {@link Optional} carrying the addressing-mode token, or empty when the relay omitted it
         */
        public Optional<String> addressingMode() {
            return Optional.ofNullable(addressingMode);
        }

        /**
         * Returns the per-participant approval outcomes.
         *
         * @return an unmodifiable list of {@link ApproveParticipantResult}; never {@code null}
         */
        public List<ApproveParticipantResult> approveParticipants() {
            return approveParticipants;
        }

        /**
         * Returns the per-participant rejection outcomes.
         *
         * @return an unmodifiable list of {@link RejectParticipantResult}; never {@code null}
         */
        public List<RejectParticipantResult> rejectParticipants() {
            return rejectParticipants;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * <p>Matches when the IQ is a {@code type="result"} envelope echoing the request's {@code id} and
         * {@code to} attributes and carrying a {@code <membership_requests_action>} child.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         * @throws NullPointerException if either argument is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsMembershipRequestsActionResponseSuccess",
                exports = "parseMembershipRequestsActionResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "result")) {
                return Optional.empty();
            }
            var requestId = request.getAttributeAsString("id").orElse(null);
            if (requestId == null || !stanza.hasAttribute("id", requestId)) {
                return Optional.empty();
            }
            var requestTo = request.getAttributeAsString("to").orElse(null);
            if (requestTo != null && !stanza.hasAttribute("from", requestTo)) {
                return Optional.empty();
            }
            var action = stanza.getChild("membership_requests_action").orElse(null);
            if (action == null) {
                return Optional.empty();
            }
            var addressingMode = stanza.getAttributeAsString("addressing_mode").orElse(null);
            var approveContainer = action.getChild("approve").orElse(null);
            var approveParticipants = new ArrayList<ApproveParticipantResult>();
            if (approveContainer != null) {
                for (var participantNode : approveContainer.getChildren("participant")) {
                    var participant = ApproveParticipantResult.of(participantNode).orElse(null);
                    if (participant == null) {
                        return Optional.empty();
                    }
                    approveParticipants.add(participant);
                }
            }
            var rejectContainer = action.getChild("reject").orElse(null);
            var rejectParticipants = new ArrayList<RejectParticipantResult>();
            if (rejectContainer != null) {
                for (var participantNode : rejectContainer.getChildren("participant")) {
                    var participant = RejectParticipantResult.of(participantNode).orElse(null);
                    if (participant == null) {
                        return Optional.empty();
                    }
                    rejectParticipants.add(participant);
                }
            }
            return Optional.of(new Success(addressingMode, approveParticipants, rejectParticipants));
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
            return Objects.equals(this.addressingMode, that.addressingMode)
                    && Objects.equals(this.approveParticipants, that.approveParticipants)
                    && Objects.equals(this.rejectParticipants, that.rejectParticipants);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(addressingMode, approveParticipants, rejectParticipants);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsMembershipRequestsActionResponse.Success[addressingMode=" + addressingMode
                    + ", approveParticipants=" + approveParticipants
                    + ", rejectParticipants=" + rejectParticipants + ']';
        }

        /**
         * Per-participant approval outcome.
         *
         * <p>Carries the approved JID and a flag indicating whether the relay supplied the optional
         * {@code <identity/>} mixin payload (used to recover the participant's username and phone number).
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsIdentityMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsMembershipRequestsActionAcceptParticipantMixins")
        public static final class ApproveParticipantResult {
            /**
             * The approved participant {@link Jid}.
             */
            private final Jid jid;

            /**
             * Whether the relay supplied an {@code <identity/>} payload for this participant.
             */
            private final boolean hasIdentityPayload;

            /**
             * Constructs an approval-outcome entry.
             *
             * @param jid                the participant {@link Jid}; never {@code null}
             * @param hasIdentityPayload whether the {@code <identity/>} mixin was present
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public ApproveParticipantResult(Jid jid, boolean hasIdentityPayload) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.hasIdentityPayload = hasIdentityPayload;
            }

            /**
             * Returns the approved participant {@link Jid}.
             *
             * @return the participant JID; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns whether the entry carried an {@code <identity/>} payload.
             *
             * @return {@code true} when the mixin was present
             */
            public boolean hasIdentityPayload() {
                return hasIdentityPayload;
            }

            /**
             * Tries to parse an {@link ApproveParticipantResult} from the given {@code <participant/>} child.
             *
             * <p>Matches when the child carries the {@code jid} attribute; the {@code <identity/>} grandchild is
             * optional.
             *
             * @param stanza the {@code <participant/>} child
             * @return an {@link Optional} carrying the parsed outcome, or empty when the child does not match
             */
            @WhatsAppWebExport(moduleName = "WASmaxInGroupsMembershipRequestsActionResponseSuccess",
                    exports = "parseMembershipRequestsActionResponseSuccessMembershipRequestsActionApproveParticipant",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<ApproveParticipantResult> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("participant")) {
                    return Optional.empty();
                }
                var jid = stanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var hasIdentity = stanza.getChild("identity").isPresent();
                return Optional.of(new ApproveParticipantResult(jid, hasIdentity));
            }

            /**
             * Compares this outcome to {@code obj} for value equality across both fields.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is an {@link ApproveParticipantResult} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (ApproveParticipantResult) obj;
                return this.hasIdentityPayload == that.hasIdentityPayload
                        && Objects.equals(this.jid, that.jid);
            }

            /**
             * Returns a hash composed of both fields.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, hasIdentityPayload);
            }

            /**
             * Returns a debug string carrying both fields.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsMembershipRequestsActionResponse.Success.ApproveParticipantResult[jid=" + jid
                        + ", hasIdentityPayload=" + hasIdentityPayload + ']';
            }
        }

        /**
         * Per-participant rejection outcome.
         *
         * <p>Carries the rejected JID and a flag indicating whether the relay supplied the optional
         * {@code <identity/>} mixin payload; the field shape mirrors {@link ApproveParticipantResult} so admin UIs
         * can use one rendering path for both outcomes.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsMembershipRequestsActionRejectParticipantMixins")
        public static final class RejectParticipantResult {
            /**
             * The rejected participant {@link Jid}.
             */
            private final Jid jid;

            /**
             * Whether the relay supplied an {@code <identity/>} payload for this participant.
             */
            private final boolean hasIdentityPayload;

            /**
             * Constructs a rejection-outcome entry.
             *
             * @param jid                the participant {@link Jid}; never {@code null}
             * @param hasIdentityPayload whether the {@code <identity/>} mixin was present
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public RejectParticipantResult(Jid jid, boolean hasIdentityPayload) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.hasIdentityPayload = hasIdentityPayload;
            }

            /**
             * Returns the rejected participant {@link Jid}.
             *
             * @return the participant JID; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns whether the entry carried an {@code <identity/>} payload.
             *
             * @return {@code true} when the mixin was present
             */
            public boolean hasIdentityPayload() {
                return hasIdentityPayload;
            }

            /**
             * Tries to parse a {@link RejectParticipantResult} from the given {@code <participant/>} child.
             *
             * <p>Matches when the child carries the {@code jid} attribute; the {@code <identity/>} grandchild is
             * optional.
             *
             * @param stanza the {@code <participant/>} child
             * @return an {@link Optional} carrying the parsed outcome, or empty when the child does not match
             */
            @WhatsAppWebExport(moduleName = "WASmaxInGroupsMembershipRequestsActionResponseSuccess",
                    exports = "parseMembershipRequestsActionResponseSuccessMembershipRequestsActionRejectParticipant",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<RejectParticipantResult> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("participant")) {
                    return Optional.empty();
                }
                var jid = stanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var hasIdentity = stanza.getChild("identity").isPresent();
                return Optional.of(new RejectParticipantResult(jid, hasIdentity));
            }

            /**
             * Compares this outcome to {@code obj} for value equality across both fields.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is a {@link RejectParticipantResult} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (RejectParticipantResult) obj;
                return this.hasIdentityPayload == that.hasIdentityPayload
                        && Objects.equals(this.jid, that.jid);
            }

            /**
             * Returns a hash composed of both fields.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, hasIdentityPayload);
            }

            /**
             * Returns a debug string carrying both fields.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsMembershipRequestsActionResponse.Success.RejectParticipantResult[jid=" + jid
                        + ", hasIdentityPayload=" + hasIdentityPayload + ']';
            }
        }
    }

    /**
     * The reply variant emitted when the relay rejected the approve/reject action as malformed or unauthorised.
     *
     * <p>WA Web logs the {@link #errorCode()} as the HTTP-style status passed back to the admin "Pending requests"
     * UI.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsMembershipRequestsActionResponseClientError")
    final class ClientError implements SmaxGroupsMembershipRequestsActionResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsMembershipRequestsActionResponseClientError",
                exports = "parseMembershipRequestsActionResponseClientError",
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
            return "SmaxGroupsMembershipRequestsActionResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The reply variant emitted on transient relay-side failure.
     *
     * <p>Logged at the same severity as {@link ClientError} but typically signals retry-eligible relay outages
     * rather than caller error.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsMembershipRequestsActionResponseServerError")
    final class ServerError implements SmaxGroupsMembershipRequestsActionResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsMembershipRequestsActionResponseServerError",
                exports = "parseMembershipRequestsActionResponseServerError",
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
            return "SmaxGroupsMembershipRequestsActionResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

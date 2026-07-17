package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Objects;
import java.util.Optional;

/**
 * Sealed reply family for a {@link SmaxGroupsAcceptGroupAddRequest}.
 *
 * The four variants partition every reply the relay can return to an accept-group-add request.
 * {@link GroupJoinRequestSuccess} means the relay accepted the {@code accept} but the group's membership-approval
 * mode rerouted the caller into the pending-approval queue; {@link Success} means the caller has joined the group
 * directly; {@link ClientError} and {@link ServerError} surface the relay's reason codes. Callers obtain the right
 * variant by passing the inbound IQ to {@link #of(Stanza, Stanza)}.
 */
public sealed interface SmaxGroupsAcceptGroupAddResponse extends SmaxStanza.Response
        permits SmaxGroupsAcceptGroupAddResponse.GroupJoinRequestSuccess, SmaxGroupsAcceptGroupAddResponse.Success,
        SmaxGroupsAcceptGroupAddResponse.ClientError, SmaxGroupsAcceptGroupAddResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsAcceptGroupAddResponse} variant in priority order and
     * returns the first that parses cleanly.
     *
     * {@link GroupJoinRequestSuccess} is probed first because its {@code <membership_approval_request/>} child
     * discriminates it from the bare {@link Success}; the two error variants are probed last.
     *
     * @implNote This implementation returns an empty {@link Optional} when the stanza shape matches none of the
     * four documented variants; WA Web throws a parsing failure on the same path, but Cobalt defers the decision
     * to the caller so it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsAcceptGroupAddRequest} stanza, used to validate
     *                echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsAcceptGroupAddRPC",
            exports = "sendAcceptGroupAddRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsAcceptGroupAddResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var groupJoin = GroupJoinRequestSuccess.of(stanza, request);
        if (groupJoin.isPresent()) {
            return groupJoin;
        }
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
     * Reply variant emitted when the relay accepted the {@code accept} but rerouted the caller into the
     * pending-approval queue.
     *
     * The caller is not yet a participant; the relay records the request and a group admin must approve it before
     * the caller joins.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsAcceptGroupAddResponseGroupJoinRequestSuccess")
    final class GroupJoinRequestSuccess implements SmaxGroupsAcceptGroupAddResponse {
        /**
         * Constructs a marker {@link GroupJoinRequestSuccess}.
         *
         * The instance carries no payload; the discriminator is solely the presence of the
         * {@code <membership_approval_request/>} child on the IQ.
         */
        public GroupJoinRequestSuccess() {
        }

        /**
         * Tries to parse a {@link GroupJoinRequestSuccess} variant from {@code stanza}.
         *
         * The IQ must be a valid {@code type="result"} echo of the request and must carry a
         * {@code <membership_approval_request/>} child.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsAcceptGroupAddResponseGroupJoinRequestSuccess",
                exports = "parseAcceptGroupAddResponseGroupJoinRequestSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<GroupJoinRequestSuccess> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            if (stanza.getChild("membership_approval_request").isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new GroupJoinRequestSuccess());
        }

        /**
         * Compares this marker to {@code obj} for value equality.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link GroupJoinRequestSuccess}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a constant hash shared by every {@link GroupJoinRequestSuccess} instance.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return GroupJoinRequestSuccess.class.hashCode();
        }

        /**
         * Returns the marker's debug representation.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsAcceptGroupAddResponse.GroupJoinRequestSuccess[]";
        }
    }

    /**
     * Reply variant emitted when the relay admitted the caller into the group as a regular participant.
     *
     * The caller has joined directly; no admin approval is pending.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsAcceptGroupAddResponseSuccess")
    final class Success implements SmaxGroupsAcceptGroupAddResponse {
        /**
         * Constructs a marker {@link Success}.
         *
         * The instance carries no payload; the discriminator is the absence of the
         * {@code <membership_approval_request/>} child.
         */
        public Success() {
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * The IQ must be a valid {@code type="result"} echo of the request and must not carry a
         * {@code <membership_approval_request/>} child, otherwise the {@link GroupJoinRequestSuccess} branch wins.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsAcceptGroupAddResponseSuccess",
                exports = "parseAcceptGroupAddResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            if (stanza.getChild("membership_approval_request").isPresent()) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Compares this marker to {@code obj} for value equality.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Success}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a constant hash shared by every {@link Success} instance.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * Returns the marker's debug representation.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsAcceptGroupAddResponse.Success[]";
        }
    }

    /**
     * Reply variant emitted when the relay rejected the {@code accept} as malformed, expired, or referencing a
     * non-existent pending request.
     *
     * The {@link #errorCode()} carries the HTTP-style status the relay reports back to the join-via-invite flow.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsAcceptGroupAddResponseClientError")
    final class ClientError implements SmaxGroupsAcceptGroupAddResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsAcceptGroupAddResponseClientError",
                exports = "parseAcceptGroupAddResponseClientError",
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
            return "SmaxGroupsAcceptGroupAddResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Reply variant emitted on transient relay-side failure.
     *
     * Unlike {@link ClientError}, this variant typically signals a retry-eligible relay outage rather than caller
     * error.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsAcceptGroupAddResponseServerError")
    final class ServerError implements SmaxGroupsAcceptGroupAddResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsAcceptGroupAddResponseServerError",
                exports = "parseAcceptGroupAddResponseServerError",
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
            return "SmaxGroupsAcceptGroupAddResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

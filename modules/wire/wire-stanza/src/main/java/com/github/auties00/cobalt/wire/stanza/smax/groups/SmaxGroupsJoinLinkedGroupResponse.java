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
 * The sealed reply family for a {@link SmaxGroupsJoinLinkedGroupRequest}.
 *
 * <p>The four variants split the relay's response into distinct cases: {@link GroupJoinRequestSuccess} means the
 * relay accepted the join but the sub-group's membership-approval mode rerouted the caller into the
 * pending-approval queue, {@link Success} means the caller has joined the sub-group directly, and
 * {@link ClientError}/{@link ServerError} surface the relay's reason codes.
 */
public sealed interface SmaxGroupsJoinLinkedGroupResponse extends SmaxStanza.Response
        permits SmaxGroupsJoinLinkedGroupResponse.GroupJoinRequestSuccess, SmaxGroupsJoinLinkedGroupResponse.Success,
                SmaxGroupsJoinLinkedGroupResponse.ClientError, SmaxGroupsJoinLinkedGroupResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsJoinLinkedGroupResponse} variant in priority order and
     * returns the first that parses cleanly.
     *
     * <p>{@link GroupJoinRequestSuccess} is tried before {@link Success} because both share the same envelope
     * shape; the {@code <membership_approval_request/>} child is the only discriminator. {@link ClientError} and
     * {@link ServerError} are tried last.
     *
     * @implNote The empty {@link Optional} surfaces when the stanza shape matches none of the four documented
     * variants; WA Web throws {@code SmaxParsingFailure} on the same path, but Cobalt defers the decision to the
     * caller so it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsJoinLinkedGroupRequest} stanza, used to validate echoed ids
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsJoinLinkedGroupRPC",
            exports = "sendJoinLinkedGroupRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsJoinLinkedGroupResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var groupJoinRequestSuccess = GroupJoinRequestSuccess.of(stanza, request);
        if (groupJoinRequestSuccess.isPresent()) {
            return groupJoinRequestSuccess;
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
     * The reply variant emitted when the relay accepted the join but the sub-group's membership-approval mode
     * rerouted the caller into the pending-approval queue.
     *
     * <p>The caller is not yet a participant but the relay has recorded the request, and a sub-group admin must
     * approve it via {@link SmaxGroupsMembershipRequestsActionRequest}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsJoinLinkedGroupResponseGroupJoinRequestSuccess")
    final class GroupJoinRequestSuccess implements SmaxGroupsJoinLinkedGroupResponse {
        /**
         * Constructs a marker {@link GroupJoinRequestSuccess}.
         *
         * <p>The instance carries no payload; the discriminator is solely the presence of the
         * {@code <membership_approval_request/>} child on the IQ.
         */
        public GroupJoinRequestSuccess() {
        }

        /**
         * Tries to parse a {@link GroupJoinRequestSuccess} variant from {@code stanza}.
         *
         * <p>Matches when the IQ is a valid {@code type="result"} echo of the request and carries a
         * {@code <membership_approval_request/>} child.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsJoinLinkedGroupResponseGroupJoinRequestSuccess",
                exports = "parseJoinLinkedGroupResponseGroupJoinRequestSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<GroupJoinRequestSuccess> of(Stanza stanza, Stanza request) {
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (stanza.getChild("membership_approval_request").isEmpty()) {
                return Optional.empty();
            }
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
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
            return "SmaxGroupsJoinLinkedGroupResponse.GroupJoinRequestSuccess[]";
        }
    }

    /**
     * The reply variant emitted when the relay admitted the caller into the sub-group directly without going through
     * membership approval.
     *
     * <p>The local chat row materialises immediately and the community sub-group preview UI flips to the joined
     * state.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsJoinLinkedGroupResponseSuccess")
    final class Success implements SmaxGroupsJoinLinkedGroupResponse {
        /**
         * Constructs a marker {@link Success}.
         *
         * <p>The instance carries no payload; the discriminator is the absence of the
         * {@code <membership_approval_request/>} child.
         */
        public Success() {
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * <p>Matches when the IQ is a valid {@code type="result"} echo of the request; the
         * {@link GroupJoinRequestSuccess} branch is tried first to claim envelopes carrying the
         * {@code <membership_approval_request/>} marker.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsJoinLinkedGroupResponseSuccess",
                exports = "parseJoinLinkedGroupResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
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
            return "SmaxGroupsJoinLinkedGroupResponse.Success[]";
        }
    }

    /**
     * The reply variant emitted when the relay rejected the join as malformed, unauthorised, or referencing a
     * non-existent or unjoinable sub-group.
     *
     * <p>WA Web logs the {@link #errorCode()} as the HTTP-style status passed back to the community sub-group
     * preview UI.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsJoinLinkedGroupResponseClientError")
    final class ClientError implements SmaxGroupsJoinLinkedGroupResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsJoinLinkedGroupResponseClientError",
                exports = "parseJoinLinkedGroupResponseClientError",
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
            return "SmaxGroupsJoinLinkedGroupResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The reply variant emitted on transient relay-side failure.
     *
     * <p>Logged at the same severity as {@link ClientError} but typically signals retry-eligible relay outages
     * rather than caller error.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsJoinLinkedGroupResponseServerError")
    final class ServerError implements SmaxGroupsJoinLinkedGroupResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsJoinLinkedGroupResponseServerError",
                exports = "parseJoinLinkedGroupResponseServerError",
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
            return "SmaxGroupsJoinLinkedGroupResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

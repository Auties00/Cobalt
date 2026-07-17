package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Sealed reply family for a {@link SmaxGroupsBatchGetGroupInfoRequest}.
 *
 * The three variants partition every reply the relay can return: {@link Success}, {@link ClientError} and
 * {@link ServerError}. {@link Success} wraps a heterogeneous {@code <groups>} list whose children are each either
 * a full group-info, a truncated group-info, a group-forbidden marker, or a group-not-exist marker; callers
 * dispatch on each child's shape to populate the chat database accordingly.
 */
public sealed interface SmaxGroupsBatchGetGroupInfoResponse extends SmaxStanza.Response
        permits SmaxGroupsBatchGetGroupInfoResponse.Success, SmaxGroupsBatchGetGroupInfoResponse.ClientError, SmaxGroupsBatchGetGroupInfoResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsBatchGetGroupInfoResponse} variant in priority order
     * and returns the first that parses cleanly.
     *
     * {@link Success} is probed first, then {@link ClientError}, then {@link ServerError}.
     *
     * @implNote This implementation returns an empty {@link Optional} when the stanza shape matches none of the
     * documented variants; WA Web throws a parsing failure on the same path, but Cobalt defers the decision to the
     * caller so it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsBatchGetGroupInfoRequest} stanza, used to validate
     *                echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsBatchGetGroupInfoRPC",
            exports = "sendBatchGetGroupInfoRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsBatchGetGroupInfoResponse> of(Stanza stanza, Stanza request) {
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
     * Reply variant carrying a {@code <groups>} wrapper with one {@code <group/>} child per requested group.
     *
     * Each {@code <group/>} child takes one of four sub-shapes (full group-info, truncated group-info,
     * group-forbidden, group-not-exist); callers dispatch on the child's structure via standard {@link Stanza}
     * accessors.
     *
     * @implNote This implementation exposes the wrapper as an unmodifiable {@link List} of raw {@link Stanza}s rather
     * than projecting the four sub-shapes into a typed alternation, mirroring the shape exposed by
     * {@link SmaxGroupsGetParticipatingGroupsResponse.Success}; callers that need to branch on the shape inspect
     * each {@code <group/>} child's description and attributes directly.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsBatchGetGroupInfoResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupInfoOrTruncatedGroupInfoOrGroupForbiddenOrGroupNotExistMixinGroup")
    final class Success implements SmaxGroupsBatchGetGroupInfoResponse {
        /**
         * Holds the {@code <group/>} children carried by the {@code <groups>} wrapper.
         */
        private final List<Stanza> groups;

        /**
         * Constructs a {@link Success}.
         *
         * The supplied list is defensively copied.
         *
         * @param groups the per-group reply nodes
         * @throws NullPointerException if {@code groups} is {@code null}
         */
        public Success(List<Stanza> groups) {
            Objects.requireNonNull(groups, "groups cannot be null");
            this.groups = List.copyOf(groups);
        }

        /**
         * Returns the per-group reply nodes.
         *
         * @return an unmodifiable list of {@code <group/>} {@link Stanza}s; never {@code null}
         */
        public List<Stanza> groups() {
            return groups;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * The IQ must be a valid {@code type="result"} echo of the request and must carry a non-empty
         * {@code <groups>} wrapper.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsBatchGetGroupInfoResponseSuccess",
                exports = "parseBatchGetGroupInfoResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var groupsWrapper = stanza.getChild("groups").orElse(null);
            if (groupsWrapper == null) {
                return Optional.empty();
            }
            var groups = groupsWrapper.streamChildren("group").toList();
            if (groups.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Success(groups));
        }

        /**
         * Compares this success to {@code obj} for value equality on {@link #groups()}.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Success} with the same group list
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
            return Objects.equals(this.groups, that.groups);
        }

        /**
         * Returns a hash derived from {@link #groups()}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(groups);
        }

        /**
         * Returns a debug string carrying {@link #groups()}.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsBatchGetGroupInfoResponse.Success[groups=" + groups + ']';
        }
    }

    /**
     * Reply variant emitted when the relay rejected the request as malformed or unauthorised.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsBatchGetGroupInfoResponseClientError")
    final class ClientError implements SmaxGroupsBatchGetGroupInfoResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsBatchGetGroupInfoResponseClientError",
                exports = "parseBatchGetGroupInfoResponseClientError",
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
            return "SmaxGroupsBatchGetGroupInfoResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Reply variant emitted on transient relay-side failure.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsBatchGetGroupInfoResponseServerError")
    final class ServerError implements SmaxGroupsBatchGetGroupInfoResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsBatchGetGroupInfoResponseServerError",
                exports = "parseBatchGetGroupInfoResponseServerError",
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
            return "SmaxGroupsBatchGetGroupInfoResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The sealed reply family for a {@link SmaxGroupsGetParticipatingGroupsRequest}.
 * <p>
 * Exactly one of three variants matches a given inbound stanza: {@link Success} carries every group the caller
 * participates in as raw {@code <group/>} subtrees, {@link ClientError} carries a caller-side rejection code, and
 * {@link ServerError} carries a transient relay-side failure code.
 */
public sealed interface SmaxGroupsGetParticipatingGroupsResponse extends SmaxStanza.Response
        permits SmaxGroupsGetParticipatingGroupsResponse.Success, SmaxGroupsGetParticipatingGroupsResponse.ClientError, SmaxGroupsGetParticipatingGroupsResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsGetParticipatingGroupsResponse} variant and returns the
     * first that parses cleanly.
     * <p>
     * Variants are tried in priority order: {@link Success} first, then {@link ClientError}, then {@link ServerError}.
     * The result is empty when the stanza matches none of the three variants.
     *
     * @implNote This implementation defers the no-match decision to the caller by returning an empty {@link Optional}
     * rather than throwing, so the caller can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound request
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsGetParticipatingGroupsRPC",
            exports = "sendGetParticipatingGroupsRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsGetParticipatingGroupsResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza);
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
     * The reply variant emitted when the relay returned the caller's participating-group set.
     * <p>
     * {@link #groups()} holds one raw {@code <group/>} subtree per group the caller belongs to.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetParticipatingGroupsResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupInfoOrTruncatedGroupInfoGroupInfoMixinGroup")
    final class Success implements SmaxGroupsGetParticipatingGroupsResponse {
        /**
         * The raw {@code <group/>} children carried by the {@code <groups/>} wrapper.
         */
        private final List<Stanza> groups;

        /**
         * Constructs a {@link Success} reply.
         * <p>
         * A {@code null} argument normalises to {@link List#of()} so callers can construct an empty result directly.
         *
         * @param groups the {@code <group/>} sub-nodes; may be {@code null}
         */
        public Success(List<Stanza> groups) {
            this.groups = List.copyOf(Objects.requireNonNullElse(groups, List.of()));
        }

        /**
         * Returns the raw {@code <group/>} sub-nodes.
         * <p>
         * The subtrees are exposed verbatim so callers project subject, picture, owner, admin list, ephemeral
         * expiration, and addressing mode directly without committing to the full mixin schema.
         *
         * @return an unmodifiable list of {@code <group/>} nodes; never {@code null}
         */
        public List<Stanza> groups() {
            return groups;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         * <p>
         * Parsing succeeds when the IQ is a {@code type="result"} envelope carrying a {@code <groups>} wrapper; the
         * wrapper may be empty when the caller participates in no groups.
         *
         * @param stanza the inbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetParticipatingGroupsResponseSuccess",
                exports = "parseGetParticipatingGroupsResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza) {
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "result")) {
                return Optional.empty();
            }
            var groupsWrapper = stanza.getChild("groups").orElse(null);
            if (groupsWrapper == null) {
                return Optional.empty();
            }
            var groups = groupsWrapper.streamChildren("group").toList();
            return Optional.of(new Success(groups));
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
            return Objects.equals(this.groups, that.groups);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(groups);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetParticipatingGroupsResponse.Success[groups=" + groups + ']';
        }
    }

    /**
     * The reply variant emitted when the relay rejected the bulk query as malformed or unauthorised.
     * <p>
     * The {@link #errorCode()} carries the HTTP-style status assigned by the relay and {@link #errorText()} carries
     * the optional human-readable reason.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetParticipatingGroupsResponseClientError")
    final class ClientError implements SmaxGroupsGetParticipatingGroupsResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay; {@code null} when omitted.
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
         * <p>
         * The shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope is validated through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, which matches only client-range codes.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetParticipatingGroupsResponseClientError",
                exports = "parseGetParticipatingGroupsResponseClientError",
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
            return "SmaxGroupsGetParticipatingGroupsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The reply variant emitted on transient relay-side failure.
     * <p>
     * Unlike {@link ClientError} this code typically signals a retry-eligible relay outage rather than a malformed or
     * unauthorised request; {@link #errorCode()} carries the server-range status and {@link #errorText()} the optional
     * reason.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetParticipatingGroupsResponseServerError")
    final class ServerError implements SmaxGroupsGetParticipatingGroupsResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay; {@code null} when omitted.
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
         * <p>
         * The shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope is validated through
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, which matches only server-range codes.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetParticipatingGroupsResponseServerError",
                exports = "parseGetParticipatingGroupsResponseServerError",
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
            return "SmaxGroupsGetParticipatingGroupsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

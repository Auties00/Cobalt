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
 * Models the inbound reply to a {@link SmaxGroupsDeleteParentGroupRequest} as a sealed variant family.
 *
 * <p>{@link Success} confirms the community was torn down, {@link ClientError} surfaces caller-side failures
 * (permissions, malformed envelope), and {@link ServerError} surfaces relay-side transient failures that may be
 * retried. Callers pattern-match the variant returned by {@link #of(Stanza, Stanza)}.
 */
public sealed interface SmaxGroupsDeleteParentGroupResponse extends SmaxStanza.Response
        permits SmaxGroupsDeleteParentGroupResponse.Success, SmaxGroupsDeleteParentGroupResponse.ClientError, SmaxGroupsDeleteParentGroupResponse.ServerError {

    /**
     * Parses the inbound IQ stanza into the first matching variant.
     *
     * <p>The probes run in priority order: {@link Success}, {@link ClientError}, then {@link ServerError}. An
     * empty {@link Optional} signals a stanza shape outside the documented union.
     *
     * @implNote
     * This implementation does not throw a parsing-failure exception, leaving the recovery decision to the
     * caller.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound {@link SmaxGroupsDeleteParentGroupRequest} stanza, used to validate
     *                the echoed {@code id} attribute; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented
     *         variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsDeleteParentGroupRPC",
            exports = "sendDeleteParentGroupRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsDeleteParentGroupResponse> of(Stanza stanza, Stanza request) {
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
     * Reports that the relay tore down the targeted community.
     *
     * <p>Carries no payload; the absence of an error envelope together with a matching
     * {@code <iq type="result">} shell is the only signal callers receive that the community and its
     * sub-groups have been deactivated.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsDeleteParentGroupResponseSuccess")
    final class Success implements SmaxGroupsDeleteParentGroupResponse {
        /**
         * Constructs a success variant.
         *
         * <p>Production instances are typically created by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
         */
        public Success() {
        }

        /**
         * Parses the inbound stanza into a {@link Success} when the result envelope is well-formed.
         *
         * <p>Runs as the first probe in the variant cascade of
         * {@link SmaxGroupsDeleteParentGroupResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation delegates the envelope shell check (description, {@code type="result"}, echoed
         * {@code id}) to {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} and accepts any payload shape;
         * the relay does not include child elements on success.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         envelope does not satisfy the result-shell contract
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsDeleteParentGroupResponseSuccess",
                exports = "parseDeleteParentGroupResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Compares this variant to {@code obj} for type equality.
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
         * Returns a constant hash for the payload-free variant.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * Returns a debug string for the payload-free variant.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsDeleteParentGroupResponse.Success[]";
        }
    }

    /**
     * Reports that the relay rejected the request as malformed, unauthorised, or targeting a non-community
     * group.
     *
     * <p>Carries the numeric {@link #errorCode()} and optional {@link #errorText()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsDeleteParentGroupResponseClientError")
    final class ClientError implements SmaxGroupsDeleteParentGroupResponse {
        /**
         * Holds the numeric server-side error code, mirroring the {@code <error code="...">} attribute on the
         * inbound stanza.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay; {@code null} when the relay omitted the
         * {@code <error text="...">} attribute.
         */
        private final String errorText;

        /**
         * Constructs a client-error variant.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ClientError} envelope.
         *
         * <p>Runs as the second probe in the variant cascade of
         * {@link SmaxGroupsDeleteParentGroupResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation delegates the error-envelope extraction to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} so every SMAX response in the family
         * shares the same client-error parsing.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         envelope does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsDeleteParentGroupResponseClientError",
                exports = "parseDeleteParentGroupResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to {@code obj} for value equality across both fields.
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
            return "SmaxGroupsDeleteParentGroupResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Reports that the relay encountered a transient internal failure.
     *
     * <p>Callers decide whether to retry based on the surfaced {@link #errorCode()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsDeleteParentGroupResponseServerError")
    final class ServerError implements SmaxGroupsDeleteParentGroupResponse {
        /**
         * Holds the numeric server-side error code, mirroring the {@code <error code="...">} attribute on the
         * inbound stanza.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay; {@code null} when the relay omitted the
         * {@code <error text="...">} attribute.
         */
        private final String errorText;

        /**
         * Constructs a server-error variant.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ServerError} envelope.
         *
         * <p>Runs as the terminal probe in the variant cascade of
         * {@link SmaxGroupsDeleteParentGroupResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation delegates the error-envelope extraction to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} so every SMAX response in the family
         * shares the same server-error parsing.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         envelope does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsDeleteParentGroupResponseServerError",
                exports = "parseDeleteParentGroupResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to {@code obj} for value equality across both fields.
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
            return "SmaxGroupsDeleteParentGroupResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

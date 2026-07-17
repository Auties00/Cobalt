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
 * Models the inbound reply to a {@link SmaxGroupsGetGroupInfoRequest} as a sealed variant family.
 *
 * <p>{@link Success} carries the group-info projection; {@link ClientError} and {@link ServerError} surface
 * caller-side and relay-side failures. Callers pattern-match the variant returned by {@link #of(Stanza, Stanza)}.
 */
public sealed interface SmaxGroupsGetGroupInfoResponse extends SmaxStanza.Response
        permits SmaxGroupsGetGroupInfoResponse.Success, SmaxGroupsGetGroupInfoResponse.ClientError, SmaxGroupsGetGroupInfoResponse.ServerError {

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
     * @param request the original outbound {@link SmaxGroupsGetGroupInfoRequest} stanza, used to validate the
     *                echoed {@code id} attribute; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented
     *         variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsGetGroupInfoRPC",
            exports = "sendGetGroupInfoRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsGetGroupInfoResponse> of(Stanza stanza, Stanza request) {
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
     * Reports that the relay echoed the {@code <group/>} subtree carrying the group-info projection.
     *
     * <p>Carries the top-level participant {@code size} attribute and the verbatim {@code <group/>} sub-stanza so
     * callers can drive their own projection (subject, picture, owner, admin list, ephemeral expiration, and so
     * on) by reading child attributes via {@link Stanza#getAttributeAsString(String)} and
     * {@link Stanza#streamChildren(String)}.
     *
     * @implNote
     * This implementation exposes the raw {@code <group/>} child rather than projecting every field because the
     * relay's group-info surface is wide and evolves with server-side feature flags; callers read only the
     * fields they need.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetGroupInfoResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupInfoMixin")
    final class Success implements SmaxGroupsGetGroupInfoResponse {
        /**
         * Holds the group's participant count in the range {@code [0, 19999]}; {@code null} when the relay
         * omitted the {@code size} attribute.
         */
        private final Integer groupSize;

        /**
         * Holds the {@code <group/>} child carrying the full group-info subtree.
         */
        private final Stanza group;

        /**
         * Constructs a success variant.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
         *
         * @param groupSize the group's participant count; may be {@code null} when the relay omitted the
         *                  {@code size} attribute
         * @param group     the {@code <group/>} sub-stanza; never {@code null}
         * @throws NullPointerException     if {@code group} is {@code null}
         * @throws IllegalArgumentException if {@code groupSize} is negative
         */
        public Success(Integer groupSize, Stanza group) {
            if (groupSize != null && groupSize < 0) {
                throw new IllegalArgumentException("groupSize must be non-negative");
            }
            this.groupSize = groupSize;
            this.group = Objects.requireNonNull(group, "group cannot be null");
        }

        /**
         * Returns the group's participant count.
         *
         * @return an {@link Optional} carrying the group size, or empty when the relay omitted the
         *         {@code size} attribute
         */
        public Optional<Integer> groupSize() {
            return Optional.ofNullable(groupSize);
        }

        /**
         * Returns the raw {@code <group/>} sub-stanza carrying the remaining group-info fields.
         *
         * <p>Callers drive their own projection via {@link Stanza#getAttributeAsString(String)} and
         * {@link Stanza#streamChildren(String)} against this stanza.
         *
         * @return the {@code <group/>} {@link Stanza}; never {@code null}
         */
        public Stanza group() {
            return group;
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant.
         *
         * <p>Runs as the first probe in the variant cascade of
         * {@link SmaxGroupsGetGroupInfoResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation validates the IQ envelope via
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, extracts the {@code <group/>} child, and
         * reads the optional {@code size} attribute as the participant count.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetGroupInfoResponseSuccess",
                exports = "parseGetGroupInfoResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var group = stanza.getChild("group").orElse(null);
            if (group == null) {
                return Optional.empty();
            }
            var sizeOpt = group.getAttributeAsInt("size");
            var size = sizeOpt.isPresent() ? sizeOpt.getAsInt() : null;
            var success = new Success(size, group);
            return Optional.of(success);
        }

        /**
         * Compares this variant to {@code obj} for value equality across both fields.
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
            return Objects.equals(this.groupSize, that.groupSize) && Objects.equals(this.group, that.group);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(groupSize, group);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetGroupInfoResponse.Success[groupSize=" + groupSize
                    + ", group=" + group + ']';
        }
    }

    /**
     * Reports that the relay rejected the request as malformed, unauthorised, or referencing a non-existent
     * group.
     *
     * <p>Carries the numeric {@link #errorCode()} and optional {@link #errorText()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetGroupInfoResponseClientError")
    final class ClientError implements SmaxGroupsGetGroupInfoResponse {
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
         * {@link SmaxGroupsGetGroupInfoResponse#of(Stanza, Stanza)}.
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetGroupInfoResponseClientError",
                exports = "parseGetGroupInfoResponseClientError",
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
            return "SmaxGroupsGetGroupInfoResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Reports that the relay encountered a transient internal failure.
     *
     * <p>Callers decide whether to retry based on the surfaced {@link #errorCode()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetGroupInfoResponseServerError")
    final class ServerError implements SmaxGroupsGetGroupInfoResponse {
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
         * {@link SmaxGroupsGetGroupInfoResponse#of(Stanza, Stanza)}.
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetGroupInfoResponseServerError",
                exports = "parseGetGroupInfoResponseServerError",
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
            return "SmaxGroupsGetGroupInfoResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

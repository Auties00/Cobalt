package com.github.auties00.cobalt.wire.stanza.iq.group;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;

import java.util.Objects;
import java.util.Optional;

/**
 * Roots the sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqQueryGroupInviteV4Request}.
 *
 * <p>The hierarchy permits exactly three variants. {@link Success} carries the {@code <group>}
 * metadata node the caller parses into group metadata; {@link ClientError} surfaces a relay rejection
 * in the sub-{@code 500} code range (typically an expired or revoked invite); {@link ServerError}
 * surfaces a transient relay failure in the {@code 500}-and-above range. Callers switch on the parsed
 * variant to discriminate the relay outcome.
 */
@WhatsAppWebModule(moduleName = "WAWebGroupInviteV4Job")
public sealed interface IqQueryGroupInviteV4Response extends IqStanza.Response
        permits IqQueryGroupInviteV4Response.Success, IqQueryGroupInviteV4Response.ClientError, IqQueryGroupInviteV4Response.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqQueryGroupInviteV4Response} variant.
     *
     * <p>Each variant's {@code of(stanza, request)} factory is tried in priority order, success then
     * client-error then server-error, and the first present result is returned. An empty result means
     * no documented variant matched.
     *
     * @param stanza  the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant
     *         matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupInviteV4Job", exports = "queryGroupInviteV4",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqQueryGroupInviteV4Response> of(Stanza stanza, Stanza request) {
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
     * Carries the success outcome: the {@code <group>} metadata node echoed by the relay.
     *
     * <p>{@link #group()} is the raw {@code <group>} stanza the caller feeds into the chat-metadata
     * parser to materialise the group's subject, participants, and settings; the parse is left to the
     * caller because it triggers store side effects and, for communities, additional sub-queries.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupInviteV4Job")
    final class Success implements IqQueryGroupInviteV4Response {
        /**
         * Holds the {@code <group>} metadata node echoed by the relay.
         */
        private final Stanza group;

        /**
         * Constructs a successful reply carrying the {@code <group>} metadata node.
         *
         * @param group the {@code <group>} stanza; never {@code null}
         * @throws NullPointerException if {@code group} is {@code null}
         */
        public Success(Stanza group) {
            this.group = Objects.requireNonNull(group, "group cannot be null");
        }

        /**
         * Returns the {@code <group>} metadata node echoed by the relay.
         *
         * @return the {@code <group>} stanza; never {@code null}
         */
        public Stanza group() {
            return group;
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant when it matches the success
         * schema.
         *
         * <p>Returns empty when the {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}
         * result-envelope check fails or when the {@code <group>} child is absent.
         *
         * @param stanza  the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupInviteV4Job", exports = "queryGroupInviteV4",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var groupNode = stanza.getChild("group").orElse(null);
            if (groupNode == null) {
                return Optional.empty();
            }
            return Optional.of(new Success(groupNode));
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two success variants are equal when they share the same runtime class and the same
         * {@link #group()} node.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal success variant
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
            return Objects.equals(this.group, that.group);
        }

        /**
         * Returns a hash code derived from the {@code <group>} node.
         *
         * @return the field-derived hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(group);
        }

        /**
         * Returns a debug string carrying the {@code <group>} node.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "IqQueryGroupInviteV4Response.Success[group=" + group + ']';
        }
    }

    /**
     * Carries a client-error outcome: the relay rejected the query with a sub-{@code 500} code.
     *
     * <p>The failure is reportable rather than retryable; it typically signals an expired, revoked, or
     * malformed invite.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupInviteV4Job")
    final class ClientError implements IqQueryGroupInviteV4Response {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text; {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply carrying the relay-echoed envelope.
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
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ClientError} variant when it matches the standard
         * SMAX client-error envelope.
         *
         * <p>Returns empty when the envelope check fails; the parse is delegated entirely to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza  the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupInviteV4Job", exports = "queryGroupInviteV4",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two client-error variants are equal when they share the same runtime class, the same
         * {@link #errorCode()}, and the same error text.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal client-error variant
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
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the error code and error text.
         *
         * @return the field-derived hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and error text.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "IqQueryGroupInviteV4Response.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a server-error outcome: the relay hit a transient {@code 500}-and-above failure
     * processing the query.
     *
     * <p>The failure is typically retryable after a short backoff.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupInviteV4Job")
    final class ServerError implements IqQueryGroupInviteV4Response {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text; {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply carrying the relay-echoed envelope.
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
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ServerError} variant when it matches the standard
         * SMAX server-error envelope.
         *
         * <p>Returns empty when the envelope check fails; the parse is delegated entirely to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza  the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupInviteV4Job", exports = "queryGroupInviteV4",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two server-error variants are equal when they share the same runtime class, the same
         * {@link #errorCode()}, and the same error text.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal server-error variant
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
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the error code and error text.
         *
         * @return the field-derived hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and error text.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "IqQueryGroupInviteV4Response.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

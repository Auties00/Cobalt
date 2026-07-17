package com.github.auties00.cobalt.wire.stanza.iq.push;

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
 * Models the inbound reply to an {@link IqGetPushServerSettingsRequest}.
 *
 * <p>The reply is one of three variants: {@link Success} carries the relay-issued server push key;
 * {@link ClientError} signals that the relay rejected the query as malformed or unauthorised; and
 * {@link ServerError} signals a transient relay failure. Splitting the reply this way lets the
 * browser-push driver distinguish a hard rejection from a retryable failure.
 *
 * @implNote This implementation splits the reply into three variants where WA Web's parser collapses
 * the success and error paths into a single {@code {webserverkey | {errorCode, errorText}}}
 * discriminated record.
 */
public sealed interface IqGetPushServerSettingsResponse extends IqStanza.Response
        permits IqGetPushServerSettingsResponse.Success, IqGetPushServerSettingsResponse.ClientError, IqGetPushServerSettingsResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching reply variant.
     *
     * <p>Each variant is attempted in priority order ({@link Success}, then {@link ClientError},
     * then {@link ServerError}) and the first that parses cleanly is returned. At most one variant
     * ever matches: {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} accepts only a
     * {@code type="result"} envelope while {@link SmaxBaseServerErrorMixin} accepts only a
     * {@code type="error"} envelope, so the accepted stanza shapes are disjoint.
     *
     * @param stanza    the inbound IQ stanza received from the relay
     * @param request the original outbound stanza, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no
     *         documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebGetPushServerSettingsJob",
            exports = "getPushServerSettings", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqGetPushServerSettingsResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the relay-issued server-side push key on a successful query.
     *
     * <p>The {@code webserverkey} is the VAPID-style server public key the browser uses to validate
     * web-push payload signatures; the browser-push driver hands it to {@code PushManager.subscribe}
     * verbatim.
     */
    @WhatsAppWebModule(moduleName = "WAWebGetPushServerSettingsJob")
    final class Success implements IqGetPushServerSettingsResponse {
        /**
         * Holds the base64-encoded server-side push key read from the {@code webserverkey}
         * attribute of the {@code <settings/>} grandchild.
         */
        private final String webServerKey;

        /**
         * Constructs a new successful reply.
         *
         * @param webServerKey the server-side push key
         * @throws NullPointerException if {@code webServerKey} is {@code null}
         */
        public Success(String webServerKey) {
            this.webServerKey = Objects.requireNonNull(webServerKey, "webServerKey cannot be null");
        }

        /**
         * Returns the base64-encoded server-side push key.
         *
         * @return the key string, never {@code null}
         */
        public String webServerKey() {
            return webServerKey;
        }

        /**
         * Parses a {@link Success} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza is not a valid result envelope per
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, when the envelope lacks the
         * {@code <settings/>} grandchild, or when that grandchild lacks the {@code webserverkey}
         * attribute; the caller falls through to the error variants in any of these cases.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when
         *         the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGetPushServerSettingsJob",
                exports = "getPushServerSettings", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var settingsChild = stanza.getChild("settings").orElse(null);
            if (settingsChild == null) {
                return Optional.empty();
            }
            var webServerKey = settingsChild.getAttributeAsString("webserverkey").orElse(null);
            if (webServerKey == null) {
                return Optional.empty();
            }
            return Optional.of(new Success(webServerKey));
        }

        /**
         * Compares this reply to another object for equality.
         *
         * <p>Two replies are equal when they share the exact runtime class and the same
         * {@link #webServerKey()}.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is an equal {@link Success}, otherwise {@code false}
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
            return Objects.equals(this.webServerKey, that.webServerKey);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code derived from {@link #webServerKey()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(webServerKey);
        }

        /**
         * Returns a debugging representation of this reply.
         *
         * @return a string including the {@link #webServerKey()} value
         */
        @Override
        public String toString() {
            return "IqGetPushServerSettingsResponse.Success[webServerKey=" + webServerKey + ']';
        }
    }

    /**
     * Signals that the relay rejected the push-server-settings query as malformed or unauthorised.
     *
     * <p>This is the client-fault branch of the reply pipeline; the browser-push driver treats it
     * as a hard failure and skips the subscription attempt for the session.
     */
    @WhatsAppWebModule(moduleName = "WAWebGetPushServerSettingsJob")
    final class ClientError implements IqGetPushServerSettingsResponse {
        /**
         * Holds the numeric server-side error code read from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text read from the {@code <error text/>} attribute, or
         * {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable text, or {@code null} when omitted
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when the relay
         *         omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Returns a populated {@link Optional} only when the stanza is a {@code type="error"}
         * envelope that echoes the {@code request} id and carries a {@code <error/>} child whose
         * {@code code} attribute falls in the client-fault range, per the contract of
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when
         *         the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGetPushServerSettingsJob",
                exports = "getPushServerSettings", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for equality.
         *
         * <p>Two replies are equal when they share the exact runtime class, the same
         * {@link #errorCode()}, and the same {@link #errorText()}.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is an equal {@link ClientError}, otherwise
         *         {@code false}
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code derived from {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debugging representation of this reply.
         *
         * @return a string including the {@link #errorCode()} and {@link #errorText()} values
         */
        @Override
        public String toString() {
            return "IqGetPushServerSettingsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Signals a transient server-side failure on a push-server-settings query.
     *
     * <p>This is the server-fault branch of the reply pipeline; the browser-push driver may retry
     * the same query on the next session attempt once the socket has settled.
     */
    @WhatsAppWebModule(moduleName = "WAWebGetPushServerSettingsJob")
    final class ServerError implements IqGetPushServerSettingsResponse {
        /**
         * Holds the numeric server-side error code read from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text read from the {@code <error text/>} attribute, or
         * {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable text, or {@code null} when omitted
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when the relay
         *         omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Returns a populated {@link Optional} only when the stanza is a {@code type="error"}
         * envelope that echoes the {@code request} id and carries a {@code <error/>} child whose
         * {@code code} attribute falls outside the client-fault range, per the contract of
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when
         *         the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGetPushServerSettingsJob",
                exports = "getPushServerSettings", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for equality.
         *
         * <p>Two replies are equal when they share the exact runtime class, the same
         * {@link #errorCode()}, and the same {@link #errorText()}.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is an equal {@link ServerError}, otherwise
         *         {@code false}
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code derived from {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debugging representation of this reply.
         *
         * @return a string including the {@link #errorCode()} and {@link #errorText()} values
         */
        @Override
        public String toString() {
            return "IqGetPushServerSettingsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

package com.github.auties00.cobalt.wire.stanza.iq.status;

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
 * Models the sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqSetAboutRequest}.
 *
 * <p>The reply is classified into one of three variants: {@link Success} carrying the relay
 * assigned about-text revision identifier, {@link ClientError} for a caller-attributable
 * rejection such as a length-cap overflow, and {@link ServerError} for a transient
 * relay-attributable failure. The split lets the about-edit surface distinguish a permanent
 * rejection from a failure that can be retried.
 */
public sealed interface IqSetAboutResponse extends IqStanza.Response
        permits IqSetAboutResponse.Success, IqSetAboutResponse.ClientError, IqSetAboutResponse.ServerError {

    /**
     * Tries each {@link IqSetAboutResponse} variant in priority order and returns the first that
     * parses cleanly.
     *
     * <p>Variants are attempted in the order {@link Success}, {@link ClientError},
     * {@link ServerError}. The first variant whose own {@code of(Stanza, Stanza)} parser produces a
     * present result wins; if none match, {@link Optional#empty()} is returned.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound stanza
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when
     *         no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSetAboutJob",
            exports = "setAbout", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqSetAboutResponse> of(Stanza stanza, Stanza request) {
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
     * Models the reply variant carrying the revision identifier the relay assigned to the new
     * about-text version.
     *
     * <p>The about-id is monotonically increasing and is read from the {@code id} attribute of
     * the success envelope, letting callers order subsequent edits against concurrently
     * arriving server-pushed about-text updates.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetAboutJob")
    final class Success implements IqSetAboutResponse {
        /**
         * Holds the relay-assigned about-text revision identifier read from the {@code id}
         * attribute of the success envelope.
         */
        private final long aboutId;

        /**
         * Constructs a successful reply from the given revision identifier.
         *
         * @param aboutId the relay-assigned revision identifier
         */
        public Success(long aboutId) {
            this.aboutId = aboutId;
        }

        /**
         * Returns the relay-assigned about-text revision identifier.
         *
         * @return the about-id
         */
        public long aboutId() {
            return aboutId;
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         *
         * <p>Returns a present {@link Optional} only when the stanza is a successful IQ result
         * echoing the {@code request} id, per {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)},
         * and carries an integer {@code id} attribute.
         *
         * @implNote This implementation returns {@link Optional#empty()} rather than
         *           synthesising an about-id when the {@code id} attribute is absent, because a
         *           fabricated identifier would break the monotonic ordering callers rely on.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSetAboutJob",
                exports = "aboutResponse",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var idAttr = stanza.getAttributeAsLong("id");
            if (idAttr.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Success(idAttr.getAsLong()));
        }

        /**
         * Compares this reply with another object for value equality on the about-id.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link Success} with an equal about-id,
         *         {@code false} otherwise
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
            return this.aboutId == that.aboutId;
        }

        /**
         * Returns a hash code derived from the about-id.
         *
         * @return the hash code for this reply
         */
        @Override
        public int hashCode() {
            return Objects.hash(aboutId);
        }

        /**
         * Returns a debug string rendering the about-id.
         *
         * @return the string representation of this reply
         */
        @Override
        public String toString() {
            return "IqSetAboutResponse.Success[aboutId=" + aboutId + ']';
        }
    }

    /**
     * Models the reply variant signalling that the relay rejected the about-text update for a
     * caller-attributable reason.
     *
     * <p>This variant covers errors whose code is below the server-error threshold, such as the
     * length-cap rejection returned when the about text exceeds the relay-side limit. Such a
     * rejection is permanent: resending the same value yields the same error.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetAboutJob")
    final class ClientError implements IqSetAboutResponse {
        /**
         * Holds the numeric server-side error code read from the {@code <error code/>}
         * attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the {@code <error text/>}
         * attribute, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the given code and optional text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional text, or {@code null} when omitted
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
         * Returns the optional error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Returns a present {@link Optional} only when the stanza is an error IQ echoing the
         * {@code request} id whose error code falls in the client-error range, as partitioned
         * by {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSetAboutJob",
                exports = "setAbout", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another object for value equality on code and text.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link ClientError} with an equal code and
         *         text, {@code false} otherwise
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
         * Returns a hash code derived from the code and text.
         *
         * @return the hash code for this reply
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string rendering the code and text.
         *
         * @return the string representation of this reply
         */
        @Override
        public String toString() {
            return "IqSetAboutResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the reply variant signalling a transient server-side failure on an about-text
     * update.
     *
     * <p>This variant covers errors whose code is at or above the server-error threshold. The
     * failure is not caller-attributable, so the same update may succeed when retried.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetAboutJob")
    final class ServerError implements IqSetAboutResponse {
        /**
         * Holds the numeric server-side error code read from the {@code <error code/>}
         * attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the {@code <error text/>}
         * attribute, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the given code and optional text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional text, or {@code null} when omitted
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
         * Returns the optional error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Returns a present {@link Optional} only when the stanza is an error IQ echoing the
         * {@code request} id whose error code falls in the server-error range, as partitioned
         * by {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSetAboutJob",
                exports = "setAbout", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another object for value equality on code and text.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link ServerError} with an equal code and
         *         text, {@code false} otherwise
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
         * Returns a hash code derived from the code and text.
         *
         * @return the hash code for this reply
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string rendering the code and text.
         *
         * @return the string representation of this reply
         */
        @Override
        public String toString() {
            return "IqSetAboutResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

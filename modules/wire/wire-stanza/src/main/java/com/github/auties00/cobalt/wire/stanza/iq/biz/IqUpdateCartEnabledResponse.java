package com.github.auties00.cobalt.wire.stanza.iq.biz;

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
 * Models the inbound reply the relay produces in response to an {@link IqUpdateCartEnabledRequest}.
 *
 * <p>The sealed family is pattern-matched to drive the commerce-settings edit surface:
 * {@link Success} echoes the post-mutation cart-enabled flag, {@link ClientError} surfaces a
 * rejected mutation and {@link ServerError} surfaces a transient internal failure.
 */
@WhatsAppWebModule(moduleName = "WAWebBusinessProfileJob")
public sealed interface IqUpdateCartEnabledResponse extends IqStanza.Response
        permits IqUpdateCartEnabledResponse.Success, IqUpdateCartEnabledResponse.ClientError, IqUpdateCartEnabledResponse.ServerError {

    /**
     * Tries each variant in priority order until one matches.
     *
     * <p>The order is {@link Success}, then {@link ClientError}, then {@link ServerError}; call this
     * on every IQ stanza acking a cart-enabled mutation.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqUpdateCartEnabledResponse> of(Stanza stanza, Stanza request) {
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
     * Models the {@code Success} variant, which carries the post-mutation cart-enabled flag the
     * relay echoes inside {@code <commerce_settings><cart enabled/>}.
     *
     * <p>Read {@link #cartEnabled()} to refresh the cached commerce-settings projection so the
     * catalog-grid affordance reflects the new state immediately.
     */
    final class Success implements IqUpdateCartEnabledResponse {
        /**
         * Holds the post-mutation cart-enabled flag echoed by the relay.
         */
        private final boolean cartEnabled;

        /**
         * Constructs a success reply from the post-mutation flag; called from {@link #of(Stanza, Stanza)}.
         *
         * @param cartEnabled the post-mutation flag
         */
        public Success(boolean cartEnabled) {
            this.cartEnabled = cartEnabled;
        }

        /**
         * Returns the post-mutation flag.
         *
         * <p>The value should match the requested state in the absence of relay-side coercion.
         *
         * @return the flag
         */
        public boolean cartEnabled() {
            return cartEnabled;
        }

        /**
         * Tries to parse a {@link Success} variant from the stanza.
         *
         * <p>The method validates the {@code <iq type="result">} envelope and reads the post-mutation
         * flag from {@code <commerce_settings><cart enabled/>}; called from {@link #of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation decodes the {@code enabled} attribute as a literal {@code "true"}
         * match.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebBusinessProfileJob",
                exports = "commerceSettingsResponse", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var enabled = stanza.getChild("commerce_settings")
                    .flatMap(cs -> cs.getChild("cart"))
                    .flatMap(cart -> cart.getAttributeAsString("enabled"))
                    .map("true"::equals)
                    .orElse(false);
            return Optional.of(new Success(enabled));
        }

        /**
         * Compares this variant with another for value equality on the cart-enabled flag.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal success
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
            return this.cartEnabled == that.cartEnabled;
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Boolean.hashCode(cartEnabled);
        }

        /**
         * Returns a diagnostic string naming the cart-enabled flag.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqUpdateCartEnabledResponse.Success[cartEnabled=" + cartEnabled + ']';
        }
    }

    /**
     * Models the {@code ClientError} variant, emitted when the relay rejects the mutation as
     * malformed or unauthorised.
     *
     * <p>Surface it as a user-facing 4xx-class error on the commerce-settings edit surface.
     */
    final class ClientError implements IqUpdateCartEnabledResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the relay's {@code <error/>} envelope; called from
         * {@link #of(Stanza, Stanza)}.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code, used to dispatch on the relay-side rejection reason.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots, so it is suitable for
         * logging only.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the stanza.
         *
         * <p>The method delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} to
         * extract the (code, text) envelope; called from {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the client-error schema
         */
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant with another for value equality on the code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal client error
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a diagnostic string naming the error code and text.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqUpdateCartEnabledResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the {@code ServerError} variant, emitted when the relay returns a transient
     * internal-failure status while processing the mutation.
     *
     * <p>The relay returns this shape when the commerce-settings backend is temporarily unavailable;
     * use it to drive a backoff-and-retry path on the commerce-settings edit surface.
     */
    final class ServerError implements IqUpdateCartEnabledResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the relay's {@code <error/>} envelope; called from
         * {@link #of(Stanza, Stanza)}.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code; a 5xx-class value is the canonical retry trigger.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots, so it is suitable for
         * logging only.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the stanza.
         *
         * <p>The method delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} to
         * extract the (code, text) envelope; called from {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the server-error schema
         */
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant with another for value equality on the code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal server error
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a diagnostic string naming the error code and text.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqUpdateCartEnabledResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

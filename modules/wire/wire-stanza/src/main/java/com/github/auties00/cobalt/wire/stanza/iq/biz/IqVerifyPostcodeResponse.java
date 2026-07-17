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
 * Models the inbound reply the relay produces in response to an {@link IqVerifyPostcodeRequest}.
 *
 * <p>The sealed family is pattern-matched to drive the cart-postcode entry surface: {@link Success}
 * carries the verdict and the optional encrypted location-name echo, {@link ClientError} surfaces a
 * rejected request and {@link ServerError} surfaces a transient internal failure.
 */
@WhatsAppWebModule(moduleName = "WAWebVerifyPostcodeJob")
public sealed interface IqVerifyPostcodeResponse extends IqStanza.Response
        permits IqVerifyPostcodeResponse.Success, IqVerifyPostcodeResponse.ClientError, IqVerifyPostcodeResponse.ServerError {

    /**
     * Tries each variant in priority order until one matches.
     *
     * <p>The order is {@link Success}, then {@link ClientError}, then {@link ServerError}; call this
     * on every IQ stanza tagged with a {@code <result_code/>} payload.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant
     *         matched
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqVerifyPostcodeResponse> of(Stanza stanza, Stanza request) {
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
     * Models the {@code Success} variant, which carries the verdict and the optional encrypted
     * location-name echo.
     *
     * <p>Dispatch on {@link #resultCode()} to drive the cart-postcode UI:
     * {@link ResultCode#SUCCESS} unlocks the catalog grid for the resolved address,
     * {@link ResultCode#INVALID_POSTCODE} prompts the buyer to retry the entry and
     * {@link ResultCode#UNSERVICEABLE_LOCATION} blocks checkout with a "not delivered to your area"
     * message.
     */
    final class Success implements IqVerifyPostcodeResponse {
        /**
         * Enumerates the verdicts the relay's reference parser documents inside the
         * {@code <result_code/>} child.
         *
         * <p>The relay throws a 500-status error for any value outside this set, so any received
         * string outside this enum is treated as an unparseable success.
         */
        public enum ResultCode {
            /**
             * Marks that the postcode was successfully decrypted and resolves to a serviceable
             * address, so the catalog grid is unlocked for the resolved location.
             */
            SUCCESS("success"),

            /**
             * Marks that the postcode could not be decrypted or did not pass validation, so the
             * buyer is prompted to retry the entry with a different postcode.
             */
            INVALID_POSTCODE("invalid_postcode"),

            /**
             * Marks that the postcode resolved to an address outside the merchant's serviceable area,
             * so checkout is blocked with a "not delivered to your area" message.
             */
            UNSERVICEABLE_LOCATION("unserviceable_location");

            /**
             * Holds the canonical string the relay echoes in the {@code <result_code/>} element.
             */
            private final String wireValue;

            /**
             * Constructs a constant from its wire string.
             *
             * @param wireValue the wire string; never {@code null}
             */
            ResultCode(String wireValue) {
                this.wireValue = wireValue;
            }

            /**
             * Returns the wire string for this constant.
             *
             * <p>The relay never accepts the enum name itself, so the wire string is used when
             * round-tripping the enum back to the wire (e.g. for logging or fixture capture).
             *
             * @return the wire string; never {@code null}
             */
            public String wireValue() {
                return wireValue;
            }

            /**
             * Looks up the constant for the given wire string.
             *
             * <p>An empty optional means the relay returned an unknown verdict and the caller should
             * treat the success as unparseable.
             *
             * @param wireValue the wire string; may be {@code null}
             * @return an {@link Optional} carrying the matching constant, or empty when the value is
             *         unknown
             */
            public static Optional<ResultCode> of(String wireValue) {
                if (wireValue == null) {
                    return Optional.empty();
                }
                for (var value : values()) {
                    if (value.wireValue.equals(wireValue)) {
                        return Optional.of(value);
                    }
                }
                return Optional.empty();
            }
        }

        /**
         * Holds the verdict returned by the relay.
         */
        private final ResultCode resultCode;

        /**
         * Holds the opaque encrypted location-name echo returned for {@link ResultCode#SUCCESS} only;
         * absent for the other verdicts.
         */
        private final String encryptedLocationName;

        /**
         * Constructs a success reply from the verdict and the optional encrypted echo; called from
         * {@link #of(Stanza, Stanza)}.
         *
         * @param resultCode            the verdict; never {@code null}
         * @param encryptedLocationName the optional encrypted echo; may be {@code null}
         * @throws NullPointerException if {@code resultCode} is {@code null}
         */
        public Success(ResultCode resultCode, String encryptedLocationName) {
            this.resultCode = Objects.requireNonNull(resultCode, "resultCode cannot be null");
            this.encryptedLocationName = encryptedLocationName;
        }

        /**
         * Returns the verdict; see {@link ResultCode} for the documented values.
         *
         * @return the verdict; never {@code null}
         */
        public ResultCode resultCode() {
            return resultCode;
        }

        /**
         * Returns the optional encrypted location-name echo.
         *
         * <p>The echo is fed back into the buyer-side direct-connection decryption flow to display
         * the resolved location-name label in the cart UI. An empty optional means the verdict is not
         * {@link ResultCode#SUCCESS} or the relay omitted the echo.
         *
         * @return an {@link Optional} carrying the echo
         */
        public Optional<String> encryptedLocationName() {
            return Optional.ofNullable(encryptedLocationName);
        }

        /**
         * Tries to parse a {@link Success} variant from the stanza.
         *
         * <p>The method validates the {@code <iq type="result">} envelope, decodes the
         * {@code <result_code/>} body and reads the optional {@code <encrypted_location_name/>} echo;
         * called from {@link #of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation returns an empty optional when the {@code <result_code/>} body is
         * unknown; WA Web's reference parser throws a 500-status error in that case, which Cobalt
         * defers to the caller's error handler.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema or the {@code result_code} is unknown
         */
        @WhatsAppWebExport(moduleName = "WAWebVerifyPostcodeJob",
                exports = "VerifyPostcode", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var resultCodeNode = stanza.getChild("result_code").orElse(null);
            if (resultCodeNode == null) {
                return Optional.empty();
            }
            var resultCodeValue = resultCodeNode.toContentString().orElse(null);
            var resultCode = ResultCode.of(resultCodeValue).orElse(null);
            if (resultCode == null) {
                return Optional.empty();
            }
            var encryptedLocationName = stanza.getChild("encrypted_location_name")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            return Optional.of(new Success(resultCode, encryptedLocationName));
        }

        /**
         * Compares this variant with another for value equality on the verdict and encrypted echo.
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
            return this.resultCode == that.resultCode
                    && Objects.equals(this.encryptedLocationName, that.encryptedLocationName);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(resultCode, encryptedLocationName);
        }

        /**
         * Returns a diagnostic string naming the verdict and encrypted echo.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqVerifyPostcodeResponse.Success[resultCode=" + resultCode
                    + ", encryptedLocationName=" + encryptedLocationName + ']';
        }
    }

    /**
     * Models the {@code ClientError} variant, emitted when the relay rejects the request as
     * malformed or referencing an unknown merchant.
     *
     * <p>Surface it as a user-facing 4xx-class error on the cart-postcode entry surface.
     */
    final class ClientError implements IqVerifyPostcodeResponse {
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
            return "IqVerifyPostcodeResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the {@code ServerError} variant, emitted when the relay returns a transient
     * internal-failure status while processing the request.
     *
     * <p>The relay returns this shape when the catalog backend is temporarily unavailable; use it to
     * drive a backoff-and-retry path on the cart-postcode entry surface.
     */
    final class ServerError implements IqVerifyPostcodeResponse {
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
            return "IqVerifyPostcodeResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

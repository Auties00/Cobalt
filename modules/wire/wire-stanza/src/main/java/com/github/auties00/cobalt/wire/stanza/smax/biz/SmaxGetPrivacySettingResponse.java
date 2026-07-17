package com.github.auties00.cobalt.wire.stanza.smax.biz;

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
 * Models the sealed family of inbound reply variants produced by the relay in response to a
 * {@link SmaxGetPrivacySettingRequest}.
 * <p>
 * Each variant projects a distinct outcome of the SMB data-sharing-with-Meta consent bridge:
 * {@link Success} carries the current consent value, {@link ClientError} carries a documented
 * {@code 4xx} rejection, and {@link ServerError} carries a transient {@code 5xx} relay failure.
 */
public sealed interface SmaxGetPrivacySettingResponse extends SmaxStanza.Response
        permits SmaxGetPrivacySettingResponse.Success, SmaxGetPrivacySettingResponse.ClientError, SmaxGetPrivacySettingResponse.ServerError {

    /**
     * Tries each variant in priority order and returns the first that parses cleanly.
     * <p>
     * Attempts {@link Success#of(Stanza, Stanza)} first, then {@link ClientError#of(Stanza, Stanza)},
     * then {@link ServerError#of(Stanza, Stanza)}, so that a malformed success stanza falls through
     * to an error variant rather than masking an error. An unrecognised stanza shape yields
     * {@link Optional#empty()}.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza, used to validate echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBizSettingsGetPrivacySettingRPC",
            exports = "sendGetPrivacySettingRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsPrivacySettingErrors",
            exports = "parsePrivacySettingErrors", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGetPrivacySettingResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the current SMB data-sharing-with-Meta consent value.
     * <p>
     * Projected when the relay returns the documented
     * {@code <privacy><smb_data_sharing_with_meta_consent>} tree; the wire value is one of the
     * {@code "true"}, {@code "false"} or {@code "notset"} dictionary literals.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsGetPrivacySettingResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsSmbDataSharingSettingMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsSmbDataSharingSettingValueMixin")
    final class Success implements SmaxGetPrivacySettingResponse {
        /**
         * The {@code value} attribute of the {@code <smb_data_sharing_with_meta_consent>}
         * child; one of the {@code "true"}, {@code "false"} or {@code "notset"} literals.
         */
        private final String dataSharingConsent;

        /**
         * Constructs a successful reply from a consent value already validated against the dictionary.
         *
         * @param dataSharingConsent the consent enum value; never {@code null}
         * @throws NullPointerException if {@code dataSharingConsent} is {@code null}
         */
        public Success(String dataSharingConsent) {
            this.dataSharingConsent = Objects.requireNonNull(dataSharingConsent,
                    "dataSharingConsent cannot be null");
        }

        /**
         * Returns the consent enum value.
         *
         * @return the consent value; never {@code null}
         */
        public String dataSharingConsent() {
            return dataSharingConsent;
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         * <p>
         * Enforces the {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} envelope check,
         * walks the {@code <privacy><smb_data_sharing_with_meta_consent>} tree, and validates
         * the {@code value} attribute against {@link SmaxBizSettingsFalseNotsetTrueFlag#of(String)};
         * any other value yields {@link Optional#empty()} rather than an exception.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsGetPrivacySettingResponseSuccess",
                exports = "parseGetPrivacySettingResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var privacy = stanza.getChild("privacy").orElse(null);
            if (privacy == null) {
                return Optional.empty();
            }
            var consent = privacy.getChild("smb_data_sharing_with_meta_consent").orElse(null);
            if (consent == null) {
                return Optional.empty();
            }
            var value = consent.getAttributeAsString("value").orElse(null);
            if (SmaxBizSettingsFalseNotsetTrueFlag.of(value).isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Success(value));
        }

        /**
         * Compares this reply to another object for value equality on the consent value.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} with an equal consent value
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
            return Objects.equals(this.dataSharingConsent, that.dataSharingConsent);
        }

        /**
         * Returns a hash code derived from the consent value.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(dataSharingConsent);
        }

        /**
         * Returns a debug rendering listing the consent value.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetPrivacySettingResponse.Success[dataSharingConsent="
                    + dataSharingConsent + ']';
        }
    }

    /**
     * Carries a documented {@code 4xx} privacy-setting rejection.
     * <p>
     * Surfaced when the relay rejected the consent fetch; the CTWA settings surface treats the
     * consent value as indeterminate on this branch.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsGetPrivacySettingResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsPrivacySettingErrors")
    final class ClientError implements SmaxGetPrivacySettingResponse {
        /**
         * The numeric server-side error code in the {@code 4xx} range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied one.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from a validated {@code 4xx} envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
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
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         * <p>
         * Routes the error extraction through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} and admits the full
         * {@code 4xx} range as a catch-all.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsGetPrivacySettingResponseError",
                exports = "parseGetPrivacySettingResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality across both fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with equal fields
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
         * Returns a hash code derived from both fields.
         *
         * @return the combined hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering listing the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetPrivacySettingResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a transient {@code 5xx} relay failure.
     * <p>
     * Surfaced when the relay could not read the consent for an internal reason; callers can
     * re-issue the request with backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsGetPrivacySettingResponseError")
    final class ServerError implements SmaxGetPrivacySettingResponse {
        /**
         * The numeric server-side error code in the {@code 5xx} range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied one.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from a validated {@code 5xx} envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
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
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         * <p>
         * Delegates the {@code 5xx} range check to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}; a stanza outside the
         * {@code 5xx} range yields {@link Optional#empty()}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsGetPrivacySettingResponseError",
                exports = "parseGetPrivacySettingResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality across both fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ServerError} with equal fields
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
         * Returns a hash code derived from both fields.
         *
         * @return the combined hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering listing the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetPrivacySettingResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

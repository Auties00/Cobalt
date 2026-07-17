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
 * The sealed family of inbound reply variants produced by the relay
 * in response to a {@link SmaxSetPrivacySettingRequest}.
 *
 * <p>The CTWA data-sharing settings flow writes the new SMB data-sharing-with-Meta consent value
 * to the relay. The three variants split the wire outcome into {@link Success} (relay accepted the
 * write and optionally echoed the post-write consent value), {@link ClientError} (relay rejected
 * the write via a {@code 4xx} envelope) and {@link ServerError} (transient {@code 5xx} relay
 * failure).
 *
 * @implNote
 * This implementation tries each variant in priority order via {@link #of(Stanza, Stanza)} and returns
 * the first successful parse.
 */
public sealed interface SmaxSetPrivacySettingResponse extends SmaxStanza.Response
        permits SmaxSetPrivacySettingResponse.Success, SmaxSetPrivacySettingResponse.ClientError, SmaxSetPrivacySettingResponse.ServerError {

    /**
     * Tries each {@link SmaxSetPrivacySettingResponse} variant in
     * priority order and returns the first that parses cleanly.
     *
     * <p>Invoked by the smax reply pump after dispatching a {@link SmaxSetPrivacySettingRequest}.
     * The priority order ensures a malformed {@link Success} stanza falls through to
     * {@link ClientError} rather than masking an error.
     *
     * @implNote
     * This implementation invokes {@link Success#of(Stanza, Stanza)}
     * first, then {@link ClientError#of(Stanza, Stanza)}, then
     * {@link ServerError#of(Stanza, Stanza)}; an unrecognised stanza
     * shape returns {@link Optional#empty()}.
     *
     * @param stanza    the inbound IQ stanza received from the relay;
     *                never {@code null}
     * @param request the original outbound stanza, used to validate
     *                echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or
     *         {@link Optional#empty()} when no documented variant
     *         matched the stanza shape
     * @throws NullPointerException if either argument is
     *                              {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBizSettingsSetPrivacySettingRPC",
            exports = "sendSetPrivacySettingRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsPrivacySettingErrors",
            exports = "parsePrivacySettingErrors", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxSetPrivacySettingResponse> of(Stanza stanza, Stanza request) {
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
     * The {@code Success} reply variant signalling that the relay
     * accepted the consent write and optionally echoed the
     * post-write value.
     *
     * <p>Projected by {@link SmaxSetPrivacySettingResponse#of(Stanza, Stanza)} when the relay returns
     * the documented {@code <privacy>} envelope. The optional {@link #dataSharingConsent} carries
     * the post-write consent value when the relay echoed one, falling back to {@code null} when the
     * inner consent echo was absent or failed validation.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsSetPrivacySettingResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsSmbDataSharingSettingMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsSmbDataSharingSettingValueMixin")
    final class Success implements SmaxSetPrivacySettingResponse {
        /**
         * The optional echoed consent value drawn from the
         * {@code <smb_data_sharing_with_meta_consent value="..."/>}
         * grandchild; {@code null} when the relay omitted the
         * grandchild or when the {@code value} attribute did not
         * pass the {@code ENUM_FALSE_NOTSET_TRUE} dictionary check.
         */
        private final String dataSharingConsent;

        /**
         * Constructs a new successful reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the {@code <privacy>} envelope has been
         * validated and the optional consent echo has been extracted.
         *
         * @param dataSharingConsent the optional echoed consent
         *                           value; may be {@code null}
         */
        public Success(String dataSharingConsent) {
            this.dataSharingConsent = dataSharingConsent;
        }

        /**
         * Returns the optional echoed consent value.
         *
         * <p>Surfaces the post-write consent value when the relay echoed one (the standard happy
         * path) or {@link Optional#empty()} when the relay sent a bare {@code <privacy/>} reply; the
         * caller treats the empty branch as a recoverable {@code null} write outcome rather than a
         * failure.
         *
         * @return an {@link Optional} carrying the consent value,
         *         or empty when the relay omitted it
         */
        public Optional<String> dataSharingConsent() {
            return Optional.ofNullable(dataSharingConsent);
        }

        /**
         * Tries to parse a {@link Success} variant from the given
         * inbound stanza.
         *
         * @implNote
         * This implementation enforces the
         * {@link SmaxIqResultResponseMixin} envelope check, then
         * walks the optional
         * {@code <privacy><smb_data_sharing_with_meta_consent>}
         * tree without rejecting on a missing grandchild; the
         * {@code value} attribute is admitted only when it passes
         * {@link SmaxBizSettingsFalseNotsetTrueFlag#of(String)}.
         * The inner mixin parse failure being collapsed to a
         * {@code null} consent (rather than a failed parse) matches
         * WA Web's {@code optionalMerge} treatment of
         * {@code parseSmbDataSharingSettingMixin}, which projects
         * {@code privacySmbDataSharingSettingMixin: null} on inner
         * failure.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsSetPrivacySettingResponseSuccess",
                exports = "parseSetPrivacySettingResponseSuccess",
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
            String consent = null;
            var consentNode = privacy.getChild("smb_data_sharing_with_meta_consent").orElse(null);
            if (consentNode != null) {
                var value = consentNode.getAttributeAsString("value").orElse(null);
                if (value != null && SmaxBizSettingsFalseNotsetTrueFlag.of(value).isPresent()) {
                    consent = value;
                }
            }
            return Optional.of(new Success(consent));
        }

        /**
         * {@inheritDoc}
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
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(dataSharingConsent);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxSetPrivacySettingResponse.Success[dataSharingConsent="
                    + dataSharingConsent + ']';
        }
    }

    /**
     * The {@code ClientError} reply variant carrying a documented
     * {@code 4xx} privacy-setting rejection.
     *
     * <p>Surfaced when the relay rejected the consent write. The caller logs the {@code (code,
     * text)} pair and preserves the pre-write display rather than retrying.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsSetPrivacySettingResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsPrivacySettingErrors")
    final class ClientError implements SmaxSetPrivacySettingResponse {
        /**
         * The numeric server-side error code in the {@code 4xx}
         * range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied
         * one.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the {@code 4xx} envelope has been validated.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may
         *                  be {@code null}
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
         * @return an {@link Optional} carrying the error text, or
         *         empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given
         * inbound stanza.
         *
         * @implNote
         * This implementation routes the {@code <iq>}/{@code <error>}
         * extraction through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}
         * and admits the full {@code 4xx} range as a catch-all,
         * matching WA Web's {@code parsePrivacySettingErrors}
         * disjunction.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsSetPrivacySettingResponseError",
                exports = "parseSetPrivacySettingResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
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
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxSetPrivacySettingResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The {@code ServerError} reply variant carrying a transient
     * {@code 5xx} relay failure.
     *
     * <p>Surfaced when the relay returned a transient internal failure while processing the consent
     * write; the caller can re-issue the request with backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizSettingsSetPrivacySettingResponseError")
    final class ServerError implements SmaxSetPrivacySettingResponse {
        /**
         * The numeric server-side error code in the {@code 5xx}
         * range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied
         * one.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the {@code 5xx} envelope has been validated.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may
         *                  be {@code null}
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
         * @return an {@link Optional} carrying the error text, or
         *         empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given
         * inbound stanza.
         *
         * @implNote
         * This implementation delegates the {@code 5xx} range check
         * to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)};
         * any stanza outside the {@code 5xx} range yields
         * {@link Optional#empty()}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsSetPrivacySettingResponseError",
                exports = "parseSetPrivacySettingResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsPrivacySettingErrors",
                exports = "parsePrivacySettingErrors",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
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
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxSetPrivacySettingResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

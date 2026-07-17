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
import java.util.OptionalInt;

/**
 * Models the sealed family of inbound reply variants produced by the relay in response to a
 * {@link SmaxGetBusinessEligibilityRequest}.
 * <p>
 * Each variant projects a distinct outcome of the SMB marketing-messages and Meta-Verified
 * gating bridge: {@link Success} carries 0 to 3 typed eligibility projections,
 * {@link ClientError} carries a documented {@code 4xx} rejection, and {@link ServerError}
 * carries a transient {@code 5xx} relay failure.
 */
public sealed interface SmaxGetBusinessEligibilityResponse extends SmaxStanza.Response
        permits SmaxGetBusinessEligibilityResponse.Success, SmaxGetBusinessEligibilityResponse.ClientError, SmaxGetBusinessEligibilityResponse.ServerError {

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
    @WhatsAppWebExport(moduleName = "WASmaxBizMarketingMessageGetBusinessEligibilityRPC",
            exports = "sendGetBusinessEligibilityRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGetBusinessEligibilityResponse> of(Stanza stanza, Stanza request) {
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
     * Carries 0 to 3 optional feature-eligibility projections.
     * <p>
     * Projected when the relay returns the documented success envelope; each sub-projection is
     * present only when the corresponding feature toggle was set on the outbound
     * {@link SmaxGetBusinessEligibilityRequest}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseSuccess")
    final class Success implements SmaxGetBusinessEligibilityResponse {
        /**
         * The optional {@code <meta_verified/>} projection.
         */
        private final MetaVerified metaVerified;

        /**
         * The optional {@code <marketing_messages/>} projection.
         */
        private final MarketingMessages marketingMessages;

        /**
         * The optional {@code <genai/>} projection.
         */
        private final Genai genai;

        /**
         * Constructs a successful reply from the three already-parsed optional projections.
         *
         * @param metaVerified      the optional Meta-Verified projection; may be {@code null}
         * @param marketingMessages the optional marketing-messages projection; may be {@code null}
         * @param genai             the optional GenAI projection; may be {@code null}
         */
        public Success(MetaVerified metaVerified,
                       MarketingMessages marketingMessages,
                       Genai genai) {
            this.metaVerified = metaVerified;
            this.marketingMessages = marketingMessages;
            this.genai = genai;
        }

        /**
         * Returns the optional Meta-Verified projection.
         *
         * @return an {@link Optional} carrying the projection, or empty when the relay omitted the {@code <meta_verified/>} child
         */
        public Optional<MetaVerified> metaVerified() {
            return Optional.ofNullable(metaVerified);
        }

        /**
         * Returns the optional marketing-messages projection.
         *
         * @return an {@link Optional} carrying the projection, or empty when the relay omitted the {@code <marketing_messages/>} child
         */
        public Optional<MarketingMessages> marketingMessages() {
            return Optional.ofNullable(marketingMessages);
        }

        /**
         * Returns the optional GenAI projection.
         *
         * @return an {@link Optional} carrying the projection, or empty when the relay omitted the {@code <genai/>} child
         */
        public Optional<Genai> genai() {
            return Optional.ofNullable(genai);
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         * <p>
         * Enforces the {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} envelope check
         * first, then dispatches the three optional projection children to
         * {@link MetaVerified#of(Stanza)}, {@link MarketingMessages#of(Stanza)} and
         * {@link Genai#of(Stanza)}; a present child that fails to parse fails the whole parse
         * while a missing child is admitted.
         *
         * @implNote This implementation fails the success parse on any present-but-malformed
         * sub-projection, unlike the {@code GetLinkedAccounts} mixins which silently swallow
         * sub-parse failures.
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseSuccess",
                exports = "parseGetBusinessEligibilityResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            MetaVerified metaVerified = null;
            var metaVerifiedNode = stanza.getChild("meta_verified").orElse(null);
            if (metaVerifiedNode != null) {
                var parsed = MetaVerified.of(metaVerifiedNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                metaVerified = parsed.get();
            }
            MarketingMessages marketingMessages = null;
            var marketingNode = stanza.getChild("marketing_messages").orElse(null);
            if (marketingNode != null) {
                var parsed = MarketingMessages.of(marketingNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                marketingMessages = parsed.get();
            }
            Genai genai = null;
            var genaiNode = stanza.getChild("genai").orElse(null);
            if (genaiNode != null) {
                var parsed = Genai.of(genaiNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                genai = parsed.get();
            }
            return Optional.of(new Success(metaVerified, marketingMessages, genai));
        }

        /**
         * Compares this reply to another object for value equality across the three projections.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} with equal projections
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
            return Objects.equals(this.metaVerified, that.metaVerified)
                    && Objects.equals(this.marketingMessages, that.marketingMessages)
                    && Objects.equals(this.genai, that.genai);
        }

        /**
         * Returns a hash code derived from the three projections.
         *
         * @return the combined hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(metaVerified, marketingMessages, genai);
        }

        /**
         * Returns a debug rendering listing the three projections.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetBusinessEligibilityResponse.Success[metaVerified=" + metaVerified
                    + ", marketingMessages=" + marketingMessages
                    + ", genai=" + genai + ']';
        }

        /**
         * Carries the Meta-Verified eligibility status plus optional onboarding-flow toggles.
         * <p>
         * Drives the Meta-Verified compose-banner and privacy-interstitial onboarding flows;
         * the {@link #additionalParams()} string is an opaque relay payload propagated verbatim
         * to Meta's onboarding endpoint.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseSuccess")
        public static final class MetaVerified {
            /**
             * The mandatory {@code status} attribute on the {@code <meta_verified/>} child.
             */
            private final SmaxGetBusinessEligibilityFailSuccessStatus status;

            /**
             * The optional {@code should_show_privacy_interstitial_to_new_users} attribute.
             */
            private final SmaxGetBusinessEligibilityFalseTrueFlag shouldShowPrivacyInterstitialToNewUsers;

            /**
             * The optional {@code additional_params} attribute; an opaque relay-side payload
             * forwarded verbatim to Meta's onboarding endpoint.
             */
            private final String additionalParams;

            /**
             * Constructs a projection from the validated status and the two optional toggles.
             *
             * @param status                                  the eligibility status; never {@code null}
             * @param shouldShowPrivacyInterstitialToNewUsers the optional privacy-interstitial toggle; may be {@code null}
             * @param additionalParams                        the optional opaque params; may be {@code null}
             * @throws NullPointerException if {@code status} is {@code null}
             */
            public MetaVerified(SmaxGetBusinessEligibilityFailSuccessStatus status,
                                SmaxGetBusinessEligibilityFalseTrueFlag shouldShowPrivacyInterstitialToNewUsers,
                                String additionalParams) {
                this.status = Objects.requireNonNull(status, "status cannot be null");
                this.shouldShowPrivacyInterstitialToNewUsers = shouldShowPrivacyInterstitialToNewUsers;
                this.additionalParams = additionalParams;
            }

            /**
             * Returns the eligibility status.
             *
             * @return the status; never {@code null}
             */
            public SmaxGetBusinessEligibilityFailSuccessStatus status() {
                return status;
            }

            /**
             * Returns the optional privacy-interstitial toggle.
             *
             * @return an {@link Optional} carrying the toggle, or empty when the relay omitted the attribute
             */
            public Optional<SmaxGetBusinessEligibilityFalseTrueFlag> shouldShowPrivacyInterstitialToNewUsers() {
                return Optional.ofNullable(shouldShowPrivacyInterstitialToNewUsers);
            }

            /**
             * Returns the optional opaque onboarding params.
             *
             * @return an {@link Optional} carrying the params, or empty when the relay omitted the attribute
             */
            public Optional<String> additionalParams() {
                return Optional.ofNullable(additionalParams);
            }

            /**
             * Tries to parse the projection from the given stanza.
             * <p>
             * Asserts the {@code meta_verified} tag, projects the mandatory {@code status}
             * attribute through {@link SmaxGetBusinessEligibilityFailSuccessStatus#of(String)},
             * and only then optionally projects the privacy-interstitial flag through
             * {@link SmaxGetBusinessEligibilityFalseTrueFlag#of(String)}.
             *
             * @implNote This implementation treats a present-but-malformed privacy-interstitial
             * attribute as a parse failure rather than a silent ignore.
             * @param stanza the {@code <meta_verified/>} stanza
             * @return an {@link Optional} carrying the projection, or empty when the stanza does not match the documented schema
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseSuccess",
                    exports = "parseGetBusinessEligibilityResponseSuccessMetaVerified",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<MetaVerified> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("meta_verified")) {
                    return Optional.empty();
                }
                var statusStr = stanza.getAttributeAsString("status").orElse(null);
                var status = SmaxGetBusinessEligibilityFailSuccessStatus.of(statusStr).orElse(null);
                if (status == null) {
                    return Optional.empty();
                }
                SmaxGetBusinessEligibilityFalseTrueFlag flag = null;
                var flagStr = stanza.getAttributeAsString("should_show_privacy_interstitial_to_new_users").orElse(null);
                if (flagStr != null) {
                    flag = SmaxGetBusinessEligibilityFalseTrueFlag.of(flagStr).orElse(null);
                    if (flag == null) {
                        return Optional.empty();
                    }
                }
                var additional = stanza.getAttributeAsString("additional_params").orElse(null);
                return Optional.of(new MetaVerified(status, flag, additional));
            }

            /**
             * Compares this projection to another object for value equality across all three fields.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is a {@link MetaVerified} with equal fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (MetaVerified) obj;
                return this.status == that.status
                        && this.shouldShowPrivacyInterstitialToNewUsers
                                == that.shouldShowPrivacyInterstitialToNewUsers
                        && Objects.equals(this.additionalParams, that.additionalParams);
            }

            /**
             * Returns a hash code derived from all three fields.
             *
             * @return the combined hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(status, shouldShowPrivacyInterstitialToNewUsers, additionalParams);
            }

            /**
             * Returns a debug rendering listing the status, the privacy-interstitial toggle and the opaque params.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "SmaxGetBusinessEligibilityResponse.Success.MetaVerified[status=" + status
                        + ", shouldShowPrivacyInterstitialToNewUsers="
                        + shouldShowPrivacyInterstitialToNewUsers
                        + ", additionalParams=" + additionalParams + ']';
            }
        }

        /**
         * Carries the marketing-messages eligibility plus the optional expiration timestamp.
         * <p>
         * Drives the SMB marketing-messages broadcast-compose gating; the {@link #expiration()}
         * timestamp is the epoch-seconds deadline at which the eligibility window closes and the
         * caller should re-issue {@link SmaxGetBusinessEligibilityRequest}.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseSuccess")
        public static final class MarketingMessages {
            /**
             * The mandatory {@code status} attribute on the {@code <marketing_messages/>} child.
             */
            private final SmaxGetBusinessEligibilityMarketingMessagesStatus status;

            /**
             * The optional {@code expiration} attribute, in epoch seconds.
             */
            private final Integer expiration;

            /**
             * Constructs a projection from the validated status and optional expiration.
             *
             * @param status     the marketing-messages status; never {@code null}
             * @param expiration the optional expiration timestamp; may be {@code null}
             * @throws NullPointerException if {@code status} is {@code null}
             */
            public MarketingMessages(SmaxGetBusinessEligibilityMarketingMessagesStatus status, Integer expiration) {
                this.status = Objects.requireNonNull(status, "status cannot be null");
                this.expiration = expiration;
            }

            /**
             * Returns the marketing-messages status.
             *
             * @return the status; never {@code null}
             */
            public SmaxGetBusinessEligibilityMarketingMessagesStatus status() {
                return status;
            }

            /**
             * Returns the optional expiration timestamp in epoch seconds.
             *
             * @return an {@link OptionalInt} carrying the timestamp, or empty when the relay omitted the attribute
             */
            public OptionalInt expiration() {
                if (expiration == null) {
                    return OptionalInt.empty();
                }
                return OptionalInt.of(expiration);
            }

            /**
             * Tries to parse the projection from the given stanza.
             * <p>
             * Asserts the {@code marketing_messages} tag, projects the mandatory {@code status}
             * through {@link SmaxGetBusinessEligibilityMarketingMessagesStatus#of(String)}, and
             * then optionally parses the {@code expiration} attribute as a non-negative
             * {@code int}; a malformed or negative value is a parse failure.
             *
             * @param stanza the {@code <marketing_messages/>} stanza
             * @return an {@link Optional} carrying the projection, or empty when the stanza does not match the documented schema
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseSuccess",
                    exports = "parseGetBusinessEligibilityResponseSuccessMarketingMessages",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<MarketingMessages> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("marketing_messages")) {
                    return Optional.empty();
                }
                var statusStr = stanza.getAttributeAsString("status").orElse(null);
                var status = SmaxGetBusinessEligibilityMarketingMessagesStatus.of(statusStr).orElse(null);
                if (status == null) {
                    return Optional.empty();
                }
                Integer expiration = null;
                var expirationStr = stanza.getAttributeAsString("expiration").orElse(null);
                if (expirationStr != null) {
                    try {
                        var parsed = Integer.parseInt(expirationStr);
                        if (parsed < 0) {
                            return Optional.empty();
                        }
                        expiration = parsed;
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                }
                return Optional.of(new MarketingMessages(status, expiration));
            }

            /**
             * Compares this projection to another object for value equality across both fields.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is a {@link MarketingMessages} with equal fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (MarketingMessages) obj;
                return this.status == that.status && Objects.equals(this.expiration, that.expiration);
            }

            /**
             * Returns a hash code derived from both fields.
             *
             * @return the combined hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(status, expiration);
            }

            /**
             * Returns a debug rendering listing the status and expiration.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "SmaxGetBusinessEligibilityResponse.Success.MarketingMessages[status=" + status
                        + ", expiration=" + expiration + ']';
            }
        }

        /**
         * Carries the GenAI per-broadcast eligibility status.
         * <p>
         * Drives the SMB GenAI-text broadcast-compose gating.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseSuccess")
        public static final class Genai {
            /**
             * The mandatory {@code status} attribute on the {@code <genai/>} child.
             */
            private final SmaxGetBusinessEligibilityFailSuccessStatus status;

            /**
             * Constructs a projection from the validated status.
             *
             * @param status the GenAI status; never {@code null}
             * @throws NullPointerException if {@code status} is {@code null}
             */
            public Genai(SmaxGetBusinessEligibilityFailSuccessStatus status) {
                this.status = Objects.requireNonNull(status, "status cannot be null");
            }

            /**
             * Returns the GenAI status.
             *
             * @return the status; never {@code null}
             */
            public SmaxGetBusinessEligibilityFailSuccessStatus status() {
                return status;
            }

            /**
             * Tries to parse the projection from the given stanza.
             * <p>
             * Asserts the {@code genai} tag and projects the mandatory {@code status} through
             * {@link SmaxGetBusinessEligibilityFailSuccessStatus#of(String)}; a missing or
             * malformed status is a parse failure.
             *
             * @param stanza the {@code <genai/>} stanza
             * @return an {@link Optional} carrying the projection, or empty when the stanza does not match the documented schema
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseSuccess",
                    exports = "parseGetBusinessEligibilityResponseSuccessGenai",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<Genai> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("genai")) {
                    return Optional.empty();
                }
                var statusStr = stanza.getAttributeAsString("status").orElse(null);
                var status = SmaxGetBusinessEligibilityFailSuccessStatus.of(statusStr).orElse(null);
                if (status == null) {
                    return Optional.empty();
                }
                return Optional.of(new Genai(status));
            }

            /**
             * Compares this projection to another object for value equality on the status.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is a {@link Genai} with an equal status
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Genai) obj;
                return this.status == that.status;
            }

            /**
             * Returns a hash code derived from the status.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(status);
            }

            /**
             * Returns a debug rendering listing the status.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "SmaxGetBusinessEligibilityResponse.Success.Genai[status=" + status + ']';
            }
        }
    }

    /**
     * Carries a documented {@code 4xx} rejection.
     * <p>
     * Surfaced when the relay rejected the request via one of the BadRequest, Forbidden or
     * NotAllowed mixin arms.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup")
    final class ClientError implements SmaxGetBusinessEligibilityResponse {
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
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} and admits the entire
         * {@code 4xx} range as a catch-all.
         *
         * @implNote Unlike {@link SmaxGetAccountNonceResponse.ClientError} this variant does not
         * enforce a literal-pair disjunction, because the WA Web mixin group admits the entire
         * {@code 4xx} range as a catch-all.
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseError",
                exports = "parseGetBusinessEligibilityResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup",
                exports = "parseIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup",
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
            return "SmaxGetBusinessEligibilityResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a transient {@code 5xx} relay failure.
     * <p>
     * Surfaced when the relay returned a transient internal failure; callers can re-issue the
     * request with backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup")
    final class ServerError implements SmaxGetBusinessEligibilityResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageGetBusinessEligibilityResponseError",
                exports = "parseGetBusinessEligibilityResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup",
                exports = "parseIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup",
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
            return "SmaxGetBusinessEligibilityResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

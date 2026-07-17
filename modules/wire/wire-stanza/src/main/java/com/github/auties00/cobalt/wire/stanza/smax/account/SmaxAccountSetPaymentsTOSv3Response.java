package com.github.auties00.cobalt.wire.stanza.smax.account;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Models the reply produced by the relay for a
 * {@link SmaxAccountSetPaymentsTOSv3Request}.
 *
 * <p>The reply is either a {@link Success} acknowledging the acceptance or an
 * {@link Error} explaining why the acceptance was rejected. The {@link Success}
 * arm tells the caller it may persist the acceptance locally; the
 * {@link Error} arm carries one of six documented {@code (code, text)} pairs
 * that the caller logs and may surface in UI.
 */
public sealed interface SmaxAccountSetPaymentsTOSv3Response extends SmaxStanza.Response
        permits SmaxAccountSetPaymentsTOSv3Response.Success, SmaxAccountSetPaymentsTOSv3Response.Error {

    /**
     * Resolves an inbound IQ reply into the first matching response variant.
     *
     * <p>Called by the smax send pipeline after dispatching a
     * {@link SmaxAccountSetPaymentsTOSv3Request}. {@link Success} is tried
     * first and falls through to {@link Error} when the success schema does not
     * match. An empty result means the stanza is neither a documented success
     * nor a documented error.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound IQ stanza used for
     *                {@code id}/{@code from} echo checks; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or
     *         {@link Optional#empty()} when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxAccountSetPaymentsTOSv3RPC",
            exports = "sendSetPaymentsTOSv3RPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxAccountSetPaymentsTOSv3Response> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        return Error.of(stanza, request);
    }

    /**
     * Validates the {@code <iq>} envelope on an inbound reply by cross-checking
     * the description, {@code type}, {@code id} and {@code from} attributes
     * against the originating request.
     *
     * <p>Used by {@link Success#of(Stanza, Stanza)} and {@link Error#of(Stanza, Stanza)}
     * to gate further parsing; a failed envelope short-circuits the variant
     * before any payload inspection.
     *
     * @implNote
     * This implementation collapses the WA Web {@code parseIQResultResponseMixin}
     * and {@code parseIQErrorResponseMixin} into a single helper parametrised
     * on the expected {@code type} attribute.
     *
     * @param reply        the inbound IQ stanza
     * @param request      the originating outbound IQ stanza
     * @param expectedType the expected {@code type} attribute value; either
     *                     {@code "result"} or {@code "error"}
     * @return {@code true} when the envelope passes every echo check;
     *         {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WASmaxInAccountIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAccountIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean validateIqEnvelope(Stanza reply, Stanza request, String expectedType) {
        if (!reply.hasDescription("iq")) {
            return false;
        }
        if (!reply.hasAttribute("type", expectedType)) {
            return false;
        }
        var requestId = request.getAttributeAsString("id").orElse(null);
        if (requestId == null) {
            return false;
        }
        if (!reply.hasAttribute("id", requestId)) {
            return false;
        }
        var requestTo = request.getAttributeAsString("to").orElse(null);
        if (requestTo == null) {
            return false;
        }
        return reply.hasAttribute("from", requestTo);
    }

    /**
     * Models the positive reply variant carrying the relay-echoed acceptance
     * payload.
     *
     * <p>Surfaced to callers when the relay accepted a v3 payments-ToS
     * acceptance; carries the echoed service literal and notice list plus the
     * outage and sandbox markers that let callers decide whether to defer the
     * local persistence step.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInAccountSetPaymentsTOSv3ResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInAccountSetPaymentsTOSv3BRConsumerOrSetPaymentsTOSv3UPIConsumerPaymentsTOSv3ResponseMixinGroup")
    final class Success implements SmaxAccountSetPaymentsTOSv3Response {
        /**
         * Holds the accepted Brazilian-FBPAY notice literals enforced by the
         * success schema.
         *
         * <p>Used to reject {@code <additional_notice notice=...>} children
         * carrying a literal outside this set when the echoed {@code service}
         * attribute is {@code "FBPAY"}.
         */
        @WhatsAppWebExport(moduleName = "WASmaxInAccountEnums",
                exports = "ENUM_BRP2PCONSENT_BRPAYPRIVACYPOLICY_BRPAYTOS_BRPAYWATOS",
                adaptation = WhatsAppAdaptation.DIRECT)
        private static final Set<String> BR_CONSUMER_NOTICE_VALUES = Set.of(
                "br_p2p_consent",
                "br_pay_privacy_policy",
                "br_pay_tos",
                "br_pay_wa_tos");

        /**
         * Holds the accepted Indian-UPI notice literals enforced by the
         * success schema.
         *
         * <p>Used to reject {@code <additional_notice notice=...>} children
         * carrying a literal outside this set when the echoed {@code service}
         * attribute is {@code "UPI"}.
         */
        @WhatsAppWebExport(moduleName = "WASmaxInAccountEnums",
                exports = "ENUM_PAYTOSV3_UPIPAYPRIVACYPOLICY",
                adaptation = WhatsAppAdaptation.DIRECT)
        private static final Set<String> UPI_CONSUMER_NOTICE_VALUES = Set.of(
                "pay_tos_v3",
                "upi_pay_privacy_policy");

        /**
         * Holds whether the {@code <accept_pay outage="1"/>} marker was set by
         * the relay.
         *
         * <p>Read by callers wanting to defer persistence when the payments
         * backend is in a degraded state.
         */
        private final boolean outage;

        /**
         * Holds whether the {@code <accept_pay sandbox="1"/>} marker was set by
         * the relay.
         *
         * <p>Read by callers wanting to route the acceptance through a
         * sandbox-aware persistence path.
         */
        private final boolean sandbox;

        /**
         * Holds the synthesised consumer-variant name; one of
         * {@code "BRConsumerPaymentsTOSv3Response"} or
         * {@code "UPIConsumerPaymentsTOSv3Response"}.
         *
         * <p>Lets callers branch on the variant without re-reading the service
         * attribute.
         */
        private final String consumerVariantName;

        /**
         * Holds the echoed {@code service} literal; either {@code "FBPAY"}
         * (Brazilian) or {@code "UPI"} (Indian).
         *
         * <p>Mirrors the value
         * {@link SmaxAccountSetPaymentsTOSv3Request#toStanza()} stamped onto the
         * outbound stanza.
         */
        private final String service;

        /**
         * Holds the echoed {@code <additional_notice/>} list.
         *
         * <p>For {@code service="FBPAY"} each entry is one of
         * {@code "br_p2p_consent"}, {@code "br_pay_privacy_policy"},
         * {@code "br_pay_tos"}, or {@code "br_pay_wa_tos"}; for
         * {@code service="UPI"} each entry is one of {@code "pay_tos_v3"} or
         * {@code "upi_pay_privacy_policy"}.
         */
        private final List<String> additionalNotices;

        /**
         * Constructs a successful reply carrying the echoed payload.
         *
         * <p>Called by {@link #of(Stanza, Stanza)} after a successful parse; not
         * intended for direct caller use.
         *
         * @implNote
         * This implementation defensively copies the notice list and
         * substitutes an empty list when the input is {@code null} so the
         * {@link #additionalNotices()} accessor never returns {@code null}.
         *
         * @param outage              whether the outage marker was set
         * @param sandbox             whether the sandbox marker was set
         * @param consumerVariantName the synthesised variant name; never
         *                            {@code null}
         * @param service             the echoed service literal; never
         *                            {@code null}
         * @param additionalNotices   the echoed notice list; may be
         *                            {@code null}, treated as empty
         * @throws NullPointerException if {@code consumerVariantName} or
         *                              {@code service} is {@code null}
         */
        public Success(boolean outage,
                       boolean sandbox,
                       String consumerVariantName,
                       String service,
                       List<String> additionalNotices) {
            this.outage = outage;
            this.sandbox = sandbox;
            this.consumerVariantName = Objects.requireNonNull(consumerVariantName, "consumerVariantName cannot be null");
            this.service = Objects.requireNonNull(service, "service cannot be null");
            this.additionalNotices = List.copyOf(Objects.requireNonNullElse(additionalNotices, List.of()));
        }

        /**
         * Reports whether the {@code <accept_pay outage="1"/>} marker was set.
         *
         * <p>Drives whether callers defer the local payments-ToS persistence
         * write when the payments backend signals a partial outage.
         *
         * @return {@code true} when the marker was set
         */
        public boolean outage() {
            return outage;
        }

        /**
         * Reports whether the {@code <accept_pay sandbox="1"/>} marker was set.
         *
         * <p>Lets callers route the acceptance through a sandbox persistence
         * path during integration testing.
         *
         * @return {@code true} when the marker was set
         */
        public boolean sandbox() {
            return sandbox;
        }

        /**
         * Returns the synthesised consumer-variant name.
         *
         * <p>Reads {@code "BRConsumerPaymentsTOSv3Response"} or
         * {@code "UPIConsumerPaymentsTOSv3Response"} depending on the echoed
         * {@link #service()}; lets callers branch on a single field rather than
         * re-parsing the service literal.
         *
         * @return the variant name; never {@code null}
         */
        public String consumerVariantName() {
            return consumerVariantName;
        }

        /**
         * Returns the echoed payments-service literal.
         *
         * <p>Reads {@code "FBPAY"} for Brazilian terms or {@code "UPI"} for
         * Indian terms.
         *
         * @return the service literal; never {@code null}
         */
        public String service() {
            return service;
        }

        /**
         * Returns the echoed {@code <additional_notice notice=...>} literals.
         *
         * <p>Lets callers cross-check that the relay accepted every notice
         * originally submitted via the matching
         * {@link SmaxAccountSetPaymentsTOSv3ConsumerVariant}.
         *
         * @return an unmodifiable list of accepted literals; never {@code null}
         */
        public List<String> additionalNotices() {
            return additionalNotices;
        }

        /**
         * Parses a {@code Success} reply from the given inbound stanza
         * cross-checked against the originating request.
         *
         * <p>Returns {@link Optional#empty()} for any deviation from the
         * documented success schema (missing {@code <accept_pay/>}, unknown
         * {@code service} literal, empty or oversize notice list, or a notice
         * literal outside the service-specific enum).
         *
         * @implNote
         * This implementation tracks the BR-vs-UPI choice via the
         * {@code service} attribute and validates each notice against the
         * matching {@link #BR_CONSUMER_NOTICE_VALUES} or
         * {@link #UPI_CONSUMER_NOTICE_VALUES} set, replacing WA Web's separate
         * per-variant mixins with a single branch.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant, or
         *         {@link Optional#empty()} when the stanza does not match the
         *         success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInAccountSetPaymentsTOSv3ResponseSuccess",
                exports = "parseSetPaymentsTOSv3ResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInAccountSetPaymentsTOSv3BRConsumerOrSetPaymentsTOSv3UPIConsumerPaymentsTOSv3ResponseMixinGroup",
                exports = "parseSetPaymentsTOSv3BRConsumerOrSetPaymentsTOSv3UPIConsumerPaymentsTOSv3ResponseMixinGroup",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInAccountSetPaymentsTOSv3BRConsumerPaymentsTOSv3ResponseMixin",
                exports = {
                        "parseSetPaymentsTOSv3BRConsumerPaymentsTOSv3ResponseAdditionalNotice",
                        "parseSetPaymentsTOSv3BRConsumerPaymentsTOSv3ResponseMixin"
                },
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInAccountSetPaymentsTOSv3UPIConsumerPaymentsTOSv3ResponseMixin",
                exports = {
                        "parseSetPaymentsTOSv3UPIConsumerPaymentsTOSv3ResponseAdditionalNotice",
                        "parseSetPaymentsTOSv3UPIConsumerPaymentsTOSv3ResponseMixin"
                },
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!validateIqEnvelope(stanza, request, "result")) {
                return Optional.empty();
            }
            var acceptPay = stanza.getChild("accept_pay").orElse(null);
            if (acceptPay == null) {
                return Optional.empty();
            }
            var outageRaw = acceptPay.getAttributeAsString("outage").orElse(null);
            if (outageRaw != null && !"1".equals(outageRaw)) {
                return Optional.empty();
            }
            var outage = "1".equals(outageRaw);
            var sandboxRaw = acceptPay.getAttributeAsString("sandbox").orElse(null);
            if (sandboxRaw != null && !"1".equals(sandboxRaw)) {
                return Optional.empty();
            }
            var sandbox = "1".equals(sandboxRaw);
            String consumerVariantName;
            String service;
            Set<String> allowedNotices;
            if (acceptPay.hasAttribute("service", "FBPAY")) {
                consumerVariantName = "BRConsumerPaymentsTOSv3Response";
                service = "FBPAY";
                allowedNotices = BR_CONSUMER_NOTICE_VALUES;
            } else if (acceptPay.hasAttribute("service", "UPI")) {
                consumerVariantName = "UPIConsumerPaymentsTOSv3Response";
                service = "UPI";
                allowedNotices = UPI_CONSUMER_NOTICE_VALUES;
            } else {
                return Optional.empty();
            }
            var notices = acceptPay.streamChildren("additional_notice")
                    .map(child -> child.getAttributeAsString("notice").orElse(null))
                    .toList();
            if (notices.isEmpty() || notices.size() > 10) {
                return Optional.empty();
            }
            for (var notice : notices) {
                if (notice == null || !allowedNotices.contains(notice)) {
                    return Optional.empty();
                }
            }
            return Optional.of(new Success(outage, sandbox, consumerVariantName, service, notices));
        }

        /**
         * Compares this success reply to another for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Success} with
         *         identical markers, variant name, service literal, and notice
         *         list
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
            return this.outage == that.outage
                    && this.sandbox == that.sandbox
                    && Objects.equals(this.consumerVariantName, that.consumerVariantName)
                    && Objects.equals(this.service, that.service)
                    && Objects.equals(this.additionalNotices, that.additionalNotices);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(outage, sandbox, consumerVariantName, service, additionalNotices);
        }

        /**
         * Returns a debug-friendly representation of this success reply.
         *
         * <p>The format is intended for logging and is not part of any
         * contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxAccountSetPaymentsTOSv3Response.Success[outage=" + outage
                    + ", sandbox=" + sandbox
                    + ", consumerVariantName=" + consumerVariantName
                    + ", service=" + service
                    + ", additionalNotices=" + additionalNotices + ']';
        }
    }

    /**
     * Models the negative reply variant carrying the relay's rejection code
     * and human-readable text.
     *
     * <p>Surfaced to callers when the relay rejected the acceptance with one of
     * six documented error mixins: internal-server-error/500,
     * service-unavailable/503, pay-upgrade-required/443, config-mismatch/453,
     * forbidden/403, or bad-request/400. The synthesised {@link #variantName()}
     * disambiguates without re-parsing the code-text pair.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInAccountSetPaymentsTOSv3ResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInAccountSetPaymentsTosErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInAccountIQErrorBadRequestMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInAccountIQErrorConfigMismatchMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInAccountIQErrorForbiddenMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInAccountIQErrorInternalServerErrorMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInAccountIQErrorPayUpgradeRequiredMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInAccountIQErrorServiceUnavailableMixin")
    final class Error implements SmaxAccountSetPaymentsTOSv3Response {
        /**
         * Holds the numeric error code carried by the relay; one of
         * {@code 400}, {@code 403}, {@code 443}, {@code 453}, {@code 500}, or
         * {@code 503}.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the relay.
         *
         * <p>Carries the variant-specific literal (e.g. {@code "bad-request"},
         * {@code "internal-server-error"}) used to classify the error mixin.
         */
        private final String errorText;

        /**
         * Holds the synthesised variant name disambiguating which of the six
         * error mixins matched.
         *
         * <p>One of {@code "IQErrorBadRequest"}, {@code "IQErrorForbidden"},
         * {@code "IQErrorPayUpgradeRequired"}, {@code "IQErrorConfigMismatch"},
         * {@code "IQErrorInternalServerError"}, or
         * {@code "IQErrorServiceUnavailable"}.
         */
        private final String variantName;

        /**
         * Constructs an error reply with a pre-classified variant name.
         *
         * <p>Called by {@link #of(Stanza, Stanza)} after
         * {@link #classifyError(int, String)} matched the code-text pair; not
         * intended for direct caller use.
         *
         * @param errorCode   the numeric error code
         * @param errorText   the human-readable error text; may be {@code null}
         * @param variantName the classified variant name; never {@code null}
         * @throws NullPointerException if {@code variantName} is {@code null}
         */
        public Error(int errorCode, String errorText, String variantName) {
            this.errorCode = errorCode;
            this.errorText = errorText;
            this.variantName = Objects.requireNonNull(variantName, "variantName cannot be null");
        }

        /**
         * Returns the relay-supplied numeric error code.
         *
         * <p>Lets callers branch on the code (e.g. retry on {@code 503},
         * surface UI on {@code 400}/{@code 443}).
         *
         * @return the code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional relay-supplied error text.
         *
         * <p>The relay always sets a text for the six documented variants; the
         * wrapping {@link Optional} accommodates the undocumented case where the
         * text was omitted.
         *
         * @return an {@link Optional} carrying the text, or
         *         {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Returns the classified error-variant name.
         *
         * <p>Lets callers branch on a single field rather than re-parsing the
         * {@code (code, text)} pair against the six error mixins.
         *
         * @return the variant name; never {@code null}
         */
        public String variantName() {
            return variantName;
        }

        /**
         * Parses an {@code Error} reply from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} for any deviation from the
         * documented error schema (missing {@code <error/>} child, unparseable
         * {@code code} attribute, or a code-text pair outside the documented
         * six mixins).
         *
         * @implNote
         * This implementation derives {@link #variantName} via
         * {@link #classifyError(int, String)} which encodes the six documented
         * mixins as a static priority cascade.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant, or
         *         {@link Optional#empty()} when the stanza does not match any
         *         documented error variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInAccountSetPaymentsTOSv3ResponseError",
                exports = "parseSetPaymentsTOSv3ResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Error> of(Stanza stanza, Stanza request) {
            if (!validateIqEnvelope(stanza, request, "error")) {
                return Optional.empty();
            }
            var errorChild = stanza.getChild("error").orElse(null);
            if (errorChild == null) {
                return Optional.empty();
            }
            var codeOpt = errorChild.getAttributeAsInt("code");
            if (codeOpt.isEmpty()) {
                return Optional.empty();
            }
            var code = codeOpt.getAsInt();
            var text = errorChild.getAttributeAsString("text").orElse(null);
            var variantName = classifyError(code, text);
            if (variantName == null) {
                return Optional.empty();
            }
            return Optional.of(new Error(code, text, variantName));
        }

        /**
         * Classifies a {@code (code, text)} pair into one of the six documented
         * payments-ToS error mixin variants.
         *
         * <p>Returns {@code null} for any pair outside the six documented
         * mixins; the caller treats {@code null} as a parse failure.
         *
         * @implNote
         * This implementation runs the WA Web priority order verbatim
         * (internal-server-error, service-unavailable, pay-upgrade-required,
         * config-mismatch, forbidden, bad-request) so the synthesised variant
         * name is identical to WA Web's branch label even when multiple pairs
         * would match.
         *
         * @param code the numeric error code
         * @param text the human-readable error text; may be {@code null}
         * @return the variant name, or {@code null} when the pair is not one of
         *         the six documented mixins
         */
        @WhatsAppWebExport(moduleName = "WASmaxInAccountSetPaymentsTosErrors",
                exports = "parseSetPaymentsTosErrors",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInAccountIQErrorBadRequestMixin",
                exports = "parseIQErrorBadRequestMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInAccountIQErrorForbiddenMixin",
                exports = "parseIQErrorForbiddenMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInAccountIQErrorConfigMismatchMixin",
                exports = "parseIQErrorConfigMismatchMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInAccountIQErrorInternalServerErrorMixin",
                exports = "parseIQErrorInternalServerErrorMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInAccountIQErrorPayUpgradeRequiredMixin",
                exports = "parseIQErrorPayUpgradeRequiredMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInAccountIQErrorServiceUnavailableMixin",
                exports = "parseIQErrorServiceUnavailableMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private static String classifyError(int code, String text) {
            if (code == 500 && "internal-server-error".equals(text)) {
                return "IQErrorInternalServerError";
            }
            if (code == 503 && "service-unavailable".equals(text)) {
                return "IQErrorServiceUnavailable";
            }
            if (code == 443 && "upgrade-required".equals(text)) {
                return "IQErrorPayUpgradeRequired";
            }
            if (code == 453 && "config-mismatch".equals(text)) {
                return "IQErrorConfigMismatch";
            }
            if (code == 403 && "forbidden".equals(text)) {
                return "IQErrorForbidden";
            }
            if (code == 400 && "bad-request".equals(text)) {
                return "IQErrorBadRequest";
            }
            return null;
        }

        /**
         * Compares this error reply to another for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link Error} with
         *         identical code, text, and variant name
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Error) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText)
                    && Objects.equals(this.variantName, that.variantName);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText, variantName);
        }

        /**
         * Returns a debug-friendly representation of this error reply.
         *
         * <p>The format is intended for logging and is not part of any
         * contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxAccountSetPaymentsTOSv3Response.Error[errorCode=" + errorCode
                    + ", errorText=" + errorText
                    + ", variantName=" + variantName + ']';
        }
    }
}

package com.github.auties00.cobalt.wire.stanza.smax.util;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;

/**
 * Validates the standard {@code <iq from id type="result">} reply produced by every SMAX domain's
 * {@code Success} response variant.
 *
 * <p>Every per-RPC {@code Success.of(...)} factory runs {@link #validate(Stanza, Stanza)} before it
 * extracts the success payload. The check asserts the {@code iq} description, the {@code id} echo,
 * the {@code from}/{@code to} echo (only when the request carried a {@code to}), and
 * {@code type="result"}. This helper is the result-envelope counterpart of
 * {@link SmaxIqErrorResponseMixin}.
 *
 * @implNote
 * This implementation deduplicates the byte-identical WA Web {@code WASmaxIn*IQResultResponseMixin}
 * modules; the {@code @WhatsAppWebModule} list keeps the source manifest pointing at every upstream
 * module. The {@code from} echo is gated on the request carrying a {@code to} attribute because a
 * subset of groups RPCs address the group server implicitly and omit the attribute on the wire.
 */
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenHackBaseIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountHackBaseIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdHackBaseIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizLinkingIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizLinkingHackBaseIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageHackBaseIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBugReportingHackBaseIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizSettingsIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBotIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBrPaymentIQResultResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBugReportingIQResultResponseMixin")
public final class SmaxIqResultResponseMixin {

    /**
     * Refuses instantiation of the static-only utility.
     *
     * <p>The class exposes only static helpers; the throwing constructor guards against reflective
     * instantiation.
     */
    private SmaxIqResultResponseMixin() {
        throw new AssertionError("SmaxIqResultResponseMixin cannot be instantiated");
    }

    /**
     * Validates that the supplied reply is a well-formed {@code <iq type="result">} echoing the
     * request's {@code id} and {@code to} attributes.
     *
     * <p>Returns {@code true} when the reply has the {@code iq} description, carries
     * {@code type="result"}, echoes the request's {@code id} verbatim, and, when the request supplied
     * a {@code to}, carries a matching {@code from} attribute. A missing request {@code id} or any
     * mismatch yields {@code false}; a request without {@code to} skips the {@code from} comparison.
     *
     * @implNote
     * This implementation enforces the WA Web invariants in order: the description is {@code iq}, the
     * type is {@code result}, the request carries an {@code id} the reply echoes, and the reply's
     * {@code from} matches the request's {@code to} when that attribute is present.
     *
     * @param reply   the inbound stanza
     * @param request the outbound stanza, used to cross-check the echoed {@code id} and {@code from}
     *                attributes
     * @return {@code true} when {@code reply} is a result envelope echoing {@code request}'s
     *         identifiers; {@code false} otherwise
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenHackBaseIQResultResponseMixin",
            exports = "parseHackBaseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountHackBaseIQResultResponseMixin",
            exports = "parseHackBaseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdHackBaseIQResultResponseMixin",
            exports = "parseHackBaseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingHackBaseIQResultResponseMixin",
            exports = "parseHackBaseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageHackBaseIQResultResponseMixin",
            exports = "parseHackBaseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBugReportingHackBaseIQResultResponseMixin",
            exports = "parseHackBaseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBotIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBrPaymentIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBugReportingIQResultResponseMixin",
            exports = "parseIQResultResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public static boolean validate(Stanza reply, Stanza request) {
        Objects.requireNonNull(reply, "reply cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        if (!reply.hasDescription("iq")) {
            return false;
        }
        if (!reply.hasAttribute("type", "result")) {
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
        if (requestTo != null && !reply.hasAttribute("from", requestTo)) {
            return false;
        }
        return true;
    }
}

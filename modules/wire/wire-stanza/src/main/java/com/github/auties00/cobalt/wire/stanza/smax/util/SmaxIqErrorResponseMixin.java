package com.github.auties00.cobalt.wire.stanza.smax.util;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Validates the standard {@code <iq from id type="error">} reply produced by every SMAX domain's
 * {@code ClientError}/{@code ServerError} response variant.
 *
 * <p>{@link #validate(Stanza, Stanza)} checks the tag, the echoed {@code id}, the {@code from}/{@code to}
 * echo, and asserts {@code type="error"}; {@link #parseError(Stanza)} pulls the {@code <error code text/>}
 * child into an {@link Envelope} record. This helper is the error-envelope counterpart of
 * {@link SmaxIqResultResponseMixin}, and every per-domain {@code WASmaxIn*IQErrorResponseMixin}
 * delegates to it.
 *
 * @implNote
 * This implementation deduplicates the byte-identical WA Web error-response mixins, one per domain;
 * the {@code @WhatsAppWebModule} list keeps the source manifest pointing at every upstream module.
 */
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInAbPropsIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizLinkingIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizSettingsIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBotIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBugReportingIQErrorResponseMixin")
public final class SmaxIqErrorResponseMixin {

    /**
     * Refuses instantiation of the static-only utility.
     *
     * <p>The class exposes only static helpers; the throwing constructor guards against reflective
     * instantiation.
     */
    private SmaxIqErrorResponseMixin() {
        throw new AssertionError("SmaxIqErrorResponseMixin cannot be instantiated");
    }

    /**
     * Validates that the supplied reply is a well-formed {@code <iq type="error">} echoing the
     * request's {@code id} and {@code to} attributes.
     *
     * <p>Returns {@code true} only when {@code reply} has the {@code iq} description, carries
     * {@code type="error"}, echoes the request's {@code id} verbatim, and carries a {@code from}
     * attribute equal to the request's {@code to}. Any missing request {@code id} or {@code to}, or
     * any mismatch, yields {@code false}.
     * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} and
     * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} run this check before they extract
     * the {@code <error>} child via {@link #parseError(Stanza)}.
     *
     * @implNote
     * This implementation enforces the four WA Web invariants in order: the description is {@code iq},
     * the type is {@code error}, the request carries an {@code id} the reply echoes, and the reply's
     * {@code from} matches the request's {@code to}.
     *
     * @param reply   the inbound stanza
     * @param request the outbound stanza, used to cross-check the echoed {@code id} and {@code from}
     *                attributes
     * @return {@code true} when {@code reply} is an error envelope echoing {@code request}'s
     *         identifiers; {@code false} otherwise
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBotIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBugReportingIQErrorResponseMixin",
            exports = "parseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public static boolean validate(Stanza reply, Stanza request) {
        Objects.requireNonNull(reply, "reply cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        if (!reply.hasDescription("iq")) {
            return false;
        }
        if (!reply.hasAttribute("type", "error")) {
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
     * Extracts the {@code <error code text/>} child carried by an {@code <iq type="error">} envelope.
     *
     * <p>Returns {@link Optional#empty()} when the reply has no {@code <error>} child or when the
     * child is missing the {@code code} attribute or carries a negative code; otherwise returns the
     * parsed {@code (code, text)} pair as an {@link Envelope}.
     * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} and
     * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} call this after
     * {@link #validate(Stanza, Stanza)} succeeds.
     *
     * @implNote
     * This implementation treats {@code text} as optional but {@code code} as mandatory; a negative
     * or missing {@code code} yields {@link Optional#empty()} so the caller can fall through to a
     * different error branch.
     *
     * @param reply the inbound error stanza
     * @return an {@link Optional} carrying the parsed {@link Envelope}, or empty when the
     *         {@code <error>} child is missing or malformed
     * @throws NullPointerException if {@code reply} is {@code null}
     */
    public static Optional<Envelope> parseError(Stanza reply) {
        Objects.requireNonNull(reply, "reply cannot be null");
        var error = reply.getChild("error").orElse(null);
        if (error == null) {
            return Optional.empty();
        }
        var code = error.getAttributeAsInt("code").orElse(-1);
        if (code < 0) {
            return Optional.empty();
        }
        var text = error.getAttributeAsString("text").orElse(null);
        return Optional.of(new Envelope(code, text));
    }

    /**
     * Carries the {@code (code, text)} pair extracted from an {@code <error>} child.
     *
     * <p>{@link #parseError(Stanza)} produces this record and every per-RPC
     * {@code ClientError}/{@code ServerError} factory consumes it. The {@code code} is always
     * non-negative; the {@code text} may be {@code null} when the relay omitted it.
     *
     * @param code the numeric error code
     * @param text the human-readable error text, or {@code null} when the relay omitted it
     */
    public record Envelope(int code, String text) {
        /**
         * Returns the {@code text} component wrapped in an {@link Optional}.
         *
         * <p>Provides null-safe access to {@link #text()} for callers that prefer an
         * {@link Optional} over a possibly-{@code null} accessor.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> textAsOptional() {
            return Optional.ofNullable(text);
        }
    }
}

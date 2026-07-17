package com.github.auties00.cobalt.wire.stanza.smax.util;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;

/**
 * Validates the legacy {@code <iq id type="error">} reply produced by SMAX domains that pre-date the
 * standard echoed-{@code from} contract.
 *
 * <p>This relaxed envelope makes the {@code from} attribute optional and accepts either a domain JID
 * ({@code s.whatsapp.net}, {@code g.us}, {@code call}) or a user JID (phone, LID, interop, msgr, or
 * bot); the relay never has to echo the request's {@code to}. Two WA Web domains still use it: the
 * BizCtwaNativeAd upload-ad-media response error and the Privacy get-contact-blacklist response
 * error.
 *
 * @implNote
 * This implementation collapses both byte-identical WA Web clones into a single helper; the
 * {@code @WhatsAppWebModule} list keeps the source manifest pointing at both upstream modules.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdDeprecatedIQErrorResponseOptionalFromMixin")
@WhatsAppWebModule(moduleName = "WASmaxInPrivacyDeprecatedIQErrorResponseOptionalFromMixin")
public final class SmaxDeprecatedIqErrorResponseOptionalFromMixin {

    /**
     * Refuses instantiation of the static-only utility.
     *
     * <p>The class exposes only static helpers; the throwing constructor guards against reflective
     * instantiation.
     */
    private SmaxDeprecatedIqErrorResponseOptionalFromMixin() {
        throw new AssertionError("SmaxDeprecatedIqErrorResponseOptionalFromMixin cannot be instantiated");
    }

    /**
     * Validates that the supplied reply is a well-formed legacy {@code <iq type="error">} echoing the
     * request's {@code id} attribute.
     *
     * <p>Returns {@code true} when the reply has the {@code iq} description, carries
     * {@code type="error"}, echoes the request's {@code id} verbatim, and either omits the
     * {@code from} attribute or carries one that parses as a domain or user JID per
     * {@link #isDomainOrUserJid(String)}. A missing request {@code id}, a {@code from} that is neither
     * a domain nor a user JID, or a non-{@code error} type yields {@code false}.
     *
     * @implNote
     * This implementation hand-rolls the validation rather than delegating to
     * {@link SmaxIqErrorResponseMixin#validate(Stanza, Stanza)} because the legacy envelope makes
     * {@code from} optional and does not require an echo of the request's {@code to}.
     *
     * @param reply   the inbound stanza
     * @param request the outbound stanza, used to cross-check the echoed {@code id} attribute
     * @return {@code true} when {@code reply} is a legacy error envelope echoing {@code request}'s
     *         {@code id}; {@code false} otherwise
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdDeprecatedIQErrorResponseOptionalFromMixin",
            exports = "parseDeprecatedIQErrorResponseOptionalFromMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInPrivacyDeprecatedIQErrorResponseOptionalFromMixin",
            exports = "parseDeprecatedIQErrorResponseOptionalFromMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static boolean validate(Stanza reply, Stanza request) {
        Objects.requireNonNull(reply, "reply cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        if (!reply.hasDescription("iq")) {
            return false;
        }
        var requestId = request.getAttributeAsString("id").orElse(null);
        if (requestId == null) {
            return false;
        }
        if (!reply.hasAttribute("id", requestId)) {
            return false;
        }
        var from = reply.getAttributeAsString("from").orElse(null);
        if (from != null && !isDomainOrUserJid(from)) {
            return false;
        }
        return reply.hasAttribute("type", "error");
    }

    /**
     * Returns whether the supplied attribute value parses as a domain JID or as a user JID.
     *
     * <p>{@link #validate(Stanza, Stanza)} uses this to enforce the optional-{@code from} contract: the
     * relay may set {@code from} to a domain JID whose server is {@link JidServer#user()},
     * {@link JidServer#groupOrCommunity()}, or {@link JidServer#call()} with no user component, or to
     * any user JID that carries a user component.
     *
     * @implNote
     * This implementation catches every {@link RuntimeException} from {@link Jid#of(String)} so a
     * malformed attribute resolves to {@code false} rather than propagating; the domain-JID branch
     * requires {@link Jid#hasUser()} to be {@code false} while the user-JID branch requires it to be
     * {@code true}.
     *
     * @param value the raw attribute string
     * @return {@code true} when {@code value} is a domain or user JID; {@code false} otherwise
     */
    private static boolean isDomainOrUserJid(String value) {
        Jid jid;
        try {
            jid = Jid.of(value);
        } catch (RuntimeException e) {
            return false;
        }
        var server = jid.server();
        if (!jid.hasUser()
                && (server.equals(JidServer.user())
                || server.equals(JidServer.groupOrCommunity())
                || server.equals(JidServer.call()))) {
            return true;
        }
        return jid.hasUser();
    }
}

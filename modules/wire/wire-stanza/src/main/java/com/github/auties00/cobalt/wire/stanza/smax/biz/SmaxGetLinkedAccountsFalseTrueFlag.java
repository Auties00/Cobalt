package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import java.util.Locale;
import java.util.Optional;

/**
 * Models the {@code show_on_profile} boolean enum on the {@code <fb_page/>} and
 * {@code <ig_professional/>} children of the {@code GetLinkedAccounts} success reply.
 * <p>
 * The value controls whether the linked Facebook page or Instagram-professional identity is
 * shown on the business profile surface: {@link #TRUE} renders the badge and {@link #FALSE}
 * suppresses it. Only the case-insensitive wire literals {@code "true"} and {@code "false"}
 * are recognised.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizLinkingEnums")
public enum SmaxGetLinkedAccountsFalseTrueFlag {
    /**
     * Wire form {@code "false"}; the linked identity is hidden on the business profile surface.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingEnums",
            exports = "ENUM_FALSE_TRUE", adaptation = WhatsAppAdaptation.DIRECT)
    FALSE,
    /**
     * Wire form {@code "true"}; the linked identity is shown on the business profile surface.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingEnums",
            exports = "ENUM_FALSE_TRUE", adaptation = WhatsAppAdaptation.DIRECT)
    TRUE;

    /**
     * Resolves a wire-form value into the matching enum constant.
     * <p>
     * Accepts both the attribute and the element-content forms of the show-on-profile mixin.
     *
     * @implNote This implementation upper-cases the input under {@link Locale#ROOT} before
     * delegating to {@link #valueOf(String)} and swallows the resulting
     * {@link IllegalArgumentException} as an empty result.
     * @param value the attribute or element-content value; may be {@code null}
     * @return an {@link Optional} carrying the matching enum constant, or empty when the value
     *         is {@code null} or does not match a documented literal
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingEnums",
            exports = "ENUM_FALSE_TRUE", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxGetLinkedAccountsFalseTrueFlag> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxGetLinkedAccountsFalseTrueFlag.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

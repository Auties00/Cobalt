package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import java.util.Locale;
import java.util.Optional;

/**
 * Models the {@code state} enum carried by the {@code <whatsapp_as_page_button>} grandchild
 * of {@code <fb_page/>} in the {@code GetLinkedAccounts} success reply.
 * <p>
 * The value is the opt-in toggle for the WhatsApp-as-page-button overlay on the linked
 * Facebook page; the CTWA pipeline consults it to decide whether the page renders a
 * direct-message call-to-action. Only the case-insensitive wire literals {@code "off"} and
 * {@code "on"} are recognised.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizLinkingEnums")
public enum SmaxGetLinkedAccountsOffOnState {
    /**
     * Wire form {@code "off"}; the WhatsApp page button is disabled.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingEnums",
            exports = "ENUM_OFF_ON", adaptation = WhatsAppAdaptation.DIRECT)
    OFF,
    /**
     * Wire form {@code "on"}; the WhatsApp page button is enabled.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingEnums",
            exports = "ENUM_OFF_ON", adaptation = WhatsAppAdaptation.DIRECT)
    ON;

    /**
     * Resolves a wire-form attribute string into the matching enum constant.
     *
     * @implNote This implementation upper-cases the input under {@link Locale#ROOT} before
     * delegating to {@link #valueOf(String)} and swallows the resulting
     * {@link IllegalArgumentException} as an empty result.
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching enum constant, or empty when the value
     *         is {@code null} or does not match a documented literal
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingEnums",
            exports = "ENUM_OFF_ON", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxGetLinkedAccountsOffOnState> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxGetLinkedAccountsOffOnState.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

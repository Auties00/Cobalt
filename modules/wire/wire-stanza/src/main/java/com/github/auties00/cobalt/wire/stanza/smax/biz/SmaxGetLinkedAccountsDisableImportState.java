package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import java.util.Locale;
import java.util.Optional;

/**
 * Models the {@code state} enum carried by the {@code <profile_sync>} grandchild of
 * {@code <fb_page/>} and the {@code <catalog>} grandchild of {@code <fb_biz/>}.
 * <p>
 * The value distinguishes a paused or disabled sync from an in-progress import on the CTWA
 * linked-pages surface, driving whether the corresponding linked-page or linked-business
 * entry renders as inert or active. Only the case-insensitive wire literals {@code "disable"}
 * and {@code "import"} are recognised.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizLinkingEnums")
public enum SmaxGetLinkedAccountsDisableImportState {
    /**
     * Wire form {@code "disable"}; the sync is currently disabled and the entry is inert.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingEnums",
            exports = "ENUM_DISABLE_IMPORT", adaptation = WhatsAppAdaptation.DIRECT)
    DISABLE,
    /**
     * Wire form {@code "import"}; the sync is in progress or requested and the entry is active.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingEnums",
            exports = "ENUM_DISABLE_IMPORT", adaptation = WhatsAppAdaptation.DIRECT)
    IMPORT;

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
            exports = "ENUM_DISABLE_IMPORT", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxGetLinkedAccountsDisableImportState> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxGetLinkedAccountsDisableImportState.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

package com.github.auties00.cobalt.wire.stanza.usync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Enumerates the {@code context} values the {@code <usync>} stanza accepts.
 *
 * <p>The context steers per-protocol backoff in
 * {@link UsyncBackoff#waitForBackoff(UsyncQuery)}: {@link #INTERACTIVE} is used
 * for queries the user is waiting on, {@link #BACKGROUND} for idle batch syncs,
 * {@link #NOTIFICATION} for queries triggered by an inbound stanza, and
 * {@link #MESSAGE} or {@link #VOIP} when the resulting device list is needed to
 * encrypt an outbound send.
 *
 * @implNote
 * This implementation is the typed counterpart of the free-form strings WA Web
 * branches on; modelling them as an enum prevents typos that the relay would
 * silently accept.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
@WhatsAppWebModule(moduleName = "WAWebUsyncBackoff")
public enum UsyncContext {
    /**
     * Interactive context.
     *
     * <p>The user is waiting on the result;
     * {@link UsyncBackoff#waitForBackoff(UsyncQuery)} short-circuits without
     * sleeping for any protocol.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery", adaptation = WhatsAppAdaptation.DIRECT)
    INTERACTIVE("interactive"),

    /**
     * Outbound-message context.
     *
     * <p>The query is a prerequisite for encrypting an outgoing message; the
     * {@code devices} protocol is exempted from per-protocol backoff because
     * blocking it would block the send.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncBackoff",
            exports = "waitForBackoff", adaptation = WhatsAppAdaptation.DIRECT)
    MESSAGE("message"),

    /**
     * VoIP signalling context.
     *
     * <p>Shares the {@code devices}-protocol backoff exemption with
     * {@link #MESSAGE}; applied to USync queries triggered by call setup or
     * renegotiation.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncBackoff",
            exports = "waitForBackoff", adaptation = WhatsAppAdaptation.DIRECT)
    VOIP("voip"),

    /**
     * Background context.
     *
     * <p>The query is part of an idle batch sync; backoff is fully honoured.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery", adaptation = WhatsAppAdaptation.DIRECT)
    BACKGROUND("background"),

    /**
     * Notification context.
     *
     * <p>The query was triggered by an inbound notification stanza such as an
     * account-sync push; backoff is fully honoured.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery", adaptation = WhatsAppAdaptation.DIRECT)
    NOTIFICATION("notification");

    /**
     * The literal value emitted on the {@code context} attribute.
     */
    private final String wireValue;

    /**
     * Binds a new constant to its wire literal.
     *
     * @param wireValue the literal the relay expects on the {@code context}
     *                  attribute
     */
    UsyncContext(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the literal emitted on the {@code context} attribute of the
     * {@code <usync>} stanza.
     *
     * <p>Read by {@link UsyncQuery#toNode()} when serialising the IQ and by
     * {@link UsyncBackoff#waitForBackoff(UsyncQuery)} when deciding whether the
     * {@code devices} backoff applies.
     *
     * @return the wire literal
     */
    public String wireValue() {
        return wireValue;
    }
}

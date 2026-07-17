package com.github.auties00.cobalt.wire.stanza.usync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

/**
 * Enumerates the {@code mode} values the {@code <usync>} stanza accepts.
 *
 * <p>Builders default to {@link #QUERY}; {@link #DELTA} is used by contact-sync
 * jobs that ship just an add/remove diff against the relay's last-known roster.
 *
 * @implNote
 * This implementation is the typed counterpart of the free-form {@code mode}
 * string WA Web assigns; the JS surface accepts any string but only the four
 * literals modelled here are observed in the call graph.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
public enum UsyncMode {
    /**
     * Standard one-shot query mode.
     *
     * <p>The relay inlines the requested protocol data in the response.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery", adaptation = WhatsAppAdaptation.DIRECT)
    QUERY("query"),

    /**
     * Notify mode.
     *
     * <p>Asks the relay to perform a side effect without returning protocol
     * payloads in the response body.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery", adaptation = WhatsAppAdaptation.DIRECT)
    NOTIFY("notify"),

    /**
     * Delta mode.
     *
     * <p>Used by background contact-sync jobs that ship only the add/remove
     * diff against the relay's last-known roster, keyed by the per-user
     * device-list hashes attached through {@link UsyncUser#withDeviceHash(String)}
     * and {@link UsyncUser#withTimestamp(java.time.Instant)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery", adaptation = WhatsAppAdaptation.DIRECT)
    DELTA("delta"),

    /**
     * Result mode.
     *
     * <p>Used internally when the relay echoes a response already obtained from
     * a peer notification.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery", adaptation = WhatsAppAdaptation.DIRECT)
    RESULT("result");

    /**
     * The literal value emitted on the {@code mode} attribute.
     */
    private final String wireValue;

    /**
     * Binds a new constant to its wire literal.
     *
     * @param wireValue the literal the relay expects on the {@code mode}
     *                  attribute
     */
    UsyncMode(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the literal emitted on the {@code mode} attribute of the
     * {@code <usync>} stanza.
     *
     * <p>Call sites that bypass {@link UsyncQuery#toNode()} to assemble a raw
     * {@link Stanza} use this to match the exact
     * wire literal.
     *
     * @return the wire literal
     */
    public String wireValue() {
        return wireValue;
    }
}

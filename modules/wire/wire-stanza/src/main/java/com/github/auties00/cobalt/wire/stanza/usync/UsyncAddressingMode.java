package com.github.auties00.cobalt.wire.stanza.usync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

/**
 * Enumerates the {@code addressing_mode} values the USync {@code <contact>}
 * child accepts.
 *
 * <p>{@link #LID} is used when the local contact database has been migrated to
 * long identifiers and the relay should resolve and verify peers in the LID
 * space; {@link #PN} is used when the request still carries phone-number
 * {@link com.github.auties00.cobalt.wire.core.jid.Jid} values.
 *
 * @implNote
 * This implementation is the typed counterpart of the frozen
 * {@code USYNC_ADDRESSING_MODE} object whose values WA Web consumes by
 * branching on the raw {@code "pn"} and {@code "lid"} strings.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
public enum UsyncAddressingMode {
    /**
     * Phone-number addressing.
     *
     * <p>The {@code addressing_mode} attribute is dropped from the wire when
     * this value is selected.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USYNC_ADDRESSING_MODE", adaptation = WhatsAppAdaptation.DIRECT)
    PN("pn"),

    /**
     * LID addressing.
     *
     * <p>Selects the {@code @lid} identifier space and forces
     * {@code addressing_mode="lid"} onto the {@code <contact>} query element.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USYNC_ADDRESSING_MODE", adaptation = WhatsAppAdaptation.DIRECT)
    LID("lid");

    /**
     * The literal value emitted on the {@code addressing_mode} attribute.
     */
    private final String wireValue;

    /**
     * Binds a new constant to its wire literal.
     *
     * @param wireValue the literal the relay expects on the wire
     */
    UsyncAddressingMode(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the literal emitted on the {@code addressing_mode} attribute of
     * the {@code <contact>} query element.
     *
     * <p>Call sites that build raw {@link Stanza}
     * stanzas without going through {@link UsyncQuery} use this to mirror the
     * exact attribute value.
     *
     * @return the wire literal
     */
    public String wireValue() {
        return wireValue;
    }
}

package com.github.auties00.cobalt.wire.stanza.iq.debug;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import java.util.Optional;

/**
 * Enumerates the GDPR data-export scopes addressed by the {@code <gdpr>} stanza family.
 *
 * <p>One constant is bound into each {@link IqDebugGdprRequest}; the constant determines
 * the {@code report_type} attribute emitted on the outbound {@code <gdpr>} child.
 * {@link #ACCOUNT} covers the full account export (transcripts, settings, profile) and
 * carries no wire string, so {@link IqDebugGdprRequest#toStanza()} omits the
 * {@code report_type} attribute entirely. {@link #NEWSLETTERS} narrows the export to the
 * user's channel follow graph and channel-post history and serialises as
 * {@code report_type="newsletters"}.
 */
@WhatsAppWebModule(moduleName = "WAWebGdprConstants")
public enum IqDebugGdprReportType {
    /**
     * Indicates a full-account GDPR data export covering transcripts, settings, and profile.
     *
     * <p>Carries no wire string, so the {@code report_type} attribute is omitted from the
     * outbound {@code <gdpr>} child.
     */
    @WhatsAppWebExport(moduleName = "WAWebGdprConstants",
            exports = "ReportType.Account",
            adaptation = WhatsAppAdaptation.DIRECT)
    ACCOUNT(null),

    /**
     * Indicates a channel-only GDPR data export covering the channel follow graph and the
     * user's own channel posts.
     *
     * <p>Serialises as the literal {@code report_type="newsletters"} attribute.
     */
    @WhatsAppWebExport(moduleName = "WAWebGdprConstants",
            exports = "ReportType.Newsletters",
            adaptation = WhatsAppAdaptation.DIRECT)
    NEWSLETTERS("newsletters");

    /**
     * Holds the literal {@code report_type} attribute value, or {@code null} when the
     * attribute is to be omitted from the outbound {@code <gdpr>} stanza.
     */
    private final String wire;

    /**
     * Constructs a report-type constant bound to the given wire string.
     *
     * @param wire the literal attribute value, or {@code null} to omit the
     *             {@code report_type} attribute entirely
     */
    IqDebugGdprReportType(String wire) {
        this.wire = wire;
    }

    /**
     * Returns the literal {@code report_type} wire string, if any.
     *
     * <p>An empty {@link Optional} marks the omit-attribute branch consumed by
     * {@link IqDebugGdprRequest#toStanza()}; a present value is routed verbatim onto the
     * outbound {@code <gdpr>} child.
     *
     * @return an {@link Optional} carrying the literal wire string, or empty when the
     *         attribute is to be omitted
     */
    public Optional<String> wire() {
        return Optional.ofNullable(wire);
    }
}

package com.github.auties00.cobalt.wire.stanza.iq.debug;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Builds the outbound {@code <iq xmlns="urn:xmpp:whatsapp:account" type="get">} stanza that
 * cancels an in-flight GDPR data-export request for the bound report type.
 *
 * <p>The IQ wraps a {@code <gdpr action="delete" [report_type=?]/>} child whose
 * {@code report_type} attribute is the {@link IqDebugGdprReportType#wire()} value, omitted
 * entirely for {@link IqDebugGdprReportType#ACCOUNT}. The stanza is routed to
 * {@link JidServer#user()} with a fixed {@code type="get"}; the IQ {@code id} attribute is
 * left for the dispatch layer to assign. The relay reply is parsed by
 * {@link IqDebugGdprResponse}.
 */
@WhatsAppWebModule(moduleName = "WAWebGdprHookUtils")
public final class IqDebugGdprRequest implements IqStanza.Request {
    /**
     * Holds the {@link IqDebugGdprReportType} whose export is being cancelled.
     */
    private final IqDebugGdprReportType reportType;

    /**
     * Constructs a cancel-GDPR request bound to the given report type.
     *
     * <p>The {@code reportType} must match the report type of the previously issued GDPR
     * request; cancelling with a mismatched type yields a {@code 404} surfaced as
     * {@link IqDebugGdprResponse.ClientError}.
     *
     * @param reportType the report type to cancel; never {@code null}
     * @throws NullPointerException if {@code reportType} is {@code null}
     */
    public IqDebugGdprRequest(IqDebugGdprReportType reportType) {
        this.reportType = Objects.requireNonNull(reportType, "reportType cannot be null");
    }

    /**
     * Returns the bound {@link IqDebugGdprReportType}.
     *
     * @return the report type being cancelled; never {@code null}
     */
    public IqDebugGdprReportType reportType() {
        return reportType;
    }

    /**
     * Builds the outbound {@code <iq>} stanza wrapping the
     * {@code <gdpr action="delete" [report_type=?]/>} payload.
     *
     * <p>The {@code report_type} attribute is emitted only when
     * {@link IqDebugGdprReportType#wire()} is present. The returned builder is wire-ready
     * except for the IQ {@code id} attribute, which the dispatch layer assigns.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <gdpr>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDebugGDPR",
            exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebGdprHookUtils",
            exports = "getGdprIq", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var gdprBuilder = new StanzaBuilder()
                .description("gdpr")
                .attribute("action", "delete");
        var wireType = reportType.wire().orElse(null);
        if (wireType != null) {
            gdprBuilder.attribute("report_type", wireType);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "urn:xmpp:whatsapp:account")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(gdprBuilder.build());
    }

    /**
     * Compares this request with another for equality by bound report type.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an {@link IqDebugGdprRequest} with the same
     *         {@link #reportType()}, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqDebugGdprRequest) obj;
        return this.reportType == that.reportType;
    }

    /**
     * Returns a hash code derived from the bound report type.
     *
     * @return the hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(reportType);
    }

    /**
     * Returns a debug string carrying the bound report type.
     *
     * @return a human-readable representation of this request
     */
    @Override
    public String toString() {
        return "IqDebugGdprRequest[reportType=" + reportType + ']';
    }
}

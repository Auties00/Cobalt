package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX request that fetches the list of user-submitted reports filed against newsletters
 * the local user administers.
 *
 * <p>This request drives the channel-reports moderation surface: the UI lists each pending report
 * under {@code data.xwa2_channels_reports.channels_reports} together with reporter metadata, status,
 * and any appeal information. The request carries a single {@code locale} variable that localises the
 * appeal-reason labels the relay attaches to each report; submit it through the MEX IQ dispatcher and
 * pair the result with {@link FetchNewsletterReportsMexResponse#of(Stanza)}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterReportsJob")
public final class FetchNewsletterReportsMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of
     * {@code WAWebMexFetchNewsletterReportsJobQuery.graphql} on the WhatsApp relay.
     *
     * <p>Sent as the {@code id} attribute of the outgoing {@code <query>} child; the WhatsApp relay
     * refuses requests whose persisted-query id is unknown.
     */
    public static final String QUERY_ID = "35936238352686172";

    /**
     * Holds the GraphQL operation name reported by WA Web's {@code MexPerfTracker} for this query.
     */
    public static final String OPERATION_NAME = "mexFetchNewsletterReports";

    /**
     * Holds the locale tag the relay uses when localising the appeal-reason labels attached to each
     * report.
     */
    private final String locale;

    /**
     * Constructs a request that localises its report labels under the given locale.
     *
     * <p>The {@code locale} is written as the sole GraphQL variable; WhatsApp Web always supplies it
     * from the active application locale.
     *
     * @param locale the locale tag for localised appeal-reason labels
     */
    public FetchNewsletterReportsMexRequest(String locale) {
        this.locale = locale;
    }

    /**
     * Returns {@link #QUERY_ID}.
     *
     * @return the persisted-query identifier of this query
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * Returns {@link #OPERATION_NAME}.
     *
     * @return the GraphQL operation name of this query
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * Serialises this request into a MEX IQ {@link StanzaBuilder} ready to be dispatched through the
     * WhatsApp relay.
     *
     * <p>Produces the {@code {variables: {locale}}} payload consumed by the persisted-query
     * identified by {@link #QUERY_ID}.
     *
     * @implNote This implementation writes the GraphQL variables directly through {@link JSONWriter}
     * and delegates IQ envelope construction to {@link Json#createMexNode(String, String)}; any
     * {@link IOException} raised by the in-memory writer is wrapped in an {@link UncheckedIOException}
     * since neither sink can fail in practice.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewsletterReportsJob", exports = "mexFetchNewsletterReports",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("locale");
            writer.writeColon();
            writer.writeString(locale);
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return Json.createMexNode(QUERY_ID, output.toString());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

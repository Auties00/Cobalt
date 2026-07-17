package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX request that fetches metadata for every newsletter followed by the authenticated
 * user.
 *
 * <p>This query drives the local newsletter list hydration run during login and periodic syncs; its
 * response is split into active channels and those the relay reports as deleted by
 * {@link FetchAllNewslettersMetadataMexResponse#partition()}. The two gating variables control
 * whether the response carries the optional {@code wamo_sub} (paid newsletter subscription) and
 * {@code status_metadata} fragments.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchAllNewslettersMetadataJob")
public final class FetchAllNewslettersMetadataMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of
     * {@code WAWebMexFetchAllNewslettersMetadataJobQuery.graphql} on the WhatsApp relay.
     *
     * <p>Sent as the {@code query_id} attribute of the outgoing {@code <query>} child.
     */
    public static final String QUERY_ID = "25399611239711790";

    /**
     * Holds the GraphQL operation name reported by WhatsApp Web's MEX perf tracker for this query.
     */
    public static final String OPERATION_NAME = "mexFetchAllNewsletters";

    /**
     * Holds the value of the {@code fetch_wamo_sub} GraphQL variable.
     */
    private final boolean fetchWamoSub;

    /**
     * Holds the value of the {@code fetch_status_metadata} GraphQL variable.
     */
    private final boolean fetchStatusMetadata;

    /**
     * Constructs a request that selects the {@code fetch_wamo_sub} gating flag with
     * {@code fetch_status_metadata} defaulted to {@code false}.
     *
     * @param fetchWamoSub the value of the {@code fetch_wamo_sub} variable
     */
    public FetchAllNewslettersMetadataMexRequest(boolean fetchWamoSub) {
        this(fetchWamoSub, false);
    }

    /**
     * Constructs a request with both GraphQL gating variables.
     *
     * @param fetchWamoSub        the value of the {@code fetch_wamo_sub} variable
     * @param fetchStatusMetadata the value of the {@code fetch_status_metadata} variable
     */
    public FetchAllNewslettersMetadataMexRequest(boolean fetchWamoSub, boolean fetchStatusMetadata) {
        this.fetchWamoSub = fetchWamoSub;
        this.fetchStatusMetadata = fetchStatusMetadata;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #QUERY_ID}.
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #OPERATION_NAME}.
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code {variables: {fetch_wamo_sub, fetch_status_metadata}}} payload. Both
     * gating booleans are always emitted: the compiled query declares both as required variables and
     * WhatsApp Web always sends both (coalescing an unset flag to {@code false}), so omitting either
     * makes the relay reject the request as {@code 400 Bad Request}.
     *
     * @implNote This implementation writes the GraphQL variables directly through a
     * {@link JSONWriter} and wraps any {@link IOException} from the in-memory writer in an
     * {@link UncheckedIOException}.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchAllNewslettersMetadataJob", exports = "mexFetchAllNewsletters",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("fetch_wamo_sub");
            writer.writeColon();
            writer.writeBool(fetchWamoSub);
            writer.writeName("fetch_status_metadata");
            writer.writeColon();
            writer.writeBool(fetchStatusMetadata);
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

package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

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
 * Fetches a rich link preview for a URL shared in a newsletter message, with the relay acting as a
 * trusted unfurl proxy.
 *
 * <p>The relay performs the URL unfurl server-side so the preview does not leak reader identity to
 * the link target, and returns a {@code xwa2_newsletter_link_preview} envelope carrying the title,
 * description and thumbnail handle. The {@link #input} variable is forwarded as an opaque
 * caller-supplied JSON string and is always materialised on the wire.
 *
 * @implNote This implementation leaves URL validation to the caller (WhatsApp Web validates the URL
 * before sending {@code {"url":"..."}}) because the codegen pipeline does not model the newsletter
 * link-preview action.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchPlaintextLinkPreviewJob")
public final class FetchPlaintextLinkPreviewMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled GraphQL query identifier for the plaintext link-preview query document.
     *
     * <p>The relay maps this identifier to a server-side persisted operation and never sees the
     * GraphQL text on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchPlaintextLinkPreviewJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "9101130456653613";

    /**
     * Holds the GraphQL operation name reported to the MEX perf tracker when this query is
     * dispatched.
     *
     * <p>The name tags the query in latency and error metrics; it is kept on the request so
     * embedders mirroring that telemetry surface can emit the same tag.
     */
    public static final String OPERATION_NAME = "fetchPlaintextLinkPreview";

    /**
     * Holds the serialised URL and optional preview options bound to the {@code input} GraphQL
     * variable.
     */
    private final String input;

    /**
     * Constructs a new request with the serialised {@code input} GraphQL variable.
     *
     * <p>The caller produces the JSON payload, which is always emitted on the wire.
     *
     * @param input the serialised {@code input} JSON payload
     */
    public FetchPlaintextLinkPreviewMexRequest(String input) {
        this.input = input;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation streams the GraphQL variables through fastjson2's
     * {@link JSONWriter}, always materialising the declared {@code input} string, then wraps the
     * payload via {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchPlaintextLinkPreviewJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.writeString(input);
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

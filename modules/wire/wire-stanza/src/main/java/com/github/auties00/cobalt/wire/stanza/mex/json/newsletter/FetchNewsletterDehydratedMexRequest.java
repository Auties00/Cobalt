package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Builds the MEX request that fetches a dehydrated representation of a newsletter.
 *
 * <p>This query is the lightweight newsletter lookup used to verify subscription count, paid
 * subscription state and verification status without triggering a full metadata hydration. The
 * {@code key} may be a newsletter Jid or an invite token; the {@code type} discriminator is derived
 * from {@link Jid#hasNewsletterServer()}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterDehydratedJob")
public final class FetchNewsletterDehydratedMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of
     * {@code WAWebMexFetchNewsletterDehydratedJobQuery.graphql} on the WhatsApp relay.
     *
     * <p>Sent as the {@code query_id} attribute of the outgoing {@code <query>} child.
     */
    public static final String QUERY_ID = "30328461880085868";

    /**
     * Holds the GraphQL operation name reported by WhatsApp Web's MEX perf tracker for this query.
     */
    public static final String OPERATION_NAME = "mexGetNewsletterDehydrated";

    /**
     * Holds the newsletter Jid (when fetching by id) or invite token (when fetching by invite).
     */
    private final Jid key;

    /**
     * Holds the {@code view_role} GraphQL variable that selects which viewer-role fragment the relay
     * populates.
     */
    private final String viewRole;

    /**
     * Indicates whether the response should carry the optional {@code wamo_sub} fragment selections.
     */
    private final boolean fetchWamoSub;

    /**
     * Constructs a request for the dehydrated representation of the given newsletter key.
     *
     * <p>Passing a newsletter-server Jid looks up by id (the {@code type} discriminator becomes
     * {@code "JID"}); passing any other Jid looks up by invite token (the discriminator becomes
     * {@code "INVITE"}).
     *
     * @param key          the newsletter Jid or invite identifier
     * @param viewRole     the {@code view_role} GraphQL variable
     * @param fetchWamoSub whether to request the optional {@code wamo_sub} fragment selections
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public FetchNewsletterDehydratedMexRequest(Jid key, String viewRole, boolean fetchWamoSub) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.viewRole = viewRole;
        this.fetchWamoSub = fetchWamoSub;
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
     * <p>Produces the {@code {variables: {input: {key, type, view_role}, fetch_wamo_sub}}} payload;
     * the {@code type} discriminator is derived from {@link Jid#hasNewsletterServer()} so newsletter
     * Jids resolve to {@code "JID"} and any other Jid resolves to {@code "INVITE"}.
     *
     * @implNote This implementation writes the GraphQL variables directly through a
     * {@link JSONWriter} and wraps any {@link IOException} from the in-memory writer in an
     * {@link UncheckedIOException}.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewsletterDehydratedJob", exports = "mexGetNewsletterDehydrated",
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
            writer.startObject();
            writer.writeName("key");
            writer.writeColon();
            writer.writeString(key.toString());
            writer.writeName("type");
            writer.writeColon();
            writer.writeString(key.hasNewsletterServer() ? "JID" : "INVITE");
            writer.writeName("view_role");
            writer.writeColon();
            writer.writeString(viewRole);
            writer.endObject();

            writer.writeName("fetch_wamo_sub");
            writer.writeColon();
            writer.writeBool(fetchWamoSub);

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

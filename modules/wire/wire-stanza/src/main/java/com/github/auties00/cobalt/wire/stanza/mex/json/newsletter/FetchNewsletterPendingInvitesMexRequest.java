package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX request that lists pending admin invites awaiting acceptance on a newsletter.
 *
 * <p>This request drives the admin-management surface where the owner inspects the users who have
 * been invited as admins but have not yet accepted. WA Web additionally gates the call behind its
 * newsletter-creation-enabled check and skips it when newsletter creation is disabled.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterPendingInvitesJob")
public final class FetchNewsletterPendingInvitesMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of
     * {@code WAWebMexFetchNewsletterPendingInvitesJobQuery.graphql} on the WhatsApp relay.
     *
     * <p>Sent as the {@code id} attribute of the outgoing {@code <query>} child.
     */
    public static final String QUERY_ID = "9783111038412085";

    /**
     * Holds the GraphQL operation name reported by WA Web's {@code MexPerfTracker} for this query.
     */
    public static final String OPERATION_NAME = "mexFetchNewsletterPendingInvites";

    /**
     * Holds the newsletter Jid whose pending admin invites are being requested.
     */
    private final String newsletterId;

    /**
     * Constructs a request for the pending admin invites of the given newsletter.
     *
     * <p>The {@code newsletter_id} variable is a declared top-level variable that is always emitted,
     * so callers supply a non-null value.
     *
     * @param newsletterId the newsletter Jid
     */
    public FetchNewsletterPendingInvitesMexRequest(String newsletterId) {
        this.newsletterId = newsletterId;
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
     * Serialises this request into a MEX IQ {@link StanzaBuilder}.
     *
     * <p>Produces the {@code {variables: {newsletter_id}}} payload; the {@code newsletter_id}
     * variable is the sole declared top-level variable and is always emitted.
     *
     * @implNote This implementation writes the GraphQL variables directly through {@link JSONWriter}
     * and wraps any {@link IOException} from the in-memory writer in an {@link UncheckedIOException}.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewsletterPendingInvitesJob", exports = "mexFetchNewsletterPendingInvites",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("newsletter_id");
            writer.writeColon();
            writer.writeString(newsletterId);
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

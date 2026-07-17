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
 * Builds the MEX request that demotes a newsletter administrator back to a regular follower.
 *
 * <p>Only the newsletter owner may demote other admins, and an admin may demote themselves. The
 * target user keeps follower membership but loses admin-only capabilities such as publishing or
 * moderating. The matching reply is parsed by {@link DemoteNewsletterAdminMexResponse}.
 *
 * @implNote This implementation expects the caller to have already converted the target user's Jid
 * to its LID string.
 */
@WhatsAppWebModule(moduleName = "WAWebMexDemoteNewsletterAdminJob")
public final class DemoteNewsletterAdminMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of this mutation on the WhatsApp relay.
     *
     * <p>Emitted as the {@code query_id} attribute of the outgoing {@code <query>} child.
     */
    public static final String QUERY_ID = "9880997548630971";

    /**
     * Holds the GraphQL operation name reported by WhatsApp Web's MEX perf tracker for this
     * mutation.
     */
    public static final String OPERATION_NAME = "demoteNewsletterAdmin";

    /**
     * Holds the Jid string of the newsletter on which the demotion is taking place.
     */
    private final String newsletterId;

    /**
     * Holds the user LID string of the admin being demoted.
     */
    private final String userId;

    /**
     * Constructs a request that demotes the given user on the given newsletter.
     *
     * <p>The {@code userId} parameter must be the user LID string.
     *
     * @param newsletterId the newsletter Jid where the demotion is applied
     * @param userId       the user LID of the admin being demoted
     */
    public DemoteNewsletterAdminMexRequest(String newsletterId, String userId) {
        this.newsletterId = newsletterId;
        this.userId = userId;
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
     * <p>Produces the {@code {variables: {newsletter_id, user_id}}} payload; both are declared
     * top-level variables and are always emitted.
     *
     * @implNote This implementation writes the GraphQL variables directly through a
     * {@link JSONWriter} and wraps any {@link IOException} from the in-memory writer in an
     * {@link UncheckedIOException}.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexDemoteNewsletterAdminJob", exports = "demoteNewsletterAdmin",
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
            writer.writeName("user_id");
            writer.writeColon();
            writer.writeString(userId);
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

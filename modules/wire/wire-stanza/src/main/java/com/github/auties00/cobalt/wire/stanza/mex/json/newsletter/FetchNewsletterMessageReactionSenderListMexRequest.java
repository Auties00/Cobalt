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
import java.util.Objects;

/**
 * Builds the MEX request that fetches the reaction-sender list for a specific newsletter message.
 *
 * <p>Backs the admin reactions inspector where the owner taps a reaction count under a newsletter
 * message to see which subscribers used each emoji. The matching response, parsed by
 * {@link FetchNewsletterMessageReactionSenderListMexResponse}, groups senders by reaction code.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterMessageReactionSenderListJob")
public final class FetchNewsletterMessageReactionSenderListMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of
     * {@code WAWebMexFetchNewsletterMessageReactionSenderListJobQuery.graphql} on the WhatsApp
     * relay.
     *
     * <p>Sent as the {@code query_id} attribute of the outgoing {@code <query>} child.
     */
    public static final String QUERY_ID = "29575462448733991";

    /**
     * Holds the GraphQL operation name reported by WhatsApp Web's MEX perf tracker for this query.
     */
    public static final String OPERATION_NAME = "mexFetchNewsletterMessageReactionSenderList";

    /**
     * Holds the newsletter Jid that owns the target message.
     */
    private final String newsletterId;

    /**
     * Holds the server-assigned identifier of the target message.
     */
    private final long serverId;

    /**
     * Constructs a request that targets the reaction senders for the message identified by the
     * newsletter Jid and server id pair.
     *
     * <p>Both fields are mandatory; the caller passes the newsletter Jid string with no
     * {@code Jid} wrapping and the canonical server message id as returned in the message envelope.
     *
     * @param newsletterId the newsletter Jid
     * @param serverId     the server-assigned message identifier
     * @throws NullPointerException if {@code newsletterId} is {@code null}
     */
    public FetchNewsletterMessageReactionSenderListMexRequest(String newsletterId, long serverId) {
        this.newsletterId = Objects.requireNonNull(newsletterId, "newsletterId");
        this.serverId = serverId;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the value of {@link #QUERY_ID}.
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the value of {@link #OPERATION_NAME}.
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code {variables: {input: {id, server_id}}}} payload.
     *
     * @implNote This implementation writes the GraphQL variables directly through a
     * {@link JSONWriter} and wraps any {@link IOException} from the in-memory writer in an
     * {@link UncheckedIOException}.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewsletterMessageReactionSenderListJob", exports = "mexFetchNewsletterMessageReactionSenderList",
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
            writer.writeName("id");
            writer.writeColon();
            writer.writeString(newsletterId);
            writer.writeName("server_id");
            writer.writeColon();
            writer.writeInt64(serverId);
            writer.endObject();
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

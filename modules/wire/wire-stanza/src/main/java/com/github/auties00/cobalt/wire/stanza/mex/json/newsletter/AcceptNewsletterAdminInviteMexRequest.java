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
 * Builds the MEX request that accepts a pending newsletter admin invite.
 *
 * <p>The invitee submits this mutation against the newsletter that issued the invite; on success
 * the relay promotes the local user to admin membership on that newsletter. The matching reply is
 * parsed by {@link AcceptNewsletterAdminInviteMexResponse}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexAcceptNewsletterAdminInviteJob")
public final class AcceptNewsletterAdminInviteMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of this mutation on the WhatsApp relay.
     *
     * <p>Emitted as the {@code query_id} attribute of the outgoing {@code <query>} child; the relay
     * refuses requests whose persisted-query identifier is unknown.
     */
    public static final String QUERY_ID = "9580828702035549";

    /**
     * Holds the GraphQL operation name reported by WhatsApp Web's MEX perf tracker for this
     * mutation.
     */
    public static final String OPERATION_NAME = "acceptNewsletterAdminInvite";

    /**
     * Holds the Jid string of the newsletter whose admin invite is being accepted.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexAcceptNewsletterAdminInviteJob", exports = "acceptNewsletterAdminInvite",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final String newsletterId;

    /**
     * Constructs a request targeting the given newsletter.
     *
     * <p>The {@code newsletterId} must be the newsletter Jid string; the relay rejects user or group
     * identifiers.
     *
     * @param newsletterId the newsletter Jid whose pending admin invite is accepted
     */
    @WhatsAppWebExport(moduleName = "WAWebMexAcceptNewsletterAdminInviteJob", exports = "acceptNewsletterAdminInvite",
            adaptation = WhatsAppAdaptation.DIRECT)
    public AcceptNewsletterAdminInviteMexRequest(String newsletterId) {
        this.newsletterId = newsletterId;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #QUERY_ID}, the persisted-query identifier of this mutation.
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #OPERATION_NAME}, the value WhatsApp Web's MEX perf tracker reports for this
     * mutation.
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code {variables: {newsletter_id: "<id>"}}} payload consumed by the
     * persisted query identified by {@link #QUERY_ID}; the {@code newsletter_id} entry is the sole
     * declared top-level variable and is always emitted.
     *
     * @implNote This implementation writes the GraphQL variables directly through a
     * {@link JSONWriter} and delegates envelope construction to
     * {@link MexStanza.Request.Json#createMexNode(String, String)}; any {@link IOException}
     * raised by the in-memory writer is wrapped in an {@link UncheckedIOException} since neither
     * sink can fail in practice.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexAcceptNewsletterAdminInviteJob", exports = "acceptNewsletterAdminInvite",
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

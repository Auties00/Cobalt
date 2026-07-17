package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX request that unsubscribes the local user from a newsletter.
 *
 * <p>This request backs the "unfollow newsletter" flow: the user unfollows a channel, this mutation
 * runs against the newsletter Jid, the relay stops forwarding that channel's updates, and the
 * channel is dropped from the local user's channel list. Construct it with the newsletter Jid string
 * and submit it through the MEX IQ dispatcher, then pair the reply with
 * {@link LeaveNewsletterMexResponse#of(Stanza)}. A returned
 * {@link LeaveNewsletterMexResponse.State} of {@code DELETED} or {@code NON_EXISTING} means the
 * relay treated the leave as already complete because the channel was removed, and {@code SUSPENDED}
 * means the channel is server-side suspended.
 */
@WhatsAppWebModule(moduleName = "WAWebMexLeaveNewsletterJob")
public final class LeaveNewsletterMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of the leave-newsletter mutation.
     *
     * <p>Emitted as the {@code query_id} attribute of the outgoing {@code <query>} child; the relay
     * refuses requests whose persisted-query id is unknown.
     */
    public static final String QUERY_ID = "9767147403369991";

    /**
     * Holds the GraphQL operation name reported for this mutation.
     *
     * <p>Forwarded to observability sinks that key telemetry on the operation name.
     */
    public static final String OPERATION_NAME = "mexLeaveNewsletter";

    /**
     * Holds the Jid string of the newsletter the local user is unsubscribing from.
     */
    private final String newsletterId;

    /**
     * Constructs a request targeting the given newsletter.
     *
     * <p>The {@code newsletterId} must be a newsletter Jid string; the relay rejects user or group
     * ids.
     *
     * @param newsletterId the newsletter Jid the local user is unsubscribing from
     */
    public LeaveNewsletterMexRequest(String newsletterId) {
        this.newsletterId = newsletterId;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #QUERY_ID}, the persisted-query identifier of the mutation.
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #OPERATION_NAME}, the operation name reported for this mutation.
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * Serialises this request into a MEX IQ {@link StanzaBuilder} ready to be dispatched through the
     * WhatsApp relay.
     *
     * <p>Produces the {@code {variables: {newsletter_id: "<id>"}}} payload consumed by the
     * persisted-query identified by {@link #QUERY_ID}. The {@code newsletter_id} entry is the sole
     * declared top-level variable and is always emitted.
     *
     * @implNote This implementation writes the GraphQL variables directly through
     * {@link JSONWriter} and delegates IQ envelope construction to
     * {@link Json#createMexNode(String, String)}; any {@link IOException} raised by the in-memory
     * writer is wrapped in an {@link UncheckedIOException} since neither sink can fail in practice.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexLeaveNewsletterJob", exports = "mexLeaveNewsletter",
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

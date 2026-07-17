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
 * Builds the MEX request that cancels a previously issued newsletter admin invitation.
 *
 * <p>This request backs the "revoke admin invite" owner action: a newsletter owner revokes a
 * pending admin invite, removing the invitee from the local pending-admins cache, and this mutation
 * removes the invite server-side. Construct it with the newsletter Jid and the invitee's user-id
 * string and submit it through the MEX IQ dispatcher.
 *
 * @implNote This implementation expects the caller to supply the already lid-migrated user id and
 * does not retry on its own, where the source caller migrates the target Jid and wraps the mutation
 * in a backoff retry. TODO: integrate the lid migration helper to match the source preprocessing.
 */
@WhatsAppWebModule(moduleName = "WAWebMexRevokeNewsletterAdminInviteJob")
public final class RevokeNewsletterAdminInviteMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of the revoke-newsletter-admin-invite mutation.
     *
     * <p>Emitted as the {@code query_id} attribute of the outgoing {@code <query>} child; the relay
     * refuses requests whose persisted-query id is unknown.
     */
    public static final String QUERY_ID = "9656078347839416";

    /**
     * Holds the GraphQL operation name reported for this mutation.
     *
     * <p>Forwarded to observability sinks that key telemetry on the operation name.
     */
    public static final String OPERATION_NAME = "revokeNewsletterAdminInvite";

    /**
     * Holds the Jid string of the newsletter whose pending admin invite is being revoked.
     */
    private final String newsletterId;

    /**
     * Holds the lid-migrated user identifier of the invite target.
     */
    private final String userId;

    /**
     * Constructs a request targeting the given pending admin invite.
     *
     * <p>Both arguments are required for the relay to identify the invite and are written
     * unconditionally as the two declared top-level GraphQL variables, so callers supply non-null
     * values.
     *
     * @param newsletterId the newsletter Jid whose pending admin invite is being revoked
     * @param userId       the lid-migrated identifier of the invite target
     */
    public RevokeNewsletterAdminInviteMexRequest(String newsletterId, String userId) {
        this.newsletterId = newsletterId;
        this.userId = userId;
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
     * <p>Produces the {@code {variables: {newsletter_id, user_id}}} payload consumed by the
     * persisted-query identified by {@link #QUERY_ID}. Both are declared top-level variables and are
     * always emitted.
     *
     * @implNote This implementation writes the GraphQL variables directly through
     * {@link JSONWriter} and delegates IQ envelope construction to
     * {@link Json#createMexNode(String, String)}; any {@link IOException} raised by the in-memory
     * writer is wrapped in an {@link UncheckedIOException} since neither sink can fail in practice.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexRevokeNewsletterAdminInviteJob", exports = "revokeNewsletterAdminInvite",
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

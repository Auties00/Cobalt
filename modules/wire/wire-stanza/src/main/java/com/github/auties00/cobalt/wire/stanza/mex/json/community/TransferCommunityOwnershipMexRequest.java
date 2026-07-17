package com.github.auties00.cobalt.wire.stanza.mex.json.community;

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
 * Outbound MEX mutation that transfers ownership of a community from the
 * current owner to another admin.
 *
 * <p>This mutation backs the transfer-ownership action in the community
 * settings. It promotes the new owner to the community owner role through the
 * {@code xwa2_group_update_users_role} mutation; the reply, modelled by
 * {@link TransferCommunityOwnershipMexResponse}, echoes the affected group id
 * and the resulting LID migration state (addressing mode) so callers can update
 * their local view of the community before replaying cached actions. WA Web
 * follows the mutation with a group-metadata refresh only when the addressing
 * mode actually changed.
 */
@WhatsAppWebModule(moduleName = "WAWebMexTransferCommunityOwnershipJob")
public final class TransferCommunityOwnershipMexRequest implements MexStanza.Request.Json {
    /**
     * Wire value of the {@code new_role} promotion applied to the new owner.
     *
     * <p>WhatsApp Web sends this constant for the sole {@code role_updates}
     * entry of an ownership transfer; it names the community owner role.
     */
    @WhatsAppWebExport(moduleName = "WAWebTransferCommunityOwnershipAction", exports = "transferCommunityOwnershipAction",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final String OWNER_ROLE = "SUPERADMIN_MEMBER";

    /**
     * Compiled GraphQL query identifier for the transfer-ownership document.
     *
     * <p>The relay maps this id to its persisted operation; the GraphQL text
     * is never sent on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexTransferCommunityOwnershipJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "29643783178598899";

    /**
     * GraphQL operation name carried by this mutation.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexTransferCommunityOwnershipJob", exports = "mexTransferCommunityOwnershipJob",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "mexTransferCommunityOwnershipJob";

    /**
     * Holds the Jid string of the community whose ownership is transferred.
     */
    private final String communityId;

    /**
     * Holds the Jid string of the admin promoted to the new community owner.
     */
    private final String newOwnerId;

    /**
     * Constructs a request that transfers ownership of the given community to
     * the given admin.
     *
     * <p>The {@code communityId} is written as the {@code group_id} input field
     * and {@code newOwnerId} becomes the sole {@code role_updates} entry,
     * promoted to {@link #OWNER_ROLE}.
     *
     * @param communityId the Jid of the community whose ownership is transferred
     * @param newOwnerId  the Jid of the admin promoted to the new owner
     */
    public TransferCommunityOwnershipMexRequest(String communityId, String newOwnerId) {
        this.communityId = communityId;
        this.newOwnerId = newOwnerId;
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
     * <p>Produces the
     * {@code {variables: {input: {group_id, role_updates: [{new_role, user_jid}]}}}}
     * payload consumed by the persisted-query identified by {@link #QUERY_ID}.
     * The {@code localParentGroupAddressingMode} flag WhatsApp Web threads
     * alongside {@code mexInput} is a client-only decision input (it gates a
     * follow-up metadata refresh) and is not part of the wire request, so it is
     * not sent.
     *
     * @implNote This implementation streams the GraphQL variables through
     * fastjson2's {@link JSONWriter} and builds the envelope through
     * {@link MexStanza.Request.Json#createMexNode(String, String)}. Any
     * {@link IOException} raised by the in-memory writer is wrapped in an
     * {@link UncheckedIOException}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexTransferCommunityOwnershipJob", exports = "mexTransferCommunityOwnershipJob",
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
            writer.writeName("group_id");
            writer.writeColon();
            writer.writeString(communityId);
            writer.writeName("role_updates");
            writer.writeColon();
            writer.startArray();
            writer.startObject();
            writer.writeName("new_role");
            writer.writeColon();
            writer.writeString(OWNER_ROLE);
            writer.writeName("user_jid");
            writer.writeColon();
            writer.writeString(newOwnerId);
            writer.endObject();
            writer.endArray();
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

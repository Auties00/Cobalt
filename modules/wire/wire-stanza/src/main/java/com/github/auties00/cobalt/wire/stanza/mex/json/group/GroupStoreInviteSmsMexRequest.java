package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the MEX mutation that asks the relay to deliver group-store invites to a set of
 * participants over SMS.
 *
 * <p>This mutation drives the SMS fallback for group invitations: the caller supplies the target
 * group Jid and the list of participants to reach, and the relay returns a per-participant
 * {@code error_code} array parsed by {@link GroupStoreInviteSmsMexResponse}. Both the group Jid and
 * the participant list are nested under the single {@code input} GraphQL variable.
 *
 * @see GroupStoreInviteSmsMexResponse
 */
@WhatsAppWebModule(moduleName = "WAWebMexGroupStoreInviteSmsJob")
public final class GroupStoreInviteSmsMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of the
     * {@code WAWebMexGroupStoreInviteSmsJobMutation} document.
     *
     * <p>The relay maps this identifier to its persisted operation; it is emitted as the
     * {@code query_id} attribute of the outgoing {@code <query>} child and the GraphQL text is never
     * sent on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGroupStoreInviteSmsJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "26810859745268181";

    /**
     * Holds the GraphQL operation name reported alongside this mutation when it is dispatched.
     *
     * <p>The name tags the operation in latency and error metrics; it is kept on the request so
     * embedders mirroring that telemetry surface can emit the same tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGroupStoreInviteSmsJobMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebMexGroupStoreInviteSmsJobMutation";

    /**
     * Holds the {@code group_jid} field of the {@code input} object: the Jid of the group whose
     * store invites are delivered over SMS.
     */
    private final String groupJid;

    /**
     * Holds the {@code partcipants} field of the {@code input} object: the participants to reach
     * over SMS.
     *
     * <p>The wire field name is misspelled {@code partcipants} (dropped {@code i}); this mirrors the
     * spelling baked into the WhatsApp Web dispatcher, which the relay expects verbatim.
     */
    private final List<String> participants;

    /**
     * Constructs a group-store-invite-SMS mutation request for the given group and participants.
     *
     * <p>The {@code groupJid} identifies the group whose invites are delivered over SMS and the
     * {@code participants} list names the recipients. Either argument whose value is {@code null} is
     * omitted from the nested {@code input} object.
     *
     * @param groupJid     the Jid of the target group, or {@code null} to omit the field
     * @param participants the participants to reach over SMS, or {@code null} to omit the field
     */
    public GroupStoreInviteSmsMexRequest(String groupJid, List<String> participants) {
        this.groupJid = groupJid;
        this.participants = participants;
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
     * {@link JSONWriter}, nesting the {@code group_jid} scalar and the {@code partcipants} array
     * under a single {@code input} object and emitting each field only when its corresponding
     * constructor argument is non-null. Each participant is emitted as an object of the shape
     * {@code {"user_jid": "<jid>"}} rather than a bare string, matching the WhatsApp Web dispatcher.
     * The participant array field is spelled {@code partcipants} to match the WhatsApp Web
     * dispatcher's misspelled key, which the relay expects verbatim. The wrapped envelope is built
     * through {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGroupStoreInviteSmsJob", exports = "mexGroupStoreInviteSms",
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
            if (participants != null) {
                writer.writeName("partcipants");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < participants.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.startObject();
                    writer.writeName("user_jid");
                    writer.writeColon();
                    writer.writeString(participants.get(i));
                    writer.endObject();
                }
                writer.endArray();
            }
            if (groupJid != null) {
                writer.writeName("group_jid");
                writer.writeColon();
                writer.writeString(groupJid);
            }
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

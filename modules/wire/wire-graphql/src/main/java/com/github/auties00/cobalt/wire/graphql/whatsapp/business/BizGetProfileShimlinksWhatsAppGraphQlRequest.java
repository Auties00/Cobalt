package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the graph.whatsapp.com GraphQL query that fetches the shimmed (redirect-wrapped) website links advertised on a
 * WhatsApp Business profile.
 *
 * <p>The single {@code bizJid} GraphQL variable names the business account whose profile websites are
 * being read. WhatsApp Web's {@code WAWebBizGetProfileShimlinksQuery} passes the business
 * {@link Jid} straight through; the graph.whatsapp.com endpoint returns one link-shim record per advertised website under
 * {@code xwa_whatsapp_smb_get_profile_linkshims}. The reply is consumed through
 * {@link BizGetProfileShimlinksWhatsAppGraphQlResponse}.
 *
 * @implNote This implementation emits the variable under the key {@code bizJid} (camel case) because
 * that is the GraphQL {@code LocalArgument} name carried in the {@code variables} object of snapshot
 * {@code 1040120866}; the compiled document remaps it to the server-side {@code biz_jid} argument
 * internally, so the wire {@code variables} key stays {@code bizJid}.
 *
 * @see BizGetProfileShimlinksWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizGetProfileShimlinksQuery")
public final class BizGetProfileShimlinksWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetProfileShimlinksQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24491258413796282";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetProfileShimlinksQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizGetProfileShimlinksQuery";

    /**
     * The {@code bizJid} GraphQL variable naming the business account whose profile websites are being
     * read, or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * Constructs a profile-shimlinks query request targeting the given business account.
     *
     * <p>The {@code bizJid} populates the {@code bizJid} GraphQL variable; a {@code null} value omits
     * the variable.
     *
     * @param bizJid the business account {@link Jid} whose profile websites are being read, or
     *               {@code null} to omit the variable
     */
    public BizGetProfileShimlinksWhatsAppGraphQlRequest(Jid bizJid) {
        this.bizJid = bizJid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
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
     * @implNote This implementation emits {@code {"bizJid": <bizJid>}}, writing the variable only when
     * {@link #bizJid} is non-null and emitting {@code "{}"} otherwise.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetProfileShimlinksQuery", exports = "getProfileShimlinks",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("bizJid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.CATALOG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}

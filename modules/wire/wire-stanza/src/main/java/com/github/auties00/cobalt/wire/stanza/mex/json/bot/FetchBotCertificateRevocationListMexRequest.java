package com.github.auties00.cobalt.wire.stanza.mex.json.bot;

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
 * Outbound MEX request that fetches the bot-feature certificate revocation list (CRL).
 *
 * <p>The reply carries the DER-encoded CRL for the {@code whatsapp_simple_signal} PKI together with
 * its {@code next_update} timestamp; the bot-signature verifier consults the revoked serial numbers
 * it lists when validating a forwarded AI bot message's certificate chain.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchBotCertificateRevocationList")
public final class FetchBotCertificateRevocationListMexRequest implements MexStanza.Request.Json {
    /**
     * Compiled GraphQL query identifier for the CRL document.
     *
     * <p>The relay maps this id to its persisted operation; the GraphQL text is never sent on the
     * wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchBotCertificateRevocationListQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "35807917542188393";

    /**
     * GraphQL operation name carried by this query.
     */
    public static final String OPERATION_NAME = "WAWebMexFetchBotCertificateRevocationListQuery";

    /**
     * The PKI feature name whose CRL is fetched, bound to the {@code crl_name} GraphQL variable.
     */
    private static final String CRL_NAME = "whatsapp_simple_signal";

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
     * @implNote This implementation streams the single {@code crl_name} GraphQL variable through
     * fastjson2's {@link JSONWriter} and wraps it in the standard MEX envelope through
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchBotCertificateRevocationList", exports = "mexFetchBotCertificateRevocationList",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("crl_name");
            writer.writeColon();
            writer.writeString(CRL_NAME);
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

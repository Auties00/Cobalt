package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that fetches the details of a Facebook ad account during the WhatsApp
 * Business ad-creation account-update step.
 *
 * <p>The operation takes a single {@code adAccountID} GraphQL variable naming the Facebook ad
 * account to read. The relay returns the account details under the aliased root {@code adAccount};
 * the reply is consumed through {@link BizAdCreationAdAccountUpdateAdAccountDetailsFacebookGraphQlResponse}.
 *
 * @implNote This implementation derives its single variable from the operation spec because the
 * {@code useWAWebBizAdCreationAdAccountUpdate_AdAccountDetailsQuery} hook module is not present in
 * the static bundle of snapshot {@code 1040120866}; it is one of the Comet ad-creation documents
 * loaded on demand. {@code adAccountID} is a Facebook ad-account identifier (a numeric string), not
 * a WhatsApp address, so it is modelled as a {@code String} rather than a
 * {@link com.github.auties00.cobalt.wire.core.jid.Jid}.
 *
 * @see BizAdCreationAdAccountUpdateAdAccountDetailsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationAdAccountUpdate_AdAccountDetailsQuery")
public final class BizAdCreationAdAccountUpdateAdAccountDetailsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationAdAccountUpdate_AdAccountDetailsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24708483602160361";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationAdAccountUpdate_AdAccountDetailsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationAdAccountUpdate_AdAccountDetailsQuery";

    /**
     * The {@code adAccountID} GraphQL variable naming the Facebook ad account to read, or
     * {@code null} to omit it.
     *
     * <p>A Facebook ad-account identifier (a numeric string), not a WhatsApp address.
     */
    private final String adAccountId;

    /**
     * Constructs an ad-account-details query request for the given ad account.
     *
     * <p>The {@code adAccountId} populates the {@code adAccountID} GraphQL variable; a {@code null}
     * value omits the variable from the serialized object.
     *
     * @param adAccountId the Facebook ad-account identifier to read, or {@code null} to omit the
     *                    variable
     */
    public BizAdCreationAdAccountUpdateAdAccountDetailsFacebookGraphQlRequest(String adAccountId) {
        this.adAccountId = adAccountId;
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
     * @implNote This implementation emits {@code {"adAccountID": <adAccountId>}}, writing the field
     * only when its value is non-null and emitting {@code "{}"} when it is {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (adAccountId != null) {
                writer.writeName("adAccountID");
                writer.writeColon();
                writer.writeString(adAccountId);
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
}

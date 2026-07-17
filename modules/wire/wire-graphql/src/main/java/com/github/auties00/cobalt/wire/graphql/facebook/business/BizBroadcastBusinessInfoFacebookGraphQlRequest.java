package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that resolves the marketing-message business info backing a WhatsApp
 * Business broadcast.
 *
 * <p>The single {@code input} GraphQL variable carries the business-info request, whose sole
 * {@code should_return_ad_account} flag controls whether the resolved ad account is returned. The Meta
 * graph endpoint returns the resolved entities under {@code xwa_smb_mm_business_info}: the
 * {@code business}, the {@code business_payment_account}, the {@code ad_account}, and the {@code page},
 * each exposed by its id. The reply is consumed through
 * {@link BizBroadcastBusinessInfoFacebookGraphQlResponse}.
 *
 * @see BizBroadcastBusinessInfoFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizBroadcastBusinessInfoMutation")
public final class BizBroadcastBusinessInfoFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizBroadcastBusinessInfoMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26164128406511010";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizBroadcastBusinessInfoMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizBroadcastBusinessInfoMutation";

    /**
     * The {@code should_return_ad_account} field of the {@code input} object controlling whether the
     * resolved ad account is returned, or {@code null} to omit the {@code input} variable.
     */
    private final Boolean shouldReturnAdAccount;

    /**
     * Constructs a resolve-business-info mutation request.
     *
     * <p>A {@code null} {@code shouldReturnAdAccount} omits the {@code input} variable.
     *
     * @param shouldReturnAdAccount whether the resolved ad account is returned, or {@code null} to omit
     *                              the variable
     */
    public BizBroadcastBusinessInfoFacebookGraphQlRequest(Boolean shouldReturnAdAccount) {
        this.shouldReturnAdAccount = shouldReturnAdAccount;
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
     * @implNote This implementation emits {@code {"input": {"should_return_ad_account":
     * <shouldReturnAdAccount>}}}, writing the {@code input} object only when
     * {@code shouldReturnAdAccount} is non-null and emitting {@code "{}"} otherwise.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (shouldReturnAdAccount != null) {
                writer.writeName("input");
                writer.writeColon();
                writer.startObject();
                writer.writeName("should_return_ad_account");
                writer.writeColon();
                writer.writeBool(shouldReturnAdAccount);
                writer.endObject();
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

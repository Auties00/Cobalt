package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ads.LwiBoostedComponentInput;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the comet mutation that creates a WhatsApp Business boosted component (an ad) from a fully
 * specified ad-creation draft.
 *
 * <p>The single {@code input} GraphQL variable is the boosted-component specification carried as a
 * {@link LwiBoostedComponentInput}: ad-account binding, creative, audience, budget, duration, and
 * objective. WhatsApp Web fills it from the ad-creation flow's accumulated draft state. The mutation
 * returns the created component under {@code create_boosted_component}; the reply is consumed through
 * {@link CometCreateBoostedComponentFacebookGraphQlResponse}.
 *
 * @see CometCreateBoostedComponentFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "LWICometCreateBoostedComponentMutation")
public final class CometCreateBoostedComponentFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "LWICometCreateBoostedComponentMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9955578997835249";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "LWICometCreateBoostedComponentMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "LWICometCreateBoostedComponentMutation";

    /**
     * The {@code input} GraphQL variable carrying the full ad specification, or {@code null} to omit
     * it.
     */
    private final LwiBoostedComponentInput input;

    /**
     * Constructs a create-boosted-component mutation request.
     *
     * <p>The {@code input} holds the full ad specification. A {@code null} value omits the variable
     * from the serialized object.
     *
     * @param input the boosted-component specification, or {@code null} to omit the variable
     */
    public CometCreateBoostedComponentFacebookGraphQlRequest(LwiBoostedComponentInput input) {
        this.input = input;
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
     * @implNote This implementation emits {@code {"input": {...}}} with the boost fields under their
     * snake_case keys, writing the variable only when the input is non-null and emitting {@code "{}"}
     * otherwise. The camelCase-to-snake_case mapping is performed by
     * {@link BizAdInputJson#writeLwiBoostedComponentInput(JSONWriter, LwiBoostedComponentInput)}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (input != null) {
                writer.writeName("input");
                writer.writeColon();
                BizAdInputJson.writeLwiBoostedComponentInput(writer, input);
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

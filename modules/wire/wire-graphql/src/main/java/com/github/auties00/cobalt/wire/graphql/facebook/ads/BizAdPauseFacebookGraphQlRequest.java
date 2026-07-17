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
 * Builds the comet mutation that pauses a running WhatsApp Business boosted component (an ad).
 *
 * <p>The single {@code boostID} GraphQL variable is the Facebook boost object identifier of the
 * boosted component to pause; the compiled document maps it onto the {@code wa_pause_boosted_component}
 * root mutation. The {@code boostID} is a numeric Facebook stanza id rather than a WhatsApp address, so
 * it is kept as a {@link String}. The mutation echoes the affected component's id; the reply is
 * consumed through {@link BizAdPauseFacebookGraphQlResponse}.
 *
 * @implNote This implementation models the {@code boostID} variable from the operation spec because
 * the {@code useWAWebBizAdPauseMutation} relay document and its calling hook are absent from the JS
 * bundle of snapshot {@code 1040120866}; only the persisted id, variable name, and response selection
 * tree are recoverable.
 *
 * @see BizAdPauseFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdPauseMutation")
public final class BizAdPauseFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdPauseMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25666806789679970";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdPauseMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdPauseMutation";

    /**
     * The {@code boostID} GraphQL variable identifying the Facebook boosted component to pause, or
     * {@code null} to omit it.
     */
    private final String boostId;

    /**
     * Constructs a pause-boosted-component mutation request.
     *
     * <p>The {@code boostId} is the Facebook boost object identifier of the boosted component to
     * pause. A {@code null} value omits the variable from the serialized object.
     *
     * @param boostId the Facebook boost object identifier, or {@code null} to omit the variable
     */
    public BizAdPauseFacebookGraphQlRequest(String boostId) {
        this.boostId = boostId;
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
     * @implNote This implementation emits {@code {"boostID": <boostId>}}, writing the variable only
     * when its value is non-null and emitting {@code "{}"} when it is {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (boostId != null) {
                writer.writeName("boostID");
                writer.writeColon();
                writer.writeString(boostId);
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

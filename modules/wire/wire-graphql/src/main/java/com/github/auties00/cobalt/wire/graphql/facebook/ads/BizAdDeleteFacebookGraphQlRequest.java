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
 * Builds the comet mutation that deletes a boosted component (an ad) from the WhatsApp Business
 * ad-management surface.
 *
 * <p>The mutation takes a single GraphQL variable, {@code boostID}, the Facebook boost id of the ad
 * to delete; it is a numeric Facebook id rather than a WhatsApp address, so it is kept as a
 * {@link String}. The mutation returns the deletion outcome under
 * {@code wa_delete_boosted_component}; the reply is consumed through
 * {@link BizAdDeleteFacebookGraphQlResponse}.
 *
 * @see BizAdDeleteFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdDeleteMutation")
public final class BizAdDeleteFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdDeleteMutation.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "33617903501158773";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdDeleteMutation.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdDeleteMutation";

    /**
     * The {@code boostID} GraphQL variable carrying the Facebook boost id of the ad to delete, or
     * {@code null} to omit it.
     */
    private final String boostId;

    /**
     * Constructs a delete-ad mutation request.
     *
     * <p>The {@code boostId} is the Facebook boost id of the ad to delete. A {@code null} value omits
     * the variable from the serialized object.
     *
     * @param boostId the Facebook boost id of the ad to delete, or {@code null} to omit the variable
     */
    public BizAdDeleteFacebookGraphQlRequest(String boostId) {
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

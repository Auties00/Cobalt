package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ads.SensitiveString;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the comet mutation that sends an email verification code for a WhatsApp Business
 * ad-account email-onboarding flow.
 *
 * <p>The mutation takes a single {@code input} GraphQL variable holding the email address to verify,
 * carried as a {@link SensitiveString} under the {@code email} key. The mutation returns whether the
 * email was sent and, if not, the failure reason; the reply is consumed through
 * {@link BizAdCreationSendEmailVerificationCodeFacebookGraphQlResponse}.
 *
 * @see BizAdCreationSendEmailVerificationCodeFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationSendEmailVerificationCodeMutation")
public final class BizAdCreationSendEmailVerificationCodeFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationSendEmailVerificationCodeMutation.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25314829244811285";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationSendEmailVerificationCodeMutation.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationSendEmailVerificationCodeMutation";

    /**
     * The {@code email} field of the {@code input} object carrying the email address to verify as a
     * sensitive string, or {@code null} to omit it.
     */
    private final SensitiveString email;

    /**
     * Constructs a send-email-verification-code mutation request.
     *
     * <p>A {@code null} {@code email} omits the field from the serialized {@code input} object.
     *
     * @param email the email address to verify, or {@code null} to omit the field
     */
    public BizAdCreationSendEmailVerificationCodeFacebookGraphQlRequest(SensitiveString email) {
        this.email = email;
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
     * @implNote This implementation emits {@code {"input": {"email": {"sensitive_string_value":
     * <email>}}}}, writing the {@code email} field only when it is non-null and emitting
     * {@code {"input": {}}} otherwise.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (email != null) {
                writer.writeName("email");
                writer.writeColon();
                BizAdInputJson.writeSensitiveString(writer, email);
            }
            writer.endObject();
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

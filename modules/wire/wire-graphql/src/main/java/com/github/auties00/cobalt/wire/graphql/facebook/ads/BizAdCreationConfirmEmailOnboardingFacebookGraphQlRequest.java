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
 * Builds the comet mutation that confirms the email step of WhatsApp Business ad-account onboarding.
 *
 * <p>The single {@code input} GraphQL variable is the onboarding-data upsert object WhatsApp Web
 * passes to {@code wa_ad_account_upsert_onboarding_data(input: $input)}: the verification code the
 * user entered ({@code code}), the email address ({@code email}), and the silent nonce returned from
 * the send-code step ({@code silent_nonce}), each carried as a {@link SensitiveString}. The mutation
 * returns the upsert outcome under {@code wa_ad_account_upsert_onboarding_data}; the reply is consumed
 * through {@link BizAdCreationConfirmEmailOnboardingFacebookGraphQlResponse}.
 *
 * @see BizAdCreationConfirmEmailOnboardingFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationConfirmEmailOnboardingMutation")
public final class BizAdCreationConfirmEmailOnboardingFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationConfirmEmailOnboardingMutation.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25542802338674299";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationConfirmEmailOnboardingMutation.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationConfirmEmailOnboardingMutation";

    /**
     * The {@code code} field of the {@code input} object carrying the verification code the user
     * entered as a sensitive string, or {@code null} to omit it.
     */
    private final SensitiveString code;

    /**
     * The {@code email} field of the {@code input} object carrying the onboarding email address as a
     * sensitive string, or {@code null} to omit it.
     */
    private final SensitiveString email;

    /**
     * The {@code silent_nonce} field of the {@code input} object carrying the nonce returned from the
     * send-code step as a sensitive string, or {@code null} to omit it.
     */
    private final SensitiveString silentNonce;

    /**
     * Constructs a confirm-email-onboarding mutation request.
     *
     * <p>Each value that is {@code null} omits its field from the serialized {@code input} object.
     *
     * @param code        the verification code the user entered, or {@code null} to omit the field
     * @param email       the onboarding email address, or {@code null} to omit the field
     * @param silentNonce the silent nonce from the send-code step, or {@code null} to omit the field
     */
    public BizAdCreationConfirmEmailOnboardingFacebookGraphQlRequest(SensitiveString code, SensitiveString email, SensitiveString silentNonce) {
        this.code = code;
        this.email = email;
        this.silentNonce = silentNonce;
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
     * @implNote This implementation emits {@code {"input": {"code": {"sensitive_string_value": ...},
     * "email": {"sensitive_string_value": ...}, "silent_nonce": {"sensitive_string_value": ...}}}},
     * writing each field only when its value is non-null and emitting {@code {"input": {}}} when all
     * are {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (code != null) {
                writer.writeName("code");
                writer.writeColon();
                BizAdInputJson.writeSensitiveString(writer, code);
            }

            if (email != null) {
                writer.writeName("email");
                writer.writeColon();
                BizAdInputJson.writeSensitiveString(writer, email);
            }

            if (silentNonce != null) {
                writer.writeName("silent_nonce");
                writer.writeColon();
                BizAdInputJson.writeSensitiveString(writer, silentNonce);
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

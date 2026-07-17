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
 * Builds the Facebook GraphQL mutation that creates or updates the caller's WhatsApp Ads identity page.
 *
 * <p>The mutation takes two GraphQL variables, {@code phone_number} and {@code code}, that the
 * server-side document folds into a single {@code input} object. WhatsApp Web's
 * {@code WAWebCreateWhatsAppAdsIdentity} fills {@code phone_number} from the linked device's
 * national-format number and {@code code} from the account nonce returned by
 * {@code WAWebGetAccountNonce}; both are wrapped in a {@code sensitive_string_value} envelope so the
 * Meta graph endpoint never logs them in the clear. The Meta graph endpoint returns the resulting advertising Page under
 * {@code create_or_update_whatsapp_ads_identity}, whose {@code id} scalar is the new page identifier;
 * the reply is consumed through {@link CreateWhatsAppAdsIdentityFacebookGraphQlResponse}.
 *
 * @see CreateWhatsAppAdsIdentityFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebCreateWhatsAppAdsIdentityMutation")
public final class CreateWhatsAppAdsIdentityFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateWhatsAppAdsIdentityMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24393949203623093";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateWhatsAppAdsIdentityMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebCreateWhatsAppAdsIdentityMutation";

    /**
     * The {@code phone_number} GraphQL variable naming the advertiser's phone number in national
     * format, or {@code null} to omit it.
     *
     * <p>A raw number string rather than an addressable {@link com.github.auties00.cobalt.wire.core.jid.Jid};
     * the Meta graph endpoint wraps it in a {@code sensitive_string_value} envelope.
     */
    private final String phoneNumber;

    /**
     * The {@code code} GraphQL variable carrying the account-nonce verification token, or
     * {@code null} to omit it.
     *
     * <p>The Meta graph endpoint wraps it in a {@code sensitive_string_value} envelope.
     */
    private final String code;

    /**
     * Constructs a create-WhatsApp-Ads-identity mutation request.
     *
     * <p>The {@code phoneNumber} populates the {@code phone_number} GraphQL variable and {@code code}
     * populates the {@code code} GraphQL variable; each value that is {@code null} omits its variable
     * from the serialized object.
     *
     * @param phoneNumber the advertiser's phone number in national format, or {@code null} to omit the
     *                    variable
     * @param code        the account-nonce verification token, or {@code null} to omit the variable
     */
    public CreateWhatsAppAdsIdentityFacebookGraphQlRequest(String phoneNumber, String code) {
        this.phoneNumber = phoneNumber;
        this.code = code;
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
     * @implNote This implementation emits
     * {@code {"phone_number": {"sensitive_string_value": <phoneNumber>}, "code": {"sensitive_string_value": <code>}}},
     * writing each variable only when its value is non-null and emitting {@code "{}"} when both are
     * {@code null}. The {@code sensitive_string_value} envelope mirrors the wrapping WhatsApp Web
     * applies in {@code WAWebCreateWhatsAppAdsIdentity} before handing the values to
     * {@code WAWebRelayClient.commitMutation}.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateWhatsAppAdsIdentity", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (phoneNumber != null) {
                writer.writeName("phone_number");
                writer.writeColon();
                writer.startObject();
                writer.writeName("sensitive_string_value");
                writer.writeColon();
                writer.writeString(phoneNumber);
                writer.endObject();
            }

            if (code != null) {
                writer.writeName("code");
                writer.writeColon();
                writer.startObject();
                writer.writeName("sensitive_string_value");
                writer.writeColon();
                writer.writeString(code);
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

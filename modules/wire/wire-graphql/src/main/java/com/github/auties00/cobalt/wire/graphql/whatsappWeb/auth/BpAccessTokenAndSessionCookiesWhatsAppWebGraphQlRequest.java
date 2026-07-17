package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the relay mutation that exchanges a business-platform authorization code for a WhatsApp
 * Business Platform access token and the associated browser session cookies.
 *
 * <p>The mutation carries two GraphQL variables. The {@code application_id} is the numeric Meta
 * application identifier the access token is minted for; WhatsApp Web pins it to a fixed value rather
 * than computing it per call. The {@code code} is the authorization code being redeemed; on the wire
 * it is not a bare scalar but the {@code WASensitiveString} wrapper object
 * {@code {"sensitive_string_value": <code>}} so the value is treated as redaction-sensitive by the
 * relay. The relay returns the minted token, its type, the session cookies, the business-platform id
 * and the associated email under {@code xwa_bp_access_token_and_session_cookies}; the reply is
 * consumed through {@link BpAccessTokenAndSessionCookiesWhatsAppWebGraphQlResponse}.
 *
 * @see BpAccessTokenAndSessionCookiesWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBPAccessTokenAndSessionCookiesMutation")
public final class BpAccessTokenAndSessionCookiesWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBPAccessTokenAndSessionCookiesMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26756198580685447";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBPAccessTokenAndSessionCookiesMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBPAccessTokenAndSessionCookiesMutation";

    /**
     * The {@code application_id} GraphQL variable naming the Meta application the access token is
     * minted for.
     *
     * <p>WhatsApp Web pins this to the fixed value {@code 656096963291760}; it is held as a field here
     * so callers in other deployments can supply the application id appropriate for their session.
     */
    private final long applicationId;

    /**
     * The raw authorization {@code code} being redeemed, or {@code null} to omit it.
     *
     * <p>The value is emitted wrapped in the {@code WASensitiveString} object
     * {@code {"sensitive_string_value": <code>}} rather than as a bare scalar.
     */
    private final String code;

    /**
     * Constructs a business-platform token-exchange mutation request.
     *
     * <p>The {@code applicationId} names the Meta application the token is minted for. The {@code code}
     * is the authorization code to redeem; a {@code null} {@code code} omits the {@code code} variable
     * from the serialized object, matching WhatsApp Web's null-coalescing of the wrapper.
     *
     * @param applicationId the Meta application identifier the access token is minted for
     * @param code          the authorization code to redeem, or {@code null} to omit the variable
     */
    public BpAccessTokenAndSessionCookiesWhatsAppWebGraphQlRequest(long applicationId, String code) {
        this.applicationId = applicationId;
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
     * {@code {"application_id": <applicationId>, "code": {"sensitive_string_value": <code>}}}, always
     * writing {@code application_id} as a numeric literal and writing the {@code code} wrapper object
     * only when {@code code} is non-null. The {@code WASensitiveString} envelope mirrors WhatsApp Web's
     * {@code {sensitive_string_value: e}} construction so the relay applies its redaction handling.
     */
    @WhatsAppWebExport(moduleName = "WAWebBPAccessTokenAndSessionCookiesMutation",
            exports = "fetchBPAccessTokenAndSessionCookies", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("application_id");
            writer.writeColon();
            writer.writeInt64(applicationId);
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

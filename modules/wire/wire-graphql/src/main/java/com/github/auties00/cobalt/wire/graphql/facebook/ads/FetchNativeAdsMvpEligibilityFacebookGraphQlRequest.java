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
 * Builds the Facebook GraphQL query that fetches the caller's native-ads MVP eligibility for the WhatsApp
 * Business advertising surface.
 *
 * <p>The operation takes a single {@code phone_number} GraphQL variable identifying the advertiser.
 * WhatsApp Web's {@code WAWebFetchNativeAdsMvpEligibility} fills it from the linked device's
 * national-format number and forwards it straight to the relay via
 * {@code WAWebRelayClient.fetchQuery}. The Meta graph endpoint returns the eligibility flags under
 * {@code wa_smb_native_ads_web_info}; the reply is consumed through
 * {@link FetchNativeAdsMvpEligibilityFacebookGraphQlResponse}.
 *
 * @see FetchNativeAdsMvpEligibilityFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebFetchNativeAdsMvpEligibilityQuery")
public final class FetchNativeAdsMvpEligibilityFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchNativeAdsMvpEligibilityQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "34778300218423824";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchNativeAdsMvpEligibilityQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebFetchNativeAdsMvpEligibilityQuery";

    /**
     * The {@code phone_number} GraphQL variable naming the advertiser's phone number in national
     * format, or {@code null} to omit it.
     *
     * <p>A raw number string rather than an addressable
     * {@link com.github.auties00.cobalt.wire.core.jid.Jid}.
     */
    private final String phoneNumber;

    /**
     * Constructs a fetch-native-ads-MVP-eligibility request for the given phone number.
     *
     * <p>The {@code phoneNumber} populates the {@code phone_number} GraphQL variable; a {@code null}
     * value omits the variable from the serialized object.
     *
     * @param phoneNumber the advertiser's phone number in national format, or {@code null} to omit the
     *                    variable
     */
    public FetchNativeAdsMvpEligibilityFacebookGraphQlRequest(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
     * @implNote This implementation emits {@code {"phone_number": <phoneNumber>}}, writing the field
     * only when its value is non-null and emitting {@code "{}"} when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchNativeAdsMvpEligibility", exports = "fetchNativeAdsMvpEligibility",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (phoneNumber != null) {
                writer.writeName("phone_number");
                writer.writeColon();
                writer.writeString(phoneNumber);
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

package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the advertiser-certification-state query built by
 * {@link BizAdCreationMaybeOpenCertificationDialogFacebookGraphQlRequest}.
 *
 * <p>Projects the linked {@code viewer.ad_integrity_certification} scalar, which carries the viewer's
 * current advertiser-certification state. A caller inspects that state to decide whether the
 * advertiser must complete certification before creating an ad. Its closed value set is not
 * confirmable from the static bundle of snapshot {@code 1040120866}, so it is exposed as a
 * {@link String}.
 *
 * @see BizAdCreationMaybeOpenCertificationDialogFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationMaybeOpenCertificationDialogQuery")
public final class BizAdCreationMaybeOpenCertificationDialogFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the viewer's advertiser-certification state, or {@code null} when the relay omitted it.
     */
    private final String certificationState;

    /**
     * Constructs a response wrapping the parsed certification state.
     *
     * <p>Reserved for the static parser.
     *
     * @param certificationState the certification state, or {@code null} when the relay omitted it
     */
    private BizAdCreationMaybeOpenCertificationDialogFacebookGraphQlResponse(String certificationState) {
        this.certificationState = certificationState;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked {@code viewer.ad_integrity_certification} scalar; the returned
     * {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationMaybeOpenCertificationDialogFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var viewer = data.getJSONObject("viewer");
        var certificationState = viewer == null ? null : viewer.getString("ad_integrity_certification");
        return Optional.of(new BizAdCreationMaybeOpenCertificationDialogFacebookGraphQlResponse(certificationState));
    }

    /**
     * Returns the viewer's advertiser-certification state.
     *
     * @return the certification state, or empty when the relay omitted the field
     */
    public Optional<String> certificationState() {
        return Optional.ofNullable(certificationState);
    }
}

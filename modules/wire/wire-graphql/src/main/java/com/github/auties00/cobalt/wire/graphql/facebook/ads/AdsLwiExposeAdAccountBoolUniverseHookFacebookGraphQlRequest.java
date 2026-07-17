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
 * Builds the Facebook GraphQL query that probes a left-side WhatsApp Business (LWI) boolean quick-experiment
 * universe for one Facebook ad account.
 *
 * <p>The operation takes seven scalar GraphQL variables that select the experiment universe and
 * record the exposure. {@code account_id} names the Facebook ad account being checked,
 * {@code universe_name} and {@code param_name} name the quick-experiment universe and the boolean
 * parameter to read inside it, {@code default_value} is the fallback value when the universe does
 * not resolve, {@code expose_with_multiple_ad_accounts} and {@code log_exposure} control whether the
 * server records an exposure, and {@code should_fetch} gates the conditional
 * {@code lwi.expose_ad_account_for_qe_bool} branch of the response. The relay returns the resolved
 * boolean under {@code lwi.expose_ad_account_for_qe_bool}; the reply is consumed through
 * {@link AdsLwiExposeAdAccountBoolUniverseHookFacebookGraphQlResponse}.
 *
 * @implNote This implementation derives its variable names and types from the operation spec because
 * the {@code useAdsLWIExposeAdAccountBoolUniverseHookQuery} hook module is not present in the static
 * bundle of snapshot {@code 1040120866}; it is one of the Comet ad-creation documents loaded on
 * demand. {@code account_id} is a Facebook ad-account identifier (a numeric string), not a WhatsApp
 * address, so it is modelled as a {@code String} rather than a
 * {@link com.github.auties00.cobalt.wire.core.jid.Jid}.
 *
 * @see AdsLwiExposeAdAccountBoolUniverseHookFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useAdsLWIExposeAdAccountBoolUniverseHookQuery")
public final class AdsLwiExposeAdAccountBoolUniverseHookFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useAdsLWIExposeAdAccountBoolUniverseHookQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "29539241599052847";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useAdsLWIExposeAdAccountBoolUniverseHookQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useAdsLWIExposeAdAccountBoolUniverseHookQuery";

    /**
     * The {@code account_id} GraphQL variable naming the Facebook ad account being probed, or
     * {@code null} to omit it.
     *
     * <p>A Facebook ad-account identifier (a numeric string), not a WhatsApp address.
     */
    private final String accountId;

    /**
     * The {@code default_value} GraphQL variable carrying the boolean to fall back to when the
     * universe does not resolve, or {@code null} to omit it.
     */
    private final Boolean defaultValue;

    /**
     * The {@code expose_with_multiple_ad_accounts} GraphQL variable controlling whether the exposure
     * is recorded when the caller manages several ad accounts, or {@code null} to omit it.
     */
    private final Boolean exposeWithMultipleAdAccounts;

    /**
     * The {@code log_exposure} GraphQL variable controlling whether the server records the exposure,
     * or {@code null} to omit it.
     */
    private final Boolean logExposure;

    /**
     * The {@code param_name} GraphQL variable naming the boolean parameter to read inside the
     * quick-experiment universe, or {@code null} to omit it.
     */
    private final String paramName;

    /**
     * The {@code should_fetch} GraphQL variable gating the conditional
     * {@code lwi.expose_ad_account_for_qe_bool} branch of the response, or {@code null} to omit it.
     */
    private final Boolean shouldFetch;

    /**
     * The {@code universe_name} GraphQL variable naming the quick-experiment universe to resolve, or
     * {@code null} to omit it.
     */
    private final String universeName;

    /**
     * Constructs an LWI boolean-universe-hook query request.
     *
     * <p>Each value populates the like-named GraphQL variable; every value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param accountId                    the Facebook ad-account identifier being probed, or
     *                                     {@code null} to omit the variable
     * @param defaultValue                 the fallback boolean when the universe does not resolve, or
     *                                     {@code null} to omit the variable
     * @param exposeWithMultipleAdAccounts whether to record the exposure with multiple ad accounts,
     *                                     or {@code null} to omit the variable
     * @param logExposure                  whether the server records the exposure, or {@code null} to
     *                                     omit the variable
     * @param paramName                    the boolean parameter name inside the universe, or
     *                                     {@code null} to omit the variable
     * @param shouldFetch                  whether to fetch the conditional response branch, or
     *                                     {@code null} to omit the variable
     * @param universeName                 the quick-experiment universe name, or {@code null} to omit
     *                                     the variable
     */
    public AdsLwiExposeAdAccountBoolUniverseHookFacebookGraphQlRequest(String accountId, Boolean defaultValue,
            Boolean exposeWithMultipleAdAccounts, Boolean logExposure, String paramName, Boolean shouldFetch,
            String universeName) {
        this.accountId = accountId;
        this.defaultValue = defaultValue;
        this.exposeWithMultipleAdAccounts = exposeWithMultipleAdAccounts;
        this.logExposure = logExposure;
        this.paramName = paramName;
        this.shouldFetch = shouldFetch;
        this.universeName = universeName;
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
     * @implNote This implementation emits {@code {"account_id": <accountId>, "default_value":
     * <defaultValue>, "expose_with_multiple_ad_accounts": <exposeWithMultipleAdAccounts>,
     * "log_exposure": <logExposure>, "param_name": <paramName>, "should_fetch": <shouldFetch>,
     * "universe_name": <universeName>}}, writing each field only when its value is non-null and
     * emitting {@code "{}"} when every value is {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (accountId != null) {
                writer.writeName("account_id");
                writer.writeColon();
                writer.writeString(accountId);
            }

            if (defaultValue != null) {
                writer.writeName("default_value");
                writer.writeColon();
                writer.writeBool(defaultValue);
            }

            if (exposeWithMultipleAdAccounts != null) {
                writer.writeName("expose_with_multiple_ad_accounts");
                writer.writeColon();
                writer.writeBool(exposeWithMultipleAdAccounts);
            }

            if (logExposure != null) {
                writer.writeName("log_exposure");
                writer.writeColon();
                writer.writeBool(logExposure);
            }

            if (paramName != null) {
                writer.writeName("param_name");
                writer.writeColon();
                writer.writeString(paramName);
            }

            if (shouldFetch != null) {
                writer.writeName("should_fetch");
                writer.writeColon();
                writer.writeBool(shouldFetch);
            }

            if (universeName != null) {
                writer.writeName("universe_name");
                writer.writeColon();
                writer.writeString(universeName);
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

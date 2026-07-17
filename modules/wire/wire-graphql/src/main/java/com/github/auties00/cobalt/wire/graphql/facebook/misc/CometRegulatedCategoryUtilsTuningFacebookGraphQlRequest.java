package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.facebook.ads.BizAdInputJson;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ads.TuningOptions;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that tunes a targeting spec for a regulated category in the WhatsApp
 * Business ad-creation flow.
 *
 * <p>The query takes four GraphQL variables: {@code legacyAccount}, the Facebook ad account the
 * targeting belongs to; {@code targetSpec}, the JSON-encoded targeting spec to tune;
 * {@code regulatedCategory}, the regulated category to tune the spec for; and {@code tuningOptions},
 * an options object controlling how the spec is tuned. The relay returns the tuned spec under the
 * linked {@code hec} root's {@code tune_target_spec_for_category} object, whose scalar
 * {@code target_spec_string} carries the resulting JSON-encoded targeting spec; the reply is
 * consumed through {@link CometRegulatedCategoryUtilsTuningFacebookGraphQlResponse}.
 *
 * @implNote This implementation derives its four variables from the operation spec because the
 * {@code LWICometRegulatedCategoryUtilsTuningQuery} module and its compiled {@code .graphql}
 * document are not present in the static bundle of snapshot {@code 1040120866}; it is one of the
 * Comet ad-creation documents loaded on demand. {@code legacyAccount} is a Facebook ad-account
 * identifier (a numeric string), not a WhatsApp address, so it is modelled as a {@code String}
 * rather than a {@link com.github.auties00.cobalt.wire.core.jid.Jid}; {@code targetSpec} and
 * {@code regulatedCategory} are already-serialized strings. {@code tuningOptions} is the typed
 * {@link TuningOptions} input object, mapped to its snake_case JSON shape by {@link BizAdInputJson}.
 *
 * @see CometRegulatedCategoryUtilsTuningFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "LWICometRegulatedCategoryUtilsTuningQuery")
public final class CometRegulatedCategoryUtilsTuningFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "LWICometRegulatedCategoryUtilsTuningQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "29533308609649721";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "LWICometRegulatedCategoryUtilsTuningQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "LWICometRegulatedCategoryUtilsTuningQuery";

    /**
     * The {@code legacyAccount} GraphQL variable naming the Facebook ad account the targeting
     * belongs to, or {@code null} to omit it.
     *
     * <p>A Facebook ad-account identifier (a numeric string), not a WhatsApp address.
     */
    private final String legacyAccount;

    /**
     * The {@code targetSpec} GraphQL variable carrying the JSON-encoded targeting spec to tune, or
     * {@code null} to omit it.
     */
    private final String targetSpec;

    /**
     * The {@code regulatedCategory} GraphQL variable naming the regulated category to tune the spec
     * for, or {@code null} to omit it.
     */
    private final String regulatedCategory;

    /**
     * The {@code tuningOptions} GraphQL object controlling how the spec is tuned, or {@code null} to
     * omit it.
     */
    private final TuningOptions tuningOptions;

    /**
     * Constructs a regulated-category tuning query request.
     *
     * <p>The {@code legacyAccount} populates the {@code legacyAccount} GraphQL variable, the
     * {@code targetSpec} populates the {@code targetSpec} variable, the {@code regulatedCategory}
     * populates the {@code regulatedCategory} variable, and the {@code tuningOptions} populates the
     * {@code tuningOptions} object. Each value that is {@code null} omits its variable from the
     * serialized object.
     *
     * @param legacyAccount     the Facebook ad-account identifier, or {@code null} to omit the
     *                          variable
     * @param targetSpec        the JSON-encoded targeting spec, or {@code null} to omit the variable
     * @param regulatedCategory the regulated category to tune for, or {@code null} to omit the
     *                          variable
     * @param tuningOptions     the {@code tuningOptions} object, or {@code null} to omit the variable
     */
    public CometRegulatedCategoryUtilsTuningFacebookGraphQlRequest(String legacyAccount, String targetSpec, String regulatedCategory, TuningOptions tuningOptions) {
        this.legacyAccount = legacyAccount;
        this.targetSpec = targetSpec;
        this.regulatedCategory = regulatedCategory;
        this.tuningOptions = tuningOptions;
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
     * @implNote This implementation emits {@code {"legacyAccount": <legacyAccount>, "targetSpec":
     * <targetSpec>, "regulatedCategory": <regulatedCategory>, "tuningOptions":
     * {"clear_custom_audiences": ...}}}, writing each variable only when its value is non-null and
     * emitting {@code "{}"} when all are {@code null}. The {@code tuningOptions} object is mapped by
     * {@link BizAdInputJson#writeTuningOptions(JSONWriter, TuningOptions)}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (legacyAccount != null) {
                writer.writeName("legacyAccount");
                writer.writeColon();
                writer.writeString(legacyAccount);
            }

            if (targetSpec != null) {
                writer.writeName("targetSpec");
                writer.writeColon();
                writer.writeString(targetSpec);
            }

            if (regulatedCategory != null) {
                writer.writeName("regulatedCategory");
                writer.writeColon();
                writer.writeString(regulatedCategory);
            }

            if (tuningOptions != null) {
                writer.writeName("tuningOptions");
                writer.writeColon();
                BizAdInputJson.writeTuningOptions(writer, tuningOptions);
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

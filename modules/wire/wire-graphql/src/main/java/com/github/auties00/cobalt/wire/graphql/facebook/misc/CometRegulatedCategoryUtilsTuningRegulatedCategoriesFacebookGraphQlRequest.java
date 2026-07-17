package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.facebook.ads.BizAdInputJson;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ads.SpecialAdCategory;
import com.github.auties00.cobalt.wire.linked.business.ads.TargetingSpec;
import com.github.auties00.cobalt.wire.linked.business.ads.TuningOptions;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the Facebook GraphQL query that tunes a targeting spec for the configured regulated categories in the
 * WhatsApp Business ad-creation flow.
 *
 * <p>The query takes five GraphQL variables. {@code legacyAccount} is the Facebook ad account the
 * targeting belongs to. {@code targetSpec}, {@code regulatedCategories}, {@code specialAdCategoryCountries}
 * and {@code tuningOptions} parameterise the regulated-category tuning: the {@link TargetingSpec
 * targeting spec} to tune, the {@link SpecialAdCategory regulated categories} to tune for, the
 * special-ad-category countries that apply, and the {@link TuningOptions tuning options}. The relay
 * returns the tuned targeting spec under the linked {@code hec} root's
 * {@code tune_target_spec_for_categories.target_spec_string}; the reply is consumed through
 * {@link CometRegulatedCategoryUtilsTuningRegulatedCategoriesFacebookGraphQlResponse}.
 *
 * @implNote This implementation derives its variables from the operation spec because the
 * {@code LWICometRegulatedCategoryUtilsTuningRegulatedCategoriesQuery} module and its compiled
 * {@code .graphql} document are not present in the static bundle of snapshot {@code 1040120866}; it
 * is one of the Comet ad-creation documents loaded on demand. {@code legacyAccount} is a Facebook
 * ad-account identifier (a numeric string), not a WhatsApp address, so it is modelled as a
 * {@code String} rather than a {@link com.github.auties00.cobalt.wire.core.jid.Jid}. The
 * {@code targetSpec} and {@code tuningOptions} objects are mapped to their snake_case JSON shapes by
 * {@link BizAdInputJson}; {@code regulatedCategories} is a JSON array of category wire literals and
 * {@code specialAdCategoryCountries} a JSON array of country codes.
 *
 * @see CometRegulatedCategoryUtilsTuningRegulatedCategoriesFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "LWICometRegulatedCategoryUtilsTuningRegulatedCategoriesQuery")
public final class CometRegulatedCategoryUtilsTuningRegulatedCategoriesFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "LWICometRegulatedCategoryUtilsTuningRegulatedCategoriesQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9654629321281796";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "LWICometRegulatedCategoryUtilsTuningRegulatedCategoriesQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "LWICometRegulatedCategoryUtilsTuningRegulatedCategoriesQuery";

    /**
     * The {@code legacyAccount} GraphQL variable naming the Facebook ad account the targeting belongs
     * to, or {@code null} to omit it.
     *
     * <p>A Facebook ad-account identifier (a numeric string), not a WhatsApp address.
     */
    private final String legacyAccount;

    /**
     * The {@code targetSpec} GraphQL input object carrying the targeting spec to tune, or {@code null}
     * to omit it.
     */
    private final TargetingSpec targetSpec;

    /**
     * The {@code regulatedCategories} GraphQL input value carrying the regulated categories to tune
     * for, written only when non-empty. Never {@code null} after construction.
     */
    private final List<SpecialAdCategory> regulatedCategories;

    /**
     * The {@code specialAdCategoryCountries} GraphQL input value carrying the special-ad-category
     * countries that apply, written only when non-empty. Never {@code null} after construction.
     */
    private final List<String> specialAdCategoryCountries;

    /**
     * The {@code tuningOptions} GraphQL input object carrying the tuning options, or {@code null} to
     * omit it.
     */
    private final TuningOptions tuningOptions;

    /**
     * Constructs a regulated-category tuning query request.
     *
     * <p>The {@code legacyAccount} populates the {@code legacyAccount} GraphQL variable. The
     * {@code targetSpec}, {@code regulatedCategories}, {@code specialAdCategoryCountries} and
     * {@code tuningOptions} populate the corresponding input values. Each value that is {@code null}
     * (or, for the lists, empty) omits its variable from the serialized object.
     *
     * @param legacyAccount              the Facebook ad-account identifier, or {@code null} to omit
     *                                   the variable
     * @param targetSpec                 the {@code targetSpec} object, or {@code null} to omit the
     *                                   variable
     * @param regulatedCategories        the regulated categories, written only when non-empty
     * @param specialAdCategoryCountries the special-ad-category countries, written only when non-empty
     * @param tuningOptions              the {@code tuningOptions} object, or {@code null} to omit the
     *                                   variable
     */
    public CometRegulatedCategoryUtilsTuningRegulatedCategoriesFacebookGraphQlRequest(String legacyAccount, TargetingSpec targetSpec, List<SpecialAdCategory> regulatedCategories, List<String> specialAdCategoryCountries, TuningOptions tuningOptions) {
        this.legacyAccount = legacyAccount;
        this.targetSpec = targetSpec;
        this.regulatedCategories = regulatedCategories == null ? List.of() : List.copyOf(regulatedCategories);
        this.specialAdCategoryCountries = specialAdCategoryCountries == null ? List.of() : List.copyOf(specialAdCategoryCountries);
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
     * @implNote This implementation emits {@code {"legacyAccount": <legacyAccount>, "targetSpec": {...},
     * "regulatedCategories": [...], "specialAdCategoryCountries": [...], "tuningOptions": {...}}},
     * writing each variable only when present (and the arrays only when non-empty) and emitting
     * {@code "{}"} when all are absent. The {@code targetSpec} and {@code tuningOptions} objects are
     * mapped by {@link BizAdInputJson}; each {@link SpecialAdCategory} is rendered as its wire literal.
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
                BizAdInputJson.writeTargetingSpec(writer, targetSpec);
            }

            if (!regulatedCategories.isEmpty()) {
                writer.writeName("regulatedCategories");
                writer.writeColon();
                BizAdInputJson.writeSpecialAdCategories(writer, regulatedCategories);
            }

            if (!specialAdCategoryCountries.isEmpty()) {
                writer.writeName("specialAdCategoryCountries");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < specialAdCategoryCountries.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(specialAdCategoryCountries.get(i));
                }
                writer.endArray();
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

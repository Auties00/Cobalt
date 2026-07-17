package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdCreationRootInput;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that bootstraps the entire WhatsApp Business ad-creation root view.
 *
 * <p>The query takes six GraphQL variables. The {@code input} is the {@link BusinessAdCreationRootInput}
 * WhatsApp Web passes to
 * {@code lwi.boosted_component_wrapper(caller: "CTWA_SMB_WEB_AD_CREATE_FLOW", draft_id: $draftID,
 * input: $input)}: the ad account, boost, flow, page, and product being created. The {@code draftID}
 * and {@code pageID} variables are Facebook stanza ids (the ad draft and the page), so each is kept as a
 * {@link String} rather than a {@link com.github.auties00.cobalt.wire.core.jid.Jid}. The
 * {@code isFBAccount} and {@code isWAAccount} variables are include-condition booleans gating the
 * Facebook-profile and WhatsApp-onboarding sub-selections respectively. The
 * {@code __relay_internal__pv__LWICometIGUserIdDoubleWriteEnabledrelayprovider} variable is a Relay
 * provided boolean flag toggling the {@code instagram_user_id} double-write sub-selections; it is
 * modelled as the {@code igUserIdDoubleWriteEnabled} field but serialized under its exact wire key.
 * The query returns the full boosted-component spec, the eligible ad accounts, the page preview, and
 * the optional Facebook/WhatsApp account context; the reply is consumed through
 * {@link BizAdCreationRootFacebookGraphQlResponse}.
 *
 * @implNote This implementation maps the typed {@code input} object to its snake_case JSON shape by
 * {@link BizAdInputJson}.
 *
 * @see BizAdCreationRootFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationRootQuery")
public final class BizAdCreationRootFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationRootQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26390847997274475";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationRootQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationRootQuery";

    /**
     * The exact wire key of the Relay provided boolean variable toggling the
     * {@code instagram_user_id} double-write sub-selections.
     *
     * <p>The compiled document populates this variable from the
     * {@code LWICometIGUserIdDoubleWriteEnabled.relayprovider} provider; Cobalt exposes the same value
     * through {@code igUserIdDoubleWriteEnabled} and serializes it under this key verbatim.
     */
    private static final String IG_USER_ID_DOUBLE_WRITE_ENABLED_KEY =
            "__relay_internal__pv__LWICometIGUserIdDoubleWriteEnabledrelayprovider";

    /**
     * The {@code input} GraphQL object identifying the boost being created, or {@code null} to omit it.
     */
    private final BusinessAdCreationRootInput input;

    /**
     * The {@code draftID} GraphQL variable carrying the Facebook ad-draft stanza id, or {@code null} to
     * omit it.
     */
    private final String draftId;

    /**
     * The {@code isFBAccount} GraphQL variable gating the Facebook-profile sub-selection, or
     * {@code null} to omit it.
     */
    private final Boolean isFbAccount;

    /**
     * The {@code isWAAccount} GraphQL variable gating the WhatsApp-onboarding sub-selection, or
     * {@code null} to omit it.
     */
    private final Boolean isWaAccount;

    /**
     * The {@code pageID} GraphQL variable carrying the Facebook page stanza id, or {@code null} to omit
     * it.
     */
    private final String pageId;

    /**
     * The Relay provided boolean flag toggling the {@code instagram_user_id} double-write
     * sub-selections, serialized under {@value #IG_USER_ID_DOUBLE_WRITE_ENABLED_KEY}, or {@code null}
     * to omit it.
     */
    private final Boolean igUserIdDoubleWriteEnabled;

    /**
     * Constructs an ad-creation root query request.
     *
     * <p>The {@code input} identifies the boost being created. The {@code draftId} and {@code pageId}
     * are Facebook stanza ids. The {@code isFbAccount} and {@code isWaAccount} flags gate the optional
     * account-context sub-selections. The {@code igUserIdDoubleWriteEnabled} flag toggles the Instagram
     * user-id double-write sub-selections and is serialized under
     * {@value #IG_USER_ID_DOUBLE_WRITE_ENABLED_KEY}. Each value that is {@code null} omits its variable
     * from the serialized object.
     *
     * @param input                      the ad-creation-root input, or {@code null} to omit the
     *                                   variable
     * @param draftId                    the Facebook ad-draft stanza id, or {@code null} to omit the
     *                                   variable
     * @param isFbAccount                whether the linked account is a Facebook account, or
     *                                   {@code null} to omit the variable
     * @param isWaAccount                whether the linked account is a WhatsApp account, or
     *                                   {@code null} to omit the variable
     * @param pageId                     the Facebook page stanza id, or {@code null} to omit the
     *                                   variable
     * @param igUserIdDoubleWriteEnabled whether the Instagram user-id double-write sub-selections are
     *                                   enabled, or {@code null} to omit the variable
     */
    public BizAdCreationRootFacebookGraphQlRequest(BusinessAdCreationRootInput input, String draftId, Boolean isFbAccount, Boolean isWaAccount, String pageId, Boolean igUserIdDoubleWriteEnabled) {
        this.input = input;
        this.draftId = draftId;
        this.isFbAccount = isFbAccount;
        this.isWaAccount = isWaAccount;
        this.pageId = pageId;
        this.igUserIdDoubleWriteEnabled = igUserIdDoubleWriteEnabled;
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
     * @implNote This implementation emits {@code {"input": {...}, "draftID": <draftId>,
     * "isFBAccount": <isFbAccount>, "isWAAccount": <isWaAccount>, "pageID": <pageId>,
     * "__relay_internal__pv__LWICometIGUserIdDoubleWriteEnabledrelayprovider":
     * <igUserIdDoubleWriteEnabled>}}, writing each variable only when its value is non-null and
     * emitting {@code "{}"} when all are {@code null}. The {@code input} object is mapped by
     * {@link BizAdInputJson#writeCreationRootInput(JSONWriter, BusinessAdCreationRootInput)}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (input != null) {
                writer.writeName("input");
                writer.writeColon();
                BizAdInputJson.writeCreationRootInput(writer, input);
            }

            if (draftId != null) {
                writer.writeName("draftID");
                writer.writeColon();
                writer.writeString(draftId);
            }

            if (isFbAccount != null) {
                writer.writeName("isFBAccount");
                writer.writeColon();
                writer.writeBool(isFbAccount);
            }

            if (isWaAccount != null) {
                writer.writeName("isWAAccount");
                writer.writeColon();
                writer.writeBool(isWaAccount);
            }

            if (pageId != null) {
                writer.writeName("pageID");
                writer.writeColon();
                writer.writeString(pageId);
            }

            if (igUserIdDoubleWriteEnabled != null) {
                writer.writeName(IG_USER_ID_DOUBLE_WRITE_ENABLED_KEY);
                writer.writeColon();
                writer.writeBool(igUserIdDoubleWriteEnabled);
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

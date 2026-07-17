package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that creates a click-to-WhatsApp marketing-messages ad campaign for a
 * WhatsApp Business broadcast.
 *
 * <p>The mutation takes a single {@code input} GraphQL variable of type
 * {@code WhatsAppMarketingMessagesCreateInput}, forwarded to the server-side field argument named
 * {@code data}. WhatsApp Web's {@code WAWebCreateMarketingCampaignAction.createMarketingCampaignAction}
 * fills the object from the campaign input with the Meta ad-account id, the campaign name, the
 * lifetime budget, the Facebook page id, and the WhatsApp Business Account id. The Meta graph endpoint returns the
 * created campaign's identifiers and status under {@code whatsapp_marketing_messages_create}; the
 * reply is consumed through {@link CreateMarketingCampaignActionFacebookGraphQlResponse}.
 *
 * @see CreateMarketingCampaignActionFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebCreateMarketingCampaignActionMutation")
public final class CreateMarketingCampaignActionFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateMarketingCampaignActionMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26304826652483067";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateMarketingCampaignActionMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebCreateMarketingCampaignActionMutation";

    /**
     * The {@code ad_account_id} field of the {@code input} object naming the Meta ad account funding
     * the campaign, or {@code null} to omit it.
     */
    private final String adAccountId;

    /**
     * The {@code campaign_name} field of the {@code input} object, or {@code null} to omit it.
     */
    private final String campaignName;

    /**
     * The {@code lifetime_budget} field of the {@code input} object carrying the campaign's lifetime
     * spending cap, or {@code null} to omit it.
     */
    private final String lifetimeBudget;

    /**
     * The {@code page_id} field of the {@code input} object naming the Facebook page associated with
     * the campaign, or {@code null} to omit it.
     */
    private final String pageId;

    /**
     * The {@code waba_id} field of the {@code input} object naming the WhatsApp Business Account, or
     * {@code null} to omit it.
     */
    private final String wabaId;

    /**
     * Constructs a create-marketing-campaign mutation request.
     *
     * <p>All five values populate the {@code input} GraphQL object: the Meta ad-account id, the
     * campaign name, the lifetime budget, the Facebook page id, and the WhatsApp Business Account id.
     * Each value that is {@code null} is omitted from the serialized object.
     *
     * @param adAccountId    the Meta ad-account id funding the campaign, or {@code null} to omit the
     *                       field
     * @param campaignName   the campaign name, or {@code null} to omit the field
     * @param lifetimeBudget the campaign's lifetime spending cap, or {@code null} to omit the field
     * @param pageId         the associated Facebook page id, or {@code null} to omit the field
     * @param wabaId         the WhatsApp Business Account id, or {@code null} to omit the field
     */
    public CreateMarketingCampaignActionFacebookGraphQlRequest(String adAccountId, String campaignName, String lifetimeBudget,
                                                                  String pageId, String wabaId) {
        this.adAccountId = adAccountId;
        this.campaignName = campaignName;
        this.lifetimeBudget = lifetimeBudget;
        this.pageId = pageId;
        this.wabaId = wabaId;
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
     * @implNote This implementation emits {@code {"input": {"ad_account_id": <adAccountId>,
     * "campaign_name": <campaignName>, "lifetime_budget": <lifetimeBudget>, "page_id": <pageId>,
     * "waba_id": <wabaId>}}}, writing each field only when its value is non-null and emitting
     * {@code {"input": {}}} when all five are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateMarketingCampaignAction", exports = "createMarketingCampaignAction",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (adAccountId != null) {
                writer.writeName("ad_account_id");
                writer.writeColon();
                writer.writeString(adAccountId);
            }

            if (campaignName != null) {
                writer.writeName("campaign_name");
                writer.writeColon();
                writer.writeString(campaignName);
            }

            if (lifetimeBudget != null) {
                writer.writeName("lifetime_budget");
                writer.writeColon();
                writer.writeString(lifetimeBudget);
            }

            if (pageId != null) {
                writer.writeName("page_id");
                writer.writeColon();
                writer.writeString(pageId);
            }

            if (wabaId != null) {
                writer.writeName("waba_id");
                writer.writeColon();
                writer.writeString(wabaId);
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

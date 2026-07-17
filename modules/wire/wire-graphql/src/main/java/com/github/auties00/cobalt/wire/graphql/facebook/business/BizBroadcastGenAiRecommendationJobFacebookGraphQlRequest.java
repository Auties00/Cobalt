package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that fetches GenAI-generated message recommendations for a WhatsApp Business
 * broadcast.
 *
 * <p>The single {@code data} GraphQL variable is the recommendation-request object. WhatsApp Web's
 * {@code WAWebBizBroadcastGenAIRecommendationJob.fetchBroadcastGenAIRecommendation} fills it with the
 * {@code actor_id} business-profile id, the {@code model} naming the generative model, an optional
 * {@code user_info} JSON blob carrying the recent message history, the {@code user_message_draft} the
 * sender is composing, and the {@code user_prompt} the sender supplied. The Meta graph endpoint returns the
 * recommendation result under {@code xwa_business_broadcast_genai_recommendation}; the reply is
 * consumed through {@link BizBroadcastGenAiRecommendationJobFacebookGraphQlResponse}.
 *
 * @see BizBroadcastGenAiRecommendationJobFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizBroadcastGenAIRecommendationJobQuery")
public final class BizBroadcastGenAiRecommendationJobFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizBroadcastGenAIRecommendationJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25919238687747626";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizBroadcastGenAIRecommendationJobQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizBroadcastGenAIRecommendationJobQuery";

    /**
     * The {@code actor_id} field of the {@code data} object naming the requesting business profile, or
     * {@code null} to omit it.
     *
     * <p>Kept as a {@link String}: it is the Facebook business-profile id ({@code bp_id}) drawn from
     * the ad-account token, not a WhatsApp address.
     */
    private final String actorId;

    /**
     * The {@code model} field of the {@code data} object naming the generative model, or {@code null}
     * to omit it.
     *
     * <p>Kept as a {@link String}: the WhatsApp Web bundle of snapshot {@code 1040120866} surfaces only
     * the {@code LLAMA} literal through the debug entrypoint and does not declare the closed set, so no
     * Java enum is inferred.
     */
    private final String model;

    /**
     * The {@code user_info} field of the {@code data} object carrying the recent message history, or
     * {@code null} to omit it.
     *
     * <p>Kept as a {@link String}: WhatsApp Web serializes it as the JSON text
     * {@code {"message_history": [...]}} and passes it through opaquely.
     */
    private final String userInfo;

    /**
     * The {@code user_message_draft} field of the {@code data} object holding the draft the sender is
     * composing, or {@code null} to omit it.
     */
    private final String userMessageDraft;

    /**
     * The {@code user_prompt} field of the {@code data} object holding the prompt the sender supplied,
     * or {@code null} to omit it.
     */
    private final String userPrompt;

    /**
     * Constructs a GenAI-recommendation query request.
     *
     * <p>All values populate the {@code data} GraphQL object; each value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param actorId          the requesting business-profile id, or {@code null} to omit the field
     * @param model            the generative model name, or {@code null} to omit the field
     * @param userInfo         the JSON-encoded message-history blob, or {@code null} to omit the field
     * @param userMessageDraft the draft the sender is composing, or {@code null} to omit the field
     * @param userPrompt       the prompt the sender supplied, or {@code null} to omit the field
     */
    public BizBroadcastGenAiRecommendationJobFacebookGraphQlRequest(String actorId, String model, String userInfo, String userMessageDraft, String userPrompt) {
        this.actorId = actorId;
        this.model = model;
        this.userInfo = userInfo;
        this.userMessageDraft = userMessageDraft;
        this.userPrompt = userPrompt;
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
     * @implNote This implementation emits {@code {"data": {"actor_id": <actorId>, "model": <model>,
     * "user_info": <userInfo>, "user_message_draft": <userMessageDraft>, "user_prompt":
     * <userPrompt>}}}, writing each field only when its value is non-null and emitting
     * {@code {"data": {}}} when all are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizBroadcastGenAIRecommendationJob", exports = "fetchBroadcastGenAIRecommendation",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("data");
            writer.writeColon();
            writer.startObject();
            if (actorId != null) {
                writer.writeName("actor_id");
                writer.writeColon();
                writer.writeString(actorId);
            }

            if (model != null) {
                writer.writeName("model");
                writer.writeColon();
                writer.writeString(model);
            }

            if (userInfo != null) {
                writer.writeName("user_info");
                writer.writeColon();
                writer.writeString(userInfo);
            }

            if (userMessageDraft != null) {
                writer.writeName("user_message_draft");
                writer.writeColon();
                writer.writeString(userMessageDraft);
            }

            if (userPrompt != null) {
                writer.writeName("user_prompt");
                writer.writeColon();
                writer.writeString(userPrompt);
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
